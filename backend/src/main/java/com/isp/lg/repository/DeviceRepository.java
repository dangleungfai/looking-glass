package com.isp.lg.repository;

import com.isp.lg.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceCode(String deviceCode);
    List<Device> findByPopIdAndStatusOrderByPriorityDesc(Long popId, Integer status);
    long countByPopId(Long popId);
}
