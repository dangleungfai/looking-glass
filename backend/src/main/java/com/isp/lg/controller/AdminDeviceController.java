package com.isp.lg.controller;

import com.isp.lg.domain.Device;
import com.isp.lg.domain.Pop;
import com.isp.lg.dto.DeviceDto;
import com.isp.lg.repository.DeviceRepository;
import com.isp.lg.repository.PopRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/devices")
public class AdminDeviceController {

    private final DeviceRepository deviceRepository;
    private final PopRepository popRepository;

    public AdminDeviceController(DeviceRepository deviceRepository, PopRepository popRepository) {
        this.deviceRepository = deviceRepository;
        this.popRepository = popRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'NETWORK_ADMIN', 'READONLY_ADMIN', 'AUDITOR')")
    public ResponseEntity<List<DeviceDto>> list() {
        List<DeviceDto> dtos = deviceRepository.findAll()
                .stream()
                .map(d -> {
                    DeviceDto dto = new DeviceDto();
                    dto.setId(d.getId());
                    dto.setDeviceCode(d.getDeviceCode());
                    dto.setDeviceName(d.getDeviceName());
                    dto.setVendor(d.getVendor());
                    dto.setOsType(d.getOsType());
                    dto.setMgmtIp(d.getMgmtIp());
                    dto.setSshPort(d.getSshPort());
                    dto.setUsername(d.getUsername());
                    dto.setStatus(d.getStatus());
                    dto.setPriority(d.getPriority());
                    dto.setTimeoutSec(d.getTimeoutSec());
                    dto.setSupportedQueryTypes(d.getSupportedQueryTypes());
                    dto.setRemark(d.getRemark());
                    Pop pop = d.getPop();
                    dto.setPopCode(pop != null ? pop.getPopCode() : null);
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'NETWORK_ADMIN')")
    public ResponseEntity<Device> create(@Valid @RequestBody DeviceBody body) {
        Pop pop = popRepository.findById(body.getPopId()).orElseThrow(() -> new IllegalArgumentException("POP not found"));
        Device d = new Device();
        d.setDeviceName(body.getDeviceName());
        d.setDeviceCode(body.getDeviceCode());
        d.setVendor(body.getVendor());
        d.setOsType(body.getOsType());
        d.setMgmtIp(body.getMgmtIp());
        d.setSshPort(body.getSshPort() != null ? body.getSshPort() : 22);
        d.setUsername(body.getUsername());
        d.setPasswordEncrypted(body.getPassword() != null ? body.getPassword().getBytes(StandardCharsets.UTF_8) : null);
        d.setAuthType(body.getAuthType() != null ? body.getAuthType() : "PASSWORD");
        d.setPop(pop);
        d.setStatus(body.getStatus() != null ? body.getStatus() : 1);
        d.setPriority(body.getPriority() != null ? body.getPriority() : 0);
        d.setTimeoutSec(body.getTimeoutSec() != null ? body.getTimeoutSec() : 10);
        d.setMaxConcurrency(body.getMaxConcurrency() != null ? body.getMaxConcurrency() : 10);
        d.setSupportedQueryTypes(body.getSupportedQueryTypes());
        d.setRemark(body.getRemark());
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(deviceRepository.save(d));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'NETWORK_ADMIN')")
    public ResponseEntity<Device> update(@PathVariable Long id, @Valid @RequestBody DeviceBody body) {
        Device d = deviceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Device not found"));
        d.setDeviceName(body.getDeviceName());
        d.setVendor(body.getVendor());
        d.setOsType(body.getOsType());
        d.setMgmtIp(body.getMgmtIp());
        d.setSshPort(body.getSshPort() != null ? body.getSshPort() : d.getSshPort());
        d.setUsername(body.getUsername());
        if (body.getPassword() != null && !body.getPassword().isEmpty()) {
            d.setPasswordEncrypted(body.getPassword().getBytes(StandardCharsets.UTF_8));
        }
        d.setStatus(body.getStatus() != null ? body.getStatus() : d.getStatus());
        d.setPriority(body.getPriority() != null ? body.getPriority() : d.getPriority());
        d.setTimeoutSec(body.getTimeoutSec() != null ? body.getTimeoutSec() : d.getTimeoutSec());
        d.setMaxConcurrency(body.getMaxConcurrency() != null ? body.getMaxConcurrency() : d.getMaxConcurrency());
        d.setSupportedQueryTypes(body.getSupportedQueryTypes());
        d.setRemark(body.getRemark());
        if (body.getPopId() != null) {
            Pop pop = popRepository.findById(body.getPopId()).orElseThrow(() -> new IllegalArgumentException("POP not found"));
            d.setPop(pop);
        }
        d.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(deviceRepository.save(d));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'NETWORK_ADMIN')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        if (!deviceRepository.existsById(id)) throw new IllegalArgumentException("Device not found");
        deviceRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "deleted"));
    }

    public static class DeviceBody {
        private String deviceCode;
        @NotBlank
        private String deviceName;
        @NotBlank
        private String vendor;
        @NotBlank
        private String osType;
        @NotBlank
        private String mgmtIp;
        private Integer sshPort;
        @NotBlank
        private String username;
        private String password;
        private String authType;
        @NotBlank
        private Long popId;
        private Integer status;
        private Integer priority;
        private Integer timeoutSec;
        private Integer maxConcurrency;
        private String supportedQueryTypes;
        private String remark;

        public String getDeviceCode() { return deviceCode; }
        public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        public String getVendor() { return vendor; }
        public void setVendor(String vendor) { this.vendor = vendor; }
        public String getOsType() { return osType; }
        public void setOsType(String osType) { this.osType = osType; }
        public String getMgmtIp() { return mgmtIp; }
        public void setMgmtIp(String mgmtIp) { this.mgmtIp = mgmtIp; }
        public Integer getSshPort() { return sshPort; }
        public void setSshPort(Integer sshPort) { this.sshPort = sshPort; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getAuthType() { return authType; }
        public void setAuthType(String authType) { this.authType = authType; }
        public Long getPopId() { return popId; }
        public void setPopId(Long popId) { this.popId = popId; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        public Integer getTimeoutSec() { return timeoutSec; }
        public void setTimeoutSec(Integer timeoutSec) { this.timeoutSec = timeoutSec; }
        public Integer getMaxConcurrency() { return maxConcurrency; }
        public void setMaxConcurrency(Integer maxConcurrency) { this.maxConcurrency = maxConcurrency; }
        public String getSupportedQueryTypes() { return supportedQueryTypes; }
        public void setSupportedQueryTypes(String supportedQueryTypes) { this.supportedQueryTypes = supportedQueryTypes; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
