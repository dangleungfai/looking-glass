package com.isp.lg.controller;

import com.isp.lg.domain.QueryLog;
import com.isp.lg.repository.QueryLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/query-logs")
public class AdminQueryLogController {

    private final QueryLogRepository queryLogRepository;

    public AdminQueryLogController(QueryLogRepository queryLogRepository) {
        this.queryLogRepository = queryLogRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'NETWORK_ADMIN', 'READONLY_ADMIN', 'AUDITOR')")
    public ResponseEntity<Page<QueryLog>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryLogRepository.findByOrderByCreatedAtDesc(PageRequest.of(page, size)));
    }
}
