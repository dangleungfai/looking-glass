package com.isp.lg.repository;

import com.isp.lg.domain.CommandTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommandTemplateRepository extends JpaRepository<CommandTemplate, Long> {
    Optional<CommandTemplate> findByVendorAndOsTypeAndQueryTypeAndStatus(String vendor, String osType, String queryType, Integer status);
    List<CommandTemplate> findByVendorAndOsTypeAndStatus(String vendor, String osType, Integer status);

    @Query("select distinct c.queryType from CommandTemplate c where c.status = 1 and c.isPublic = 1 and c.queryType is not null and c.queryType <> ''")
    List<String> findEnabledPublicQueryTypes();
}
