package com.isp.lg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isp.lg.domain.SystemSetting;
import com.isp.lg.dto.SystemSettingsDto;
import com.isp.lg.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.function.Supplier;

@Service
public class SystemSettingsService {

    private static final String DEFAULT_FOOTER_TEXT = "© 2026 ISP Looking Glass. All rights reserved.";
    private static final String DEFAULT_APPEARANCE = "default";
    public static final String KEY_GENERAL = "system_general";
    public static final String KEY_DEVICE_DEFAULTS = "system_device_defaults";
    public static final String KEY_RATE_LIMIT = "system_rate_limit";
    public static final String KEY_SECURITY = "system_security";
    public static final String KEY_LDAP = "system_ldap";

    private final SystemSettingRepository repo;
    private final ObjectMapper objectMapper;

    public SystemSettingsService(SystemSettingRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SystemSettingsDto load() {
        SystemSettingsDto dto = new SystemSettingsDto();
        dto.setGeneral(normalizeGeneral(readJson(KEY_GENERAL, SystemSettingsDto.General.class, this::defaultGeneral)));
        dto.setDeviceDefaults(normalizeDeviceDefaults(readJson(KEY_DEVICE_DEFAULTS, SystemSettingsDto.DeviceDefaults.class, this::defaultDeviceDefaults)));
        dto.setRateLimit(readJson(KEY_RATE_LIMIT, SystemSettingsDto.RateLimit.class, this::defaultRateLimit));
        dto.setSecurity(readJson(KEY_SECURITY, SystemSettingsDto.Security.class, this::defaultSecurity));
        dto.setLdap(normalizeLdap(readJson(KEY_LDAP, SystemSettingsDto.LdapSettings.class, this::defaultLdap)));
        return dto;
    }

    @Transactional
    public SystemSettingsDto save(SystemSettingsDto dto) {
        if (dto == null) dto = new SystemSettingsDto();
        if (dto.getGeneral() == null) {
            dto.setGeneral(readJson(KEY_GENERAL, SystemSettingsDto.General.class, this::defaultGeneral));
        }
        if (dto.getDeviceDefaults() == null) {
            dto.setDeviceDefaults(readJson(KEY_DEVICE_DEFAULTS, SystemSettingsDto.DeviceDefaults.class, this::defaultDeviceDefaults));
        }
        if (dto.getRateLimit() == null) {
            dto.setRateLimit(readJson(KEY_RATE_LIMIT, SystemSettingsDto.RateLimit.class, this::defaultRateLimit));
        }
        if (dto.getSecurity() == null) {
            dto.setSecurity(readJson(KEY_SECURITY, SystemSettingsDto.Security.class, this::defaultSecurity));
        }
        if (dto.getLdap() == null) {
            dto.setLdap(readJson(KEY_LDAP, SystemSettingsDto.LdapSettings.class, this::defaultLdap));
        }
        preserveLdapBindPasswordIfBlank(dto.getLdap());
        dto.setGeneral(normalizeGeneral(dto.getGeneral()));
        dto.setDeviceDefaults(normalizeDeviceDefaults(dto.getDeviceDefaults()));
        dto.setLdap(normalizeLdap(dto.getLdap()));

        writeJson(KEY_GENERAL, dto.getGeneral(), "系统通用配置");
        writeJson(KEY_DEVICE_DEFAULTS, dto.getDeviceDefaults(), "设备全局默认配置");
        writeJson(KEY_RATE_LIMIT, dto.getRateLimit(), "公网限流配置");
        writeJson(KEY_SECURITY, dto.getSecurity(), "安全配置");
        writeJson(KEY_LDAP, dto.getLdap(), "LDAP 认证配置");
        return load();
    }

    public SystemSettingsDto.DeviceDefaults getDeviceDefaults() {
        return normalizeDeviceDefaults(readJson(KEY_DEVICE_DEFAULTS, SystemSettingsDto.DeviceDefaults.class, this::defaultDeviceDefaults));
    }

    public SystemSettingsDto.RateLimit getRateLimit() {
        return readJson(KEY_RATE_LIMIT, SystemSettingsDto.RateLimit.class, this::defaultRateLimit);
    }

    public boolean isCaptchaEnabled() {
        SystemSettingsDto.Security security = readJson(KEY_SECURITY, SystemSettingsDto.Security.class, this::defaultSecurity);
        Boolean enabled = security.getCaptchaEnabled();
        return enabled != null && enabled;
    }

    public String getSystemName() {
        return normalizeGeneral(readJson(KEY_GENERAL, SystemSettingsDto.General.class, this::defaultGeneral)).getSystemName();
    }

    public boolean getShowPopCode() {
        Boolean show = normalizeGeneral(readJson(KEY_GENERAL, SystemSettingsDto.General.class, this::defaultGeneral)).getShowPopCode();
        return show == null || show;
    }

    public String getFooterText() {
        String text = normalizeGeneral(readJson(KEY_GENERAL, SystemSettingsDto.General.class, this::defaultGeneral)).getFooterText();
        if (text == null || text.isBlank()) return DEFAULT_FOOTER_TEXT;
        return text;
    }

    public String getHomeIntroText() {
        String text = normalizeGeneral(readJson(KEY_GENERAL, SystemSettingsDto.General.class, this::defaultGeneral)).getHomeIntroText();
        if (text == null || text.isBlank()) return defaultHomeIntroText();
        return text;
    }

    public String getAppearance() {
        return normalizeGeneral(readJson(KEY_GENERAL, SystemSettingsDto.General.class, this::defaultGeneral)).getAppearance();
    }

    private <T> T readJson(String key, Class<T> type, Supplier<T> defaultSupplier) {
        return repo.findBySettingKey(key)
                .map(s -> {
                    String json = s.getSettingValue();
                    if (json == null || json.isBlank()) return defaultSupplier.get();
                    try {
                        return objectMapper.readValue(json, type);
                    } catch (Exception e) {
                        return defaultSupplier.get();
                    }
                })
                .orElseGet(defaultSupplier);
    }

    private void writeJson(String key, Object value, String description) {
        try {
            String json = objectMapper.writeValueAsString(value);
            SystemSetting setting = repo.findBySettingKey(key).orElseGet(() -> {
                SystemSetting s = new SystemSetting();
                s.setSettingKey(key);
                s.setSettingValue("");
                return s;
            });
            setting.setSettingValue(json);
            setting.setDescription(description);
            setting.setUpdatedAt(Instant.now());
            repo.save(setting);
        } catch (Exception e) {
            throw new RuntimeException("保存系统设置失败: " + key, e);
        }
    }

    private SystemSettingsDto.DeviceDefaults defaultDeviceDefaults() {
        SystemSettingsDto.DeviceDefaults d = new SystemSettingsDto.DeviceDefaults();
        d.setAuthType("SSH");
        d.setUsername("admin");
        d.setPassword("");
        d.setSshPort(22);
        d.setTelnetPort(23);
        d.setTimeoutSec(15);
        d.setMaxConcurrency(10);
        return d;
    }

    private SystemSettingsDto.General defaultGeneral() {
        SystemSettingsDto.General g = new SystemSettingsDto.General();
        g.setSystemName("LOOKING GLASS");
        g.setShowPopCode(true);
        g.setFooterText(DEFAULT_FOOTER_TEXT);
        g.setHomeIntroText(defaultHomeIntroText());
        g.setAppearance(DEFAULT_APPEARANCE);
        return g;
    }

    private SystemSettingsDto.General normalizeGeneral(SystemSettingsDto.General g) {
        if (g == null) return defaultGeneral();
        String name = g.getSystemName();
        if (name == null || name.isBlank()) {
            g.setSystemName("LOOKING GLASS");
        } else {
            g.setSystemName(name.trim());
        }
        if (g.getShowPopCode() == null) {
            g.setShowPopCode(true);
        }
        String footerText = g.getFooterText();
        g.setFooterText(footerText == null || footerText.isBlank() ? DEFAULT_FOOTER_TEXT : footerText.trim());
        String introText = g.getHomeIntroText();
        if (introText == null || introText.isBlank()) {
            g.setHomeIntroText(defaultHomeIntroText());
        } else {
            g.setHomeIntroText(stripDeprecatedIntroHeading(introText.trim()));
        }
        String appearance = g.getAppearance();
        if (appearance == null || appearance.isBlank()) {
            g.setAppearance(DEFAULT_APPEARANCE);
        } else {
            String normalized = appearance.trim();
            if (!"default".equals(normalized) && !"techBlue".equals(normalized) && !"dark".equals(normalized)) {
                normalized = DEFAULT_APPEARANCE;
            }
            g.setAppearance(normalized);
        }
        return g;
    }

    private String stripDeprecatedIntroHeading(String text) {
        return text
                .replace("\n✅ 英文版（对外标准版）\n", "\n")
                .replace("✅ 英文版（对外标准版）\n", "")
                .replace("\n✅ 英文版（对外标准版）", "\n")
                .replace("✅ 英文版（对外标准版）", "");
    }

    private String defaultHomeIntroText() {
        return String.join("\n",
                "🌐 Looking Glass 网络诊断平台说明",
                "",
                "本 Looking Glass 系统为用户提供对我司骨干网络的可视化诊断能力，使您能够从我司网络视角，实时查询路由信息与网络连通性，获得与我司客户一致的网络透明度。",
                "",
                "⸻",
                "",
                "🔍 支持的查询功能",
                "",
                "BGP 查询（Border Gateway Protocol）",
                "用于查看从指定 PoP（节点）到目标 IP 地址或前缀的路由信息，包括最佳路径及其他可用路径（如 AS_PATH、Next-Hop 等）。",
                "",
                "Ping 测试",
                "用于检测从指定 PoP 到目标 IP 地址或域名的连通性及延迟（RTT）。",
                "",
                "Traceroute 路由追踪",
                "用于显示数据包从指定 PoP 到目标地址所经过的路径，并展示每一跳的延迟信息。",
                "",
                "POP-to-POP 测试（可选）",
                "用于查看我司不同 PoP 节点之间的网络延迟表现（仅限内部网络）。",
                "",
                "⸻",
                "",
                "📍 使用方法",
                "1. 选择测试节点（PoP / 城市 / 路由器）",
                "2. 选择查询类型（BGP / Ping / Trace）",
                "3. 输入目标 IP 地址或域名",
                "4. 点击查询执行测试",
                "",
                "⸻",
                "",
                "⚠️ 注意事项",
                "• 本系统仅提供有限的网络诊断命令（BGP / Ping / Trace），不支持自定义命令执行",
                "• 查询结果反映的是从我司网络视角到目标的路径及性能，可能与用户本地测试结果不同",
                "• DNS 解析结果可能因解析节点不同而存在差异，建议优先使用 IP 地址进行测试",
                "• 为保障系统稳定性，查询频率及并发可能受到限制",
                "",
                "⸻",
                "",
                "📩 问题反馈",
                "如您在使用过程中遇到问题或发现异常，请通过我们的技术支持渠道与我们联系。",
                "",
                "⸻",
                "",
                "🌐 Looking Glass Network Diagnostic Tool",
                "",
                "This Looking Glass platform provides visibility into our backbone routing and network performance, allowing users to perform real-time diagnostics from our network perspective.",
                "",
                "⸻",
                "",
                "🔍 Available Queries",
                "",
                "BGP (Border Gateway Protocol)",
                "Displays routing information from the selected PoP to the specified IP address or prefix, including best path and alternative routes (AS_PATH, next-hop, etc.).",
                "",
                "Ping",
                "Measures latency and packet reachability between the selected PoP and the destination IP address or hostname.",
                "",
                "Traceroute",
                "Shows the path taken by packets across the network from the selected PoP to the destination, including per-hop latency.",
                "",
                "POP-to-POP(Optional)",
                "Measures latency between different PoPs within our network (internal use).",
                "",
                "⸻",
                "",
                "📍 How to Use",
                "1. Select a PoP (Point of Presence) or node",
                "2. Choose a query type (BGP / Ping / Trace)",
                "3. Enter a destination IP address or hostname",
                "4. Click Run to execute",
                "",
                "⸻",
                "",
                "⚠️ Notes",
                "• This system allows only a limited set of diagnostic commands (BGP, Ping, Trace)",
                "• Results reflect routing and performance from our network perspective, which may differ from your local network",
                "• DNS resolution may vary depending on the resolver location; using an IP address is recommended",
                "• Query rate and concurrency may be limited for system stability",
                "",
                "⸻",
                "",
                "📩 Support",
                "If you encounter any issues or would like to report a problem, please contact our support team."
        );
    }

    private SystemSettingsDto.DeviceDefaults normalizeDeviceDefaults(SystemSettingsDto.DeviceDefaults d) {
        if (d == null) return defaultDeviceDefaults();
        String authType = d.getAuthType() == null ? "SSH" : d.getAuthType().trim().toUpperCase();
        if ("PASSWORD".equals(authType) || "SSH_KEY".equals(authType)) {
            authType = "SSH";
        }
        if (!"SSH".equals(authType) && !"TELNET".equals(authType)) {
            authType = "SSH";
        }
        d.setAuthType(authType);
        if (d.getSshPort() == null) d.setSshPort(22);
        if (d.getTelnetPort() == null) d.setTelnetPort(23);
        if (d.getUsername() == null || d.getUsername().isBlank()) d.setUsername("admin");
        if (d.getPassword() == null) d.setPassword("");
        if (d.getTimeoutSec() == null) d.setTimeoutSec(15);
        if (d.getMaxConcurrency() == null) d.setMaxConcurrency(10);
        return d;
    }

    private SystemSettingsDto.RateLimit defaultRateLimit() {
        SystemSettingsDto.RateLimit r = new SystemSettingsDto.RateLimit();
        r.setPerIpPerMinute(20);
        return r;
    }

    private SystemSettingsDto.Security defaultSecurity() {
        SystemSettingsDto.Security s = new SystemSettingsDto.Security();
        s.setCaptchaEnabled(false);
        s.setLogRetainDays(30);
        return s;
    }

    /**
     * 前端不返回已保存的 bind 密码；留空提交时表示不修改，沿用库中已有值。
     */
    private void preserveLdapBindPasswordIfBlank(SystemSettingsDto.LdapSettings incoming) {
        if (incoming == null) return;
        String pw = incoming.getBindPassword();
        if (pw != null && !pw.isBlank()) return;
        SystemSettingsDto.LdapSettings previous =
                readJson(KEY_LDAP, SystemSettingsDto.LdapSettings.class, this::defaultLdap);
        if (previous.getBindPassword() != null && !previous.getBindPassword().isBlank()) {
            incoming.setBindPassword(previous.getBindPassword());
        }
    }

    private SystemSettingsDto.LdapSettings defaultLdap() {
        SystemSettingsDto.LdapSettings l = new SystemSettingsDto.LdapSettings();
        l.setEnabled(false);
        l.setServerUrl("ldap://127.0.0.1:389");
        l.setUseTls(false);
        l.setBaseDn("");
        l.setBindDn("");
        l.setBindPassword("");
        l.setUserSearchBase("");
        l.setUserSearchFilter("(uid={0})");
        l.setConnectTimeoutMs(5000);
        l.setAllowLocalFallback(true);
        return l;
    }

    private SystemSettingsDto.LdapSettings normalizeLdap(SystemSettingsDto.LdapSettings l) {
        if (l == null) return defaultLdap();
        if (l.getEnabled() == null) l.setEnabled(false);
        if (l.getServerUrl() == null) l.setServerUrl("ldap://127.0.0.1:389");
        else l.setServerUrl(l.getServerUrl().trim());
        if (l.getUseTls() == null) l.setUseTls(false);
        if (l.getBaseDn() == null) l.setBaseDn("");
        else l.setBaseDn(l.getBaseDn().trim());
        if (l.getBindDn() == null) l.setBindDn("");
        else l.setBindDn(l.getBindDn().trim());
        if (l.getBindPassword() == null) l.setBindPassword("");
        if (l.getUserSearchBase() == null) l.setUserSearchBase("");
        else l.setUserSearchBase(l.getUserSearchBase().trim());
        if (l.getUserSearchFilter() == null || l.getUserSearchFilter().isBlank()) {
            l.setUserSearchFilter("(uid={0})");
        } else {
            l.setUserSearchFilter(l.getUserSearchFilter().trim());
        }
        if (l.getConnectTimeoutMs() == null || l.getConnectTimeoutMs() < 1000) {
            l.setConnectTimeoutMs(5000);
        }
        if (l.getAllowLocalFallback() == null) {
            l.setAllowLocalFallback(true);
        }
        return l;
    }
}

