package com.isp.lg.service;

import com.isp.lg.domain.*;
import com.isp.lg.dto.PublicQueryRequest;
import com.isp.lg.dto.PublicQueryResponse;
import com.isp.lg.dto.SystemSettingsDto;
import com.isp.lg.repository.*;
import com.isp.lg.worker.WorkerClient;
import com.isp.lg.worker.WorkerTaskRequest;
import com.isp.lg.worker.WorkerTaskResponse;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class QueryService {
    private static final Pattern TEMPLATE_PARAM = Pattern.compile("\\$\\{([a-zA-Z0-9_\\-]+)}");
    private static final List<String> SUPPORTED_QUERY_TYPES = Arrays.asList(
            "IPV4_PING",
            "IPV6_PING",
            "IPV4_TRACEROUTE",
            "IPV6_TRACEROUTE",
            "IPV4_BGP_ROUTE",
            "IPV6_BGP_ROUTE"
    );

    private final PopRepository popRepository;
    private final DeviceRepository deviceRepository;
    private final CommandTemplateRepository commandTemplateRepository;
    private final QueryLogRepository queryLogRepository;
    private final InputValidationService inputValidationService;
    private final WorkerClient workerClient;
    private final BlacklistService blacklistService;
    private final RateLimitService rateLimitService;
    private final MeterRegistry meterRegistry;
    private final SystemSettingsService systemSettingsService;
    private final CaptchaService captchaService;

    public QueryService(PopRepository popRepository,
                        DeviceRepository deviceRepository,
                        CommandTemplateRepository commandTemplateRepository,
                        QueryLogRepository queryLogRepository,
                        InputValidationService inputValidationService,
                        WorkerClient workerClient,
                        BlacklistService blacklistService,
                        RateLimitService rateLimitService,
                        MeterRegistry meterRegistry,
                        SystemSettingsService systemSettingsService,
                        CaptchaService captchaService) {
        this.popRepository = popRepository;
        this.deviceRepository = deviceRepository;
        this.commandTemplateRepository = commandTemplateRepository;
        this.queryLogRepository = queryLogRepository;
        this.inputValidationService = inputValidationService;
        this.workerClient = workerClient;
        this.blacklistService = blacklistService;
        this.rateLimitService = rateLimitService;
        this.meterRegistry = meterRegistry;
        this.systemSettingsService = systemSettingsService;
        this.captchaService = captchaService;
    }

    @Transactional(readOnly = true)
    public List<com.isp.lg.dto.PopDto> listPublicPops() {
        return popRepository.findByIsPublicAndStatusOrderBySortOrderAsc(1, 1)
                .stream()
                .map(p -> new com.isp.lg.dto.PopDto(p.getPopCode(), p.getPopName(), p.getCountry(), p.getCity()))
                .collect(Collectors.toList());
    }

    public List<String> listPublicQueryTypes() {
        Map<String, Integer> orderRank = new HashMap<>();
        for (int i = 0; i < SUPPORTED_QUERY_TYPES.size(); i++) {
            orderRank.put(SUPPORTED_QUERY_TYPES.get(i), i);
        }
        return commandTemplateRepository.findEnabledPublicQueryTypes().stream()
                .map(this::normalizeQueryType)
                .filter(q -> q != null && !q.isBlank())
                .filter(orderRank::containsKey)
                .distinct()
                .sorted((a, b) -> {
                    Integer ra = orderRank.get(a);
                    Integer rb = orderRank.get(b);
                    return Integer.compare(ra, rb);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public PublicQueryResponse executeQuery(PublicQueryRequest request, String sourceIp, String userAgent) {
        String requestId = "LG" + System.currentTimeMillis();
        long startMs = System.currentTimeMillis();
        String normalizedQueryType = normalizeQueryType(request.getQueryType());

        if (blacklistService.isBlacklisted(sourceIp)) {
            throw new SecurityException("Access denied");
        }
        if (!rateLimitService.allow(sourceIp)) {
            meterRegistry.counter("lookingglass.rate_limit_exceeded", "source", "public").increment();
            throw new SecurityException("Rate limit exceeded. Try again later.");
        }
        if (systemSettingsService.isCaptchaEnabled()) {
            boolean passed = captchaService.validateAndConsume(request.getCaptchaToken(), sourceIp);
            if (!passed) {
                throw new SecurityException("Captcha 验证失败，请刷新验证码后重试。");
            }
        }

        inputValidationService.validateTargetForQueryType(request.getTarget(), normalizedQueryType);

        Pop pop = popRepository.findByPopCode(request.getPopCode())
                .orElseThrow(() -> new IllegalArgumentException("POP not found or not public"));
        if (pop.getIsPublic() != 1 || pop.getStatus() != 1) {
            throw new IllegalArgumentException("POP not available");
        }

        List<Device> devices = deviceRepository.findByPopIdAndStatusOrderByPriorityDesc(pop.getId(), 1);
        Device device = null;
        CommandTemplate template = null;
        for (Device d : devices) {
            if (supportsQueryType(d, normalizedQueryType)) {
                for (String candidateType : queryTypeCandidates(normalizedQueryType)) {
                    template = commandTemplateRepository
                            .findByVendorAndOsTypeAndQueryTypeAndStatus(d.getVendor(), d.getOsType(), candidateType, 1)
                            .orElse(null);
                    if (template != null) break;
                }
                if (template != null) {
                    device = d;
                    break;
                }
            }
        }
        if (device == null || template == null) {
            throw new IllegalArgumentException("No device or template available for this POP and query type");
        }

        Map<String, Object> params = request.getParams() != null ? request.getParams() : new HashMap<>();
        int count = inputValidationService.clampCount(getOptionalInt(params, "count", "pingCount"));
        int maxHops = inputValidationService.clampMaxHops(getOptionalInt(params, "max_hop", "maxHops", "maxHop"));
        String target = request.getTarget().trim();

        String effectiveTemplate = resolveEffectiveTemplate(template);
        String command = fillTemplate(effectiveTemplate, target, count, maxHops, params);
        SystemSettingsDto.DeviceDefaults defaults = systemSettingsService.getDeviceDefaults();
        String execAuthType = pickAuthType(device.getAuthType(), defaults.getAuthType());
        int execPort = pickPort(execAuthType, device.getSshPort(), defaults.getSshPort(), defaults.getTelnetPort());
        String execUsername = pickUsername(device.getUsername(), defaults.getUsername());
        String devicePassword = device.getPasswordEncrypted() != null ? new String(device.getPasswordEncrypted(), StandardCharsets.UTF_8) : null;
        String execPassword = pickPassword(devicePassword, defaults.getPassword());

        WorkerTaskRequest task = new WorkerTaskRequest();
        task.setRequestId(requestId);
        task.setMgmtIp(device.getMgmtIp());
        task.setSshPort(execPort);
        task.setUsername(execUsername);
        task.setPassword(execPassword);
        task.setCommand(command);
        task.setQueryType(normalizedQueryType);
        task.setTimeoutSec(device.getTimeoutSec() != null ? device.getTimeoutSec() : 10);

        QueryLog log = new QueryLog();
        log.setRequestId(requestId);
        log.setSourceIp(sourceIp);
        log.setUserAgent(userAgent);
        log.setQueryType(normalizedQueryType);
        log.setQueryTarget(target);
        log.setPopId(pop.getId());
        log.setDeviceId(device.getId());
        log.setCommandTemplateId(template.getId());
        log.setRawCommand(command);
        log.setResultStatus("PENDING");
        log.setCreatedAt(Instant.now());
        queryLogRepository.save(log);

        WorkerTaskResponse resp;
        try {
            resp = workerClient.execute(task);
        } catch (Exception e) {
            log.setResultStatus("FAILED");
            log.setErrorMessage(buildFailureReason(e.getMessage(), task));
            log.setDurationMs((int) (System.currentTimeMillis() - startMs));
            queryLogRepository.save(log);
            throw new RuntimeException("Worker execution failed: " + e.getMessage());
        }

        log.setResultStatus(resp.getStatus() != null ? resp.getStatus() : "FAILED");
        log.setResultText(resp.getRawText());
        log.setDurationMs(resp.getDurationMs() != null ? resp.getDurationMs().intValue() : (int) (System.currentTimeMillis() - startMs));
        if ("FAILED".equalsIgnoreCase(log.getResultStatus())) {
            log.setErrorMessage(buildFailureReason(resp.getErrorMessage(), task));
        } else {
            log.setErrorMessage(null);
        }
        queryLogRepository.save(log);

        meterRegistry.counter("lookingglass.query_total", "query_type", normalizedQueryType, "status", log.getResultStatus()).increment();

        Map<String, Object> result = new HashMap<>();
        result.put("rawText", resp.getRawText());
        if (resp.getResult() != null) {
            result.putAll(resp.getResult());
        }

        return new PublicQueryResponse(
                requestId,
                resp.getStatus() != null ? resp.getStatus() : "SUCCESS",
                resp.getDurationMs() != null ? resp.getDurationMs() : System.currentTimeMillis() - startMs,
                pop.getPopCode(),
                device.getDeviceCode(),
                result
        );
    }

    private boolean supportsQueryType(Device d, String queryType) {
        if (d.getSupportedQueryTypes() == null || d.getSupportedQueryTypes().isEmpty()) return true;
        Set<String> supported = Arrays.stream(d.getSupportedQueryTypes().split(","))
                .map(String::trim)
                .map(this::normalizeQueryType)
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.toSet());
        return supported.contains(normalizeQueryType(queryType));
    }

    private Integer getOptionalInt(Map<String, Object> params, String... keys) {
        for (String key : keys) {
            Object v = params.get(key);
            if (v == null) continue;
            if (v instanceof Number) return ((Number) v).intValue();
            try {
                return Integer.parseInt(v.toString().trim());
            } catch (NumberFormatException ignored) {
                // continue checking compatible key names
            }
        }
        return null;
    }

    private String fillTemplate(String template, String target, int count, int maxHops, Map<String, Object> params) {
        Map<String, String> replacements = new HashMap<>();
        if (params != null) {
            for (Map.Entry<String, Object> e : params.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                replacements.put(e.getKey(), String.valueOf(e.getValue()));
            }
        }

        String prefix = replacements.getOrDefault("prefix", target);
        String asn = replacements.getOrDefault("asn", target).replaceAll("(?i)^AS", "").trim();
        replacements.put("target", target);
        replacements.put("count", String.valueOf(count));
        replacements.put("max_hop", String.valueOf(maxHops));
        replacements.put("maxHops", String.valueOf(maxHops));
        replacements.put("maxHop", String.valueOf(maxHops));
        replacements.put("prefix", prefix);
        replacements.put("asn", asn);

        Matcher matcher = TEMPLATE_PARAM.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = replacements.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String resolveEffectiveTemplate(CommandTemplate template) {
        String commandText = template.getCommandText() != null ? template.getCommandText().trim() : "";
        if (commandText.contains("${")) {
            return commandText;
        }

        String vendor = template.getVendor() != null ? template.getVendor().toUpperCase() : "";
        String queryType = normalizeQueryType(template.getQueryType());

        switch (queryType) {
            case "IPV4_PING":
                if ("JUNIPER_JUNOS".equals(vendor)) return "ping count ${count} ${target}";
                if ("HUAWEI_VRP".equals(vendor)) return "ping -c ${count} ${target}";
                return "ping ${target} count ${count}";
            case "IPV6_PING":
                if ("JUNIPER_JUNOS".equals(vendor)) return "ping inet6 ${target} count ${count}";
                if ("HUAWEI_VRP".equals(vendor)) return "ping ipv6 -c ${count} ${target}";
                return "ping ipv6 ${target} count ${count}";
            case "IPV4_TRACEROUTE":
                if ("JUNIPER_JUNOS".equals(vendor)) return "traceroute ${target}";
                if ("HUAWEI_VRP".equals(vendor)) return "tracert ${target}";
                return "traceroute ${target} max-hop ${max_hop}";
            case "IPV6_TRACEROUTE":
                if ("JUNIPER_JUNOS".equals(vendor)) return "traceroute inet6 ${target}";
                if ("HUAWEI_VRP".equals(vendor)) return "tracert ipv6 ${target}";
                return "traceroute ipv6 ${target} max-hop ${max_hop}";
            case "IPV4_BGP_ROUTE":
                if ("JUNIPER_JUNOS".equals(vendor)) return "show route protocol bgp ${prefix}";
                if ("HUAWEI_VRP".equals(vendor)) return "display bgp routing-table ${prefix}";
                return "show bgp ipv4 unicast ${prefix}";
            case "IPV6_BGP_ROUTE":
                if ("JUNIPER_JUNOS".equals(vendor)) return "show route table inet6.0 protocol bgp ${prefix}";
                if ("HUAWEI_VRP".equals(vendor)) return "display bgp ipv6 routing-table ${prefix}";
                return "show bgp ipv6 unicast ${prefix}";
            default:
                return commandText;
        }
    }

    private String normalizeQueryType(String queryType) {
        if (queryType == null) return "";
        String q = queryType.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        switch (q) {
            case "PING":
            case "IPV4PING":
            case "IPV4_PING":
                return "IPV4_PING";
            case "IPV6PING":
            case "IPV6_PING":
                return "IPV6_PING";
            case "TRACEROUTE":
            case "IPV4TRACEROUTE":
            case "IPV4_TRACEROUTE":
                return "IPV4_TRACEROUTE";
            case "IPV6TRACEROUTE":
            case "IPV6_TRACEROUTE":
                return "IPV6_TRACEROUTE";
            case "BGP_PREFIX":
            case "BGP_ASN":
            case "ROUTE_LOOKUP":
            case "IPV4_BGP_ROUTE":
            case "IPV4BGPROUTE":
                return "IPV4_BGP_ROUTE";
            case "IPV6_BGP_ROUTE":
            case "IPV6BGPROUTE":
                return "IPV6_BGP_ROUTE";
            default:
                return q;
        }
    }

    private List<String> queryTypeCandidates(String normalizedQueryType) {
        switch (normalizedQueryType) {
            case "IPV4_PING":
                return Arrays.asList("IPV4_PING", "PING");
            case "IPV6_PING":
                return Arrays.asList("IPV6_PING");
            case "IPV4_TRACEROUTE":
                return Arrays.asList("IPV4_TRACEROUTE", "TRACEROUTE");
            case "IPV6_TRACEROUTE":
                return Arrays.asList("IPV6_TRACEROUTE");
            case "IPV4_BGP_ROUTE":
                return Arrays.asList("IPV4_BGP_ROUTE", "BGP_PREFIX", "ROUTE_LOOKUP");
            case "IPV6_BGP_ROUTE":
                return Arrays.asList("IPV6_BGP_ROUTE");
            default:
                return Collections.singletonList(normalizedQueryType);
        }
    }

    private String pickAuthType(String deviceAuthType, String globalAuthType) {
        String g = normalizeAuthType(globalAuthType);
        if (g != null) return g;
        String d = normalizeAuthType(deviceAuthType);
        return d != null ? d : "SSH";
    }

    private String normalizeAuthType(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim().toUpperCase();
        if ("PASSWORD".equals(v) || "SSH_KEY".equals(v)) return "SSH";
        if (!"SSH".equals(v) && !"TELNET".equals(v)) return null;
        return v;
    }

    private int pickPort(String authType, Integer devicePort, Integer globalSshPort, Integer globalTelnetPort) {
        if ("TELNET".equals(authType)) {
            if (globalTelnetPort != null && globalTelnetPort > 0) return globalTelnetPort;
            if (devicePort != null && devicePort > 0) return devicePort;
            return 23;
        }
        if (globalSshPort != null && globalSshPort > 0) return globalSshPort;
        if (devicePort != null && devicePort > 0) return devicePort;
        return 22;
    }

    private String pickUsername(String deviceUsername, String globalUsername) {
        if (globalUsername != null && !globalUsername.isBlank()) return globalUsername.trim();
        if (deviceUsername != null && !deviceUsername.isBlank()) return deviceUsername.trim();
        return "admin";
    }

    private String pickPassword(String devicePassword, String globalPassword) {
        if (globalPassword != null && !globalPassword.isBlank()) return globalPassword;
        return devicePassword;
    }

    private String buildFailureReason(String rawError, WorkerTaskRequest task) {
        String err = rawError != null ? rawError.trim() : "";
        String lower = err.toLowerCase();
        String endpoint = task.getMgmtIp() + ":" + task.getSshPort();

        if (lower.contains("authentication_failed") || lower.contains("authentication failed")
                || lower.contains("permission denied")) {
            return "失败原因: 账号密码错误/认证失败。建议检查设备 AAA、用户名密码和登录策略。目标: " + endpoint
                    + (err.isEmpty() ? "" : "。原始错误: " + err);
        }
        if (lower.contains("port_unreachable") || lower.contains("port_refused")
                || lower.contains("connection refused") || lower.contains("unable to connect to port")) {
            return "失败原因: 端口不通或被拒绝。建议检查 ACL/防火墙、设备服务监听和端口配置。目标: " + endpoint
                    + (err.isEmpty() ? "" : "。原始错误: " + err);
        }
        if (lower.contains("network_unreachable") || lower.contains("no route to host")
                || lower.contains("network is unreachable")) {
            return "失败原因: 网络不通(无路由/不可达)。建议检查管理网路由与链路状态。目标: " + endpoint
                    + (err.isEmpty() ? "" : "。原始错误: " + err);
        }
        if (lower.contains("network_timeout") || lower.contains("connection timeout")
                || lower.contains("timed out") || lower.contains("timeout")) {
            return "失败原因: 连接超时。可能是网络抖动、端口被丢弃或设备无响应。目标: " + endpoint
                    + (err.isEmpty() ? "" : "。原始错误: " + err);
        }
        if (lower.contains("dns_resolve_failed") || lower.contains("name or service not known")) {
            return "失败原因: 地址解析失败。请检查管理地址配置。目标: " + endpoint
                    + (err.isEmpty() ? "" : "。原始错误: " + err);
        }
        if (lower.contains("no password or private key provided")) {
            return "失败原因: 缺少登录凭据。请在系统设置或设备配置中补全账号密码。目标: " + endpoint;
        }
        return "失败原因: 未分类执行失败。建议结合原始错误排查。目标: " + endpoint
                + (err.isEmpty() ? "" : "。原始错误: " + err);
    }
}
