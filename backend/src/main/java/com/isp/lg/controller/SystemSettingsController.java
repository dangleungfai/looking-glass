package com.isp.lg.controller;

import com.isp.lg.dto.SystemSettingsDto;
import com.isp.lg.service.SystemSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/system-settings")
public class SystemSettingsController {

    private final SystemSettingsService service;

    public SystemSettingsController(SystemSettingsService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'READONLY')")
    public ResponseEntity<SystemSettingsDto> get() {
        SystemSettingsDto dto = service.load();
        if (dto.getLdap() != null) {
            dto.getLdap().setBindPassword("");
        }
        return ResponseEntity.ok(dto);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemSettingsDto> update(@RequestBody SystemSettingsDto dto) {
        return ResponseEntity.ok(service.save(dto));
    }
}

