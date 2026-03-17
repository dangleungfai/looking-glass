package com.isp.lg.repository;

import com.isp.lg.domain.QueryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryLogRepository extends JpaRepository<QueryLog, Long> {
    Page<QueryLog> findByOrderByCreatedAtDesc(Pageable pageable);
}
