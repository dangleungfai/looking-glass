package com.isp.lg.controller;

import com.isp.lg.repository.IpBlacklistRepository;
import com.isp.lg.repository.QueryLogRepository;
import com.isp.lg.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final QueryLogRepository queryLogRepository;
    private final IpBlacklistRepository ipBlacklistRepository;
    private final UserRepository userRepository;

    public AdminDashboardController(QueryLogRepository queryLogRepository,
                                    IpBlacklistRepository ipBlacklistRepository,
                                    UserRepository userRepository) {
        this.queryLogRepository = queryLogRepository;
        this.ipBlacklistRepository = ipBlacklistRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/visualization")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'READONLY')")
    public ResponseEntity<Map<String, Object>> visualization(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "24") int securityHours) {
        int boundedDays = Math.max(1, Math.min(days, 30));
        int boundedHours = Math.max(1, Math.min(securityHours, 168));
        Instant trendStart = Instant.now().minus(boundedDays, ChronoUnit.DAYS);
        Instant securityStart = Instant.now().minus(boundedHours, ChronoUnit.HOURS);

        List<Map<String, Object>> trend = queryLogRepository.findHourlyTrendSince(trendStart).stream()
                .map(r -> mapOf(
                        "bucket", r.getBucket(),
                        "total", r.getTotal() == null ? 0L : r.getTotal(),
                        "success", r.getSuccess() == null ? 0L : r.getSuccess(),
                        "failed", r.getFailed() == null ? 0L : r.getFailed()
                ))
                .toList();

        List<Map<String, Object>> topTargets = queryLogRepository.findTopTargetsSince(trendStart).stream()
                .map(r -> mapOf(
                        "target", r.getTarget() == null ? "-" : r.getTarget(),
                        "count", r.getCnt() == null ? 0L : r.getCnt()
                ))
                .toList();

        List<Map<String, Object>> topFailSourceIps = queryLogRepository.findTopFailedSourceIpsSince(securityStart).stream()
                .map(r -> mapOf(
                        "sourceIp", r.getSourceIp() == null ? "-" : r.getSourceIp(),
                        "count", r.getCnt() == null ? 0L : r.getCnt()
                ))
                .toList();

        long securityTotal = queryLogRepository.countTotalSince(securityStart);
        long securityFailed = queryLogRepository.countFailedSince(securityStart);
        long uniqueSourceIps = queryLogRepository.countDistinctSourceIpSince(securityStart);
        long blacklistActive = ipBlacklistRepository.countByStatus(1);
        long ldapUsers = userRepository.countByUserType("LDAP");
        long localUsers = userRepository.countByUserType("LOCAL");

        Map<String, Object> security = mapOf(
                "hours", boundedHours,
                "totalQueries", securityTotal,
                "failedQueries", securityFailed,
                "successRate", securityTotal <= 0 ? 100 : Math.round((securityTotal - securityFailed) * 100.0 / securityTotal),
                "uniqueSourceIps", uniqueSourceIps,
                "blacklistActive", blacklistActive,
                "ldapUsers", ldapUsers,
                "localUsers", localUsers,
                "topFailSourceIps", topFailSourceIps
        );

        return ResponseEntity.ok(mapOf(
                "days", boundedDays,
                "trend", trend,
                "topTargets", topTargets,
                "security", security
        ));
    }

    private Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
}
