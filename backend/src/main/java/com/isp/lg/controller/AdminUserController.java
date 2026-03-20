package com.isp.lg.controller;

import com.isp.lg.domain.Role;
import com.isp.lg.domain.User;
import com.isp.lg.dto.AdminUserCreateRequest;
import com.isp.lg.dto.AdminUserResponse;
import com.isp.lg.dto.AdminUserUpdateRequest;
import com.isp.lg.repository.RoleRepository;
import com.isp.lg.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .sorted(Comparator
                        .comparing((AdminUserResponse r) -> !"admin".equalsIgnoreCase(r.getUsername()))
                        .thenComparing(AdminUserResponse::getUsername, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<AdminUserResponse> create(@Valid @RequestBody AdminUserCreateRequest req) {
        if (userRepository.findByUsername(req.getUsername().trim()).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        Role role = roleRepository.findByRoleCode(req.getRoleCode().trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("无效的角色: " + req.getRoleCode()));
        User u = new User();
        u.setUsername(req.getUsername().trim());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setUserType("LOCAL");
        u.setRole(role);
        u.setEmail(req.getEmail() != null ? req.getEmail().trim() : null);
        u.setMobile(req.getMobile() != null ? req.getMobile().trim() : null);
        u.setStatus(req.getStatus() != null && req.getStatus() == 0 ? 0 : 1);
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        userRepository.save(u);
        return ResponseEntity.ok(toResponse(u));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponse> update(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateRequest req) {
        User u = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }
        if (req.getRoleCode() != null && !req.getRoleCode().isBlank()) {
            String rc = req.getRoleCode().trim().toUpperCase();
            Role role = roleRepository.findByRoleCode(rc)
                    .orElseThrow(() -> new IllegalArgumentException("无效的角色: " + rc));
            if ("ADMIN".equals(u.getRole().getRoleCode()) && !"ADMIN".equals(rc)) {
                long cnt = userRepository.countByRole_RoleCode("ADMIN");
                if (cnt <= 1) {
                    throw new IllegalStateException("不能移除唯一管理员角色");
                }
            }
            u.setRole(role);
        }
        if (req.getEmail() != null) {
            u.setEmail(req.getEmail().isBlank() ? null : req.getEmail().trim());
        }
        if (req.getMobile() != null) {
            u.setMobile(req.getMobile().isBlank() ? null : req.getMobile().trim());
        }
        if (req.getStatus() != null) {
            if (req.getStatus() == 0 && "ADMIN".equals(u.getRole().getRoleCode())) {
                long cnt = userRepository.countByRole_RoleCode("ADMIN");
                if (cnt <= 1) {
                    throw new IllegalStateException("不能禁用唯一管理员");
                }
            }
            u.setStatus(req.getStatus());
        }
        u.setUpdatedAt(Instant.now());
        userRepository.save(u);
        return ResponseEntity.ok(toResponse(u));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        String self = currentUsername();
        User u = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if ("admin".equalsIgnoreCase(u.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "admin 为系统内置账号，不能删除"));
        }
        if (self != null && self.equalsIgnoreCase(u.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "不能删除当前登录用户"));
        }
        if ("ADMIN".equals(u.getRole().getRoleCode())) {
            long cnt = userRepository.countByRole_RoleCode("ADMIN");
            if (cnt <= 1) {
                return ResponseEntity.badRequest().body(Map.of("error", "不能删除唯一管理员"));
            }
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private AdminUserResponse toResponse(User u) {
        AdminUserResponse r = new AdminUserResponse();
        r.setId(u.getId());
        r.setUsername(u.getUsername());
        r.setEmail(u.getEmail());
        r.setMobile(u.getMobile());
        r.setUserType(u.getUserType() == null || u.getUserType().isBlank() ? "LOCAL" : u.getUserType());
        r.setStatus(u.getStatus());
        r.setCreatedAt(u.getCreatedAt());
        r.setUpdatedAt(u.getUpdatedAt());
        if (u.getRole() != null) {
            r.setRoleCode(u.getRole().getRoleCode());
            r.setRoleName(u.getRole().getRoleName());
        }
        return r;
    }
}
