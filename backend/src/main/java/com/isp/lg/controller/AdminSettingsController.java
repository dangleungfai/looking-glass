package com.isp.lg.controller;

import com.isp.lg.domain.SystemSetting;
import com.isp.lg.repository.SystemSettingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    private final SystemSettingRepository systemSettingRepository;

    public AdminSettingsController(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'NETWORK_ADMIN', 'READONLY_ADMIN')")
    public ResponseEntity<List<SystemSetting>> list() {
        return ResponseEntity.ok(systemSettingRepository.findAll());
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'NETWORK_ADMIN')")
    public ResponseEntity<SystemSetting> update(@PathVariable String key, @RequestBody Map<String, String> body) {
        String value = body.get("settingValue");
        SystemSetting s = systemSettingRepository.findBySettingKey(key)
                .orElseGet(() -> {
                    SystemSetting n = new SystemSetting();
                    n.setSettingKey(key);
                    n.setSettingValue("");
                    return systemSettingRepository.save(n);
                });
        s.setSettingValue(value != null ? value : s.getSettingValue());
        s.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(systemSettingRepository.save(s));
    }
}
