package com.isp.lg.controller;

import com.isp.lg.domain.Role;
import com.isp.lg.repository.RoleRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleController {

    private final RoleRepository roleRepository;

    public AdminRoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return roleRepository.findAll().stream()
                .filter(r -> "ADMIN".equals(r.getRoleCode()) || "OPS".equals(r.getRoleCode()) || "READONLY".equals(r.getRoleCode()))
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toMap(Role r) {
        return Map.of(
                "id", r.getId(),
                "roleCode", r.getRoleCode(),
                "roleName", r.getRoleName() != null ? r.getRoleName() : r.getRoleCode()
        );
    }
}
