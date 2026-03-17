package com.isp.lg.repository;

import com.isp.lg.domain.IpBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IpBlacklistRepository extends JpaRepository<IpBlacklist, Long> {
    List<IpBlacklist> findByStatus(Integer status);
}
