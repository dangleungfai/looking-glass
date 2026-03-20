package com.isp.lg.controller;

import com.isp.lg.dto.LdapTestRequest;
import com.isp.lg.dto.SystemSettingsDto;
import com.isp.lg.service.LdapAuthService;
import com.isp.lg.service.SystemSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ldap")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLdapController {

    private final SystemSettingsService systemSettingsService;
    private final LdapAuthService ldapAuthService;

    public AdminLdapController(SystemSettingsService systemSettingsService, LdapAuthService ldapAuthService) {
        this.systemSettingsService = systemSettingsService;
        this.ldapAuthService = ldapAuthService;
    }

    /** 测试与目录连接及服务账号绑定（如已配置 bindDn） */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        SystemSettingsDto.LdapSettings ldap = systemSettingsService.load().getLdap();
        LdapAuthService.LdapTestResult r = ldapAuthService.testConnection(ldap);
        if (r.success()) {
            return ResponseEntity.ok(Map.of("success", true, "message", r.message()));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", r.message()));
    }

    /** 使用当前保存的 LDAP 配置测试指定用户能否登录 */
    @PostMapping("/test-user")
    public ResponseEntity<Map<String, Object>> testUser(@Valid @RequestBody LdapTestRequest body) {
        SystemSettingsDto.LdapSettings ldap = systemSettingsService.load().getLdap();
        LdapAuthService.LdapTestResult r = ldapAuthService.testUserLogin(ldap, body.getTestUsername(), body.getTestPassword());
        if (r.success()) {
            return ResponseEntity.ok(Map.of("success", true, "message", r.message()));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", r.message()));
    }
}
