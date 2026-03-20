package com.isp.lg.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoleModelUpdater implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public RoleModelUpdater(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ensureRole("ADMIN", "管理员", "系统管理员");
        ensureRole("OPS", "运维", "运维管理员");
        ensureRole("READONLY", "只读", "只读用户");

        remapUsers("SUPER_ADMIN", "ADMIN");
        remapUsers("NETWORK_ADMIN", "OPS");
        remapUsers("READONLY_ADMIN", "READONLY");
        remapUsers("AUDITOR", "READONLY");
    }

    private void ensureRole(String roleCode, String roleName, String desc) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE role_code = ?",
                Integer.class,
                roleCode
        );
        if (count == null || count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO roles(role_name, role_code, description) VALUES(?,?,?)",
                    roleName, roleCode, desc
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE roles SET role_name = ?, description = ? WHERE role_code = ?",
                    roleName, desc, roleCode
            );
        }
    }

    private void remapUsers(String fromRoleCode, String toRoleCode) {
        jdbcTemplate.update(
                "UPDATE users u " +
                        "JOIN roles rf ON u.role_id = rf.id " +
                        "JOIN roles rt ON rt.role_code = ? " +
                        "SET u.role_id = rt.id " +
                        "WHERE rf.role_code = ?",
                toRoleCode, fromRoleCode
        );
    }
}
