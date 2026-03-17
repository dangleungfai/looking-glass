package com.isp.lg.config;

import com.isp.lg.domain.Role;
import com.isp.lg.domain.User;
import com.isp.lg.repository.RoleRepository;
import com.isp.lg.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;
        Role adminRole = roleRepository.findByRoleCode("SUPER_ADMIN")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setRoleName("Super Admin");
                    r.setRoleCode("SUPER_ADMIN");
                    r.setDescription("超级管理员");
                    return roleRepository.save(r);
                });
        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@example.com");
        admin.setRole(adminRole);
        admin.setStatus(1);
        admin.setCreatedAt(Instant.now());
        admin.setUpdatedAt(Instant.now());
        userRepository.save(admin);
    }
}
