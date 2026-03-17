package com.isp.lg.repository;

import com.isp.lg.domain.Pop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PopRepository extends JpaRepository<Pop, Long> {
    Optional<Pop> findByPopCode(String popCode);
    List<Pop> findByIsPublicAndStatusOrderBySortOrderAsc(Integer isPublic, Integer status);
}
