package com.isp.lg.service;

import com.isp.lg.domain.*;
import com.isp.lg.dto.PublicQueryRequest;
import com.isp.lg.dto.PublicQueryResponse;
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
import java.util.stream.Collectors;

@Service
public class QueryService {

    private final PopRepository popRepository;
    private final DeviceRepository deviceRepository;
    private final CommandTemplateRepository commandTemplateRepository;
    private final QueryLogRepository queryLogRepository;
    private final InputValidationService inputValidationService;
    private final WorkerClient workerClient;
    private final BlacklistService blacklistService;
    private final RateLimitService rateLimitService;
    private final MeterRegistry meterRegistry;

    public QueryService(PopRepository popRepository,
                        DeviceRepository deviceRepository,
                        CommandTemplateRepository commandTemplateRepository,
                        QueryLogRepository queryLogRepository,
                        InputValidationService inputValidationService,
                        WorkerClient workerClient,
                        BlacklistService blacklistService,
                        RateLimitService rateLimitService,
                        MeterRegistry meterRegistry) {
        this.popRepository = popRepository;
        this.deviceRepository = deviceRepository;
        this.commandTemplateRepository = commandTemplateRepository;
        this.queryLogRepository = queryLogRepository;
        this.inputValidationService = inputValidationService;
        this.workerClient = workerClient;
        this.blacklistService = blacklistService;
        this.rateLimitService = rateLimitService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public List<com.isp.lg.dto.PopDto> listPublicPops() {
        return popRepository.findByIsPublicAndStatusOrderBySortOrderAsc(1, 1)
                .stream()
                .map(p -> new com.isp.lg.dto.PopDto(p.getPopCode(), p.getPopName(), p.getCountry(), p.getCity()))
                .collect(Collectors.toList());
    }

    public List<String> listPublicQueryTypes() {
        return Arrays.asList("PING", "TRACEROUTE", "BGP_PREFIX", "BGP_ASN", "ROUTE_LOOKUP");
    }

    @Transactional
    public PublicQueryResponse executeQuery(PublicQueryRequest request, String sourceIp, String userAgent) {
        String requestId = "LG" + System.currentTimeMillis();
        long startMs = System.currentTimeMillis();

        if (blacklistService.isBlacklisted(sourceIp)) {
            throw new SecurityException("Access denied");
        }
        if (!rateLimitService.allow(sourceIp)) {
            meterRegistry.counter("lookingglass.rate_limit_exceeded", "source", "public").increment();
            throw new SecurityException("Rate limit exceeded. Try again later.");
        }

        inputValidationService.validateTargetForQueryType(request.getTarget(), request.getQueryType());

        Pop pop = popRepository.findByPopCode(request.getPopCode())
                .orElseThrow(() -> new IllegalArgumentException("POP not found or not public"));
        if (pop.getIsPublic() != 1 || pop.getStatus() != 1) {
            throw new IllegalArgumentException("POP not available");
        }

        List<Device> devices = deviceRepository.findByPopIdAndStatusOrderByPriorityDesc(pop.getId(), 1);
        Device device = null;
        CommandTemplate template = null;
        for (Device d : devices) {
            if (supportsQueryType(d, request.getQueryType())) {
                template = commandTemplateRepository
                        .findByVendorAndOsTypeAndQueryTypeAndStatus(d.getVendor(), d.getOsType(), request.getQueryType(), 1)
                        .orElse(null);
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
        int count = inputValidationService.clampCount(getInt(params, "count"));
        int maxHops = inputValidationService.clampMaxHops(getInt(params, "maxHops"));
        String target = request.getTarget().trim();

        String command = fillTemplate(template.getCommandText(), request.getQueryType(), target, count, maxHops);
        String password = device.getPasswordEncrypted() != null ? new String(device.getPasswordEncrypted(), StandardCharsets.UTF_8) : null;

        WorkerTaskRequest task = new WorkerTaskRequest();
        task.setRequestId(requestId);
        task.setMgmtIp(device.getMgmtIp());
        task.setSshPort(device.getSshPort() != null ? device.getSshPort() : 22);
        task.setUsername(device.getUsername());
        task.setPassword(password);
        task.setCommand(command);
        task.setQueryType(request.getQueryType());
        task.setTimeoutSec(device.getTimeoutSec() != null ? device.getTimeoutSec() : 10);

        QueryLog log = new QueryLog();
        log.setRequestId(requestId);
        log.setSourceIp(sourceIp);
        log.setUserAgent(userAgent);
        log.setQueryType(request.getQueryType());
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
            log.setErrorMessage(e.getMessage());
            log.setDurationMs((int) (System.currentTimeMillis() - startMs));
            queryLogRepository.save(log);
            throw new RuntimeException("Worker execution failed: " + e.getMessage());
        }

        log.setResultStatus(resp.getStatus() != null ? resp.getStatus() : "FAILED");
        log.setResultText(resp.getRawText());
        log.setDurationMs(resp.getDurationMs() != null ? resp.getDurationMs().intValue() : (int) (System.currentTimeMillis() - startMs));
        log.setErrorMessage(resp.getErrorMessage());
        queryLogRepository.save(log);

        meterRegistry.counter("lookingglass.query_total", "query_type", request.getQueryType(), "status", log.getResultStatus()).increment();

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
        return Arrays.asList(d.getSupportedQueryTypes().split(",")).stream()
                .map(String::trim)
                .anyMatch(q -> q.equalsIgnoreCase(queryType));
    }

    private int getInt(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String fillTemplate(String template, String queryType, String target, int count, int maxHops) {
        return template
                .replace("${target}", target)
                .replace("${count}", String.valueOf(count))
                .replace("${max_hop}", String.valueOf(maxHops))
                .replace("${prefix}", target)
                .replace("${asn}", target.replaceAll("(?i)^AS", "").trim());
    }
}
