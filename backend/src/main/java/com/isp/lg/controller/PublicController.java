package com.isp.lg.controller;

import com.isp.lg.dto.PopDto;
import com.isp.lg.dto.PublicQueryRequest;
import com.isp.lg.dto.PublicQueryResponse;
import com.isp.lg.service.CaptchaService;
import com.isp.lg.service.QueryService;
import com.isp.lg.service.SystemAssetService;
import com.isp.lg.service.SystemSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final QueryService queryService;
    private final SystemSettingsService systemSettingsService;
    private final SystemAssetService systemAssetService;
    private final CaptchaService captchaService;

    public PublicController(QueryService queryService,
                            SystemSettingsService systemSettingsService,
                            SystemAssetService systemAssetService,
                            CaptchaService captchaService) {
        this.queryService = queryService;
        this.systemSettingsService = systemSettingsService;
        this.systemAssetService = systemAssetService;
        this.captchaService = captchaService;
    }

    @GetMapping("/pops")
    public ResponseEntity<List<PopDto>> listPops() {
        return ResponseEntity.ok(queryService.listPublicPops());
    }

    @GetMapping("/query-types")
    public ResponseEntity<List<String>> listQueryTypes() {
        return ResponseEntity.ok(queryService.listPublicQueryTypes());
    }

    @GetMapping("/system-name")
    public ResponseEntity<Map<String, Object>> systemName(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = "unknown";
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("systemName", systemSettingsService.getSystemName());
        payload.put("showPopCode", systemSettingsService.getShowPopCode());
        payload.put("footerText", systemSettingsService.getFooterText());
        payload.put("homeIntroText", systemSettingsService.getHomeIntroText());
        payload.put("logoUrl", "/api/public/logo");
        payload.put("hasCustomLogo", systemAssetService.hasCustomLogo());
        payload.put("clientIp", clientIp);
        payload.put("captchaEnabled", systemSettingsService.isCaptchaEnabled());
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/captcha")
    public ResponseEntity<Map<String, Object>> captcha(HttpServletRequest request) {
        boolean enabled = systemSettingsService.isCaptchaEnabled();
        if (!enabled) {
            return ResponseEntity.ok(Map.of("enabled", false));
        }
        String sourceIp = getClientIp(request);
        CaptchaService.CaptchaChallenge challenge = captchaService.issue(sourceIp);
        return ResponseEntity.ok(Map.of(
                "enabled", true,
                "captchaId", challenge.captchaId(),
                "question", challenge.question()
        ));
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() throws Exception {
        SystemAssetService.AssetData data = systemAssetService.loadLogo();
        return ResponseEntity.ok()
                .header("Content-Type", data.contentType())
                .cacheControl(CacheControl.noStore())
                .body(data.bytes());
    }

    @PostMapping("/query")
    public ResponseEntity<PublicQueryResponse> query(@Valid @RequestBody PublicQueryRequest request,
                                                      HttpServletRequest httpRequest) {
        String sourceIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        PublicQueryResponse response = queryService.executeQuery(request, sourceIp, userAgent != null ? userAgent : "");
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String[] candidateHeaders = {
                "X-Forwarded-For",
                "X-Real-IP",
                "CF-Connecting-IP",
                "True-Client-IP",
                "X-Client-IP",
                "X-Original-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : candidateHeaders) {
            String value = request.getHeader(header);
            String ip = extractFirstValidIp(value);
            if (ip != null) {
                return ip;
            }
        }

        String forwarded = request.getHeader("Forwarded");
        String ipFromForwarded = parseForwardedFor(forwarded);
        if (ipFromForwarded != null) {
            return ipFromForwarded;
        }

        return normalizeIp(request.getRemoteAddr());
    }

    private String extractFirstValidIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split(",");
        String fallbackLoopback = null;
        for (String part : parts) {
            String ip = normalizeIp(part.trim());
            if (ip != null && !ip.equalsIgnoreCase("unknown")) {
                if (!isLoopbackIp(ip)) {
                    return ip;
                }
                if (fallbackLoopback == null) {
                    fallbackLoopback = ip;
                }
            }
        }
        return fallbackLoopback;
    }

    private String parseForwardedFor(String forwarded) {
        if (forwarded == null || forwarded.isBlank()) {
            return null;
        }
        String[] entries = forwarded.split(",");
        for (String entry : entries) {
            String[] segments = entry.split(";");
            for (String segment : segments) {
                String s = segment.trim();
                if (!s.toLowerCase(Locale.ROOT).startsWith("for=")) {
                    continue;
                }
                String ip = s.substring(4).trim();
                if (ip.startsWith("\"") && ip.endsWith("\"") && ip.length() > 1) {
                    ip = ip.substring(1, ip.length() - 1);
                }
                // RFC 7239 IPv6 可能是 [2001:db8::1]:1234
                if (ip.startsWith("[") && ip.contains("]")) {
                    ip = ip.substring(1, ip.indexOf("]"));
                }
                int colonPos = ip.indexOf(':');
                if (colonPos > 0 && ip.chars().filter(ch -> ch == ':').count() == 1) {
                    ip = ip.substring(0, colonPos);
                }
                ip = normalizeIp(ip);
                if (ip != null && !ip.equalsIgnoreCase("unknown")) {
                    return ip;
                }
            }
        }
        return null;
    }

    private boolean isLoopbackIp(String ip) {
        if (ip == null) return false;
        return "127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip);
    }

    private String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        String value = ip.trim();
        if ("::1".equals(value) || "0:0:0:0:0:0:0:1".equals(value)) {
            return "127.0.0.1";
        }
        if (value.startsWith("::ffff:")) {
            return value.substring(7);
        }
        return value;
    }
}
