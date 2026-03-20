package com.isp.lg.controller;

import com.isp.lg.domain.Role;
import com.isp.lg.domain.User;
import com.isp.lg.dto.LoginRequest;
import com.isp.lg.dto.LoginResponse;
import com.isp.lg.dto.SystemSettingsDto;
import com.isp.lg.repository.RoleRepository;
import com.isp.lg.repository.UserRepository;
import com.isp.lg.security.JwtTokenService;
import com.isp.lg.service.LdapAuthService;
import com.isp.lg.service.SystemSettingsService;
import com.unboundid.ldap.sdk.LDAPException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final SystemSettingsService systemSettingsService;
    private final LdapAuthService ldapAuthService;

    public AdminAuthController(AuthenticationManager authenticationManager,
                               UserRepository userRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder,
                               JwtTokenService jwtTokenService,
                               SystemSettingsService systemSettingsService,
                               LdapAuthService ldapAuthService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.systemSettingsService = systemSettingsService;
        this.ldapAuthService = ldapAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String username = request.getUsername().trim();
        SystemSettingsDto.LdapSettings ldap = systemSettingsService.load().getLdap();
        boolean ldapEnabled = ldap != null && Boolean.TRUE.equals(ldap.getEnabled());
        boolean ldapOk = false;

        if (ldapEnabled) {
            try {
                ldapAuthService.authenticateUser(ldap, username, request.getPassword());
                ldapOk = true;
            } catch (LDAPException e) {
                if (!Boolean.TRUE.equals(ldap.getAllowLocalFallback())) {
                    throw new BadCredentialsException("LDAP 认证失败: " + e.getMessage());
                }
            } catch (RuntimeException e) {
                if (!Boolean.TRUE.equals(ldap.getAllowLocalFallback())) {
                    throw new BadCredentialsException("LDAP 认证失败: " + e.getMessage());
                }
            }
        }

        if (!ldapOk) {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            SecurityContextHolder.getContext().setAuthentication(null);
        }

        User user = ldapOk
                ? userRepository.findByUsername(username).orElseGet(() -> autoProvisionReadonlyLdapUser(username))
                : userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("用户未在本系统建档，请联系管理员开通账号"));
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BadCredentialsException("账号已禁用");
        }
        String roleCode = user.getRole().getRoleCode();
        String token = jwtTokenService.generateToken(user.getUsername(), roleCode);

        return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), roleCode));
    }

    private User autoProvisionReadonlyLdapUser(String username) {
        Role readonly = roleRepository.findByRoleCode("READONLY")
                .or(() -> roleRepository.findByRoleCode("READONLY_ADMIN"))
                .orElseThrow(() -> new IllegalStateException("系统缺少默认角色 READONLY"));
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        u.setUserType("LDAP");
        u.setRole(readonly);
        u.setStatus(1);
        Instant now = Instant.now();
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        return userRepository.save(u);
    }
}
