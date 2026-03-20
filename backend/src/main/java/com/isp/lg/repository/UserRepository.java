package com.isp.lg.repository;

import com.isp.lg.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    long countByRole_RoleCode(String roleCode);

    long countByUserType(String userType);
}
