package com.isp.lg.controller;

import com.isp.lg.domain.Device;
import com.isp.lg.domain.Pop;
import com.isp.lg.dto.DeviceDto;
import com.isp.lg.dto.SystemSettingsDto;
import com.isp.lg.repository.DeviceRepository;
import com.isp.lg.repository.PopRepository;
import com.isp.lg.service.SystemSettingsService;
import com.isp.lg.worker.WorkerClient;
import com.isp.lg.worker.WorkerTaskRequest;
import com.isp.lg.worker.WorkerTaskResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/devices")
public class AdminDeviceController {

    private final DeviceRepository deviceRepository;
    private final PopRepository popRepository;
    private final SystemSettingsService systemSettingsService;
    private final WorkerClient workerClient;

    public AdminDeviceController(DeviceRepository deviceRepository,
                                 PopRepository popRepository,
                                 SystemSettingsService systemSettingsService,
                                 WorkerClient workerClient) {
        this.deviceRepository = deviceRepository;
        this.popRepository = popRepository;
        this.systemSettingsService = systemSettingsService;
        this.workerClient = workerClient;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'READONLY')")
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
                    dto.setAuthType(normalizeAuthType(d.getAuthType()));
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
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ResponseEntity<DeviceDto> create(@RequestBody DeviceBody body) {
        SystemSettingsDto.DeviceDefaults defaults = systemSettingsService.getDeviceDefaults();
        Pop pop = resolvePop(body.getPopId());
        String vendor = requireText(body.getVendor(), "vendor");
        String deviceName = requireText(body.getDeviceName(), "deviceName");
        String mgmtIp = requireText(body.getMgmtIp(), "mgmtIp");
        Device d = new Device();
        d.setDeviceName(deviceName);
        d.setDeviceCode(buildDeviceCode(body.getDeviceCode(), deviceName));
        d.setVendor(vendor);
        d.setOsType(resolveOsType(vendor));
        d.setMgmtIp(mgmtIp);
        String authType = body.getAuthType() != null && !body.getAuthType().isBlank() ? body.getAuthType() : defaults.getAuthType();
        String normalizedAuthType = normalizeAuthType(authType);
        d.setSshPort(body.getSshPort() != null ? body.getSshPort() : defaultPortByAuthType(defaults, normalizedAuthType));
        String username = body.getUsername() != null && !body.getUsername().isBlank() ? body.getUsername() : defaults.getUsername();
        d.setUsername(username != null && !username.isBlank() ? username : "admin");
        String password = body.getPassword() != null ? body.getPassword() : defaults.getPassword();
        d.setPasswordEncrypted(password != null && !password.isBlank() ? password.getBytes(StandardCharsets.UTF_8) : null);
        d.setAuthType(normalizedAuthType);
        d.setPop(pop);
        d.setStatus(body.getStatus() != null ? body.getStatus() : 1);
        d.setPriority(body.getPriority() != null ? body.getPriority() : 0);
        d.setTimeoutSec(body.getTimeoutSec() != null ? body.getTimeoutSec() : (defaults.getTimeoutSec() != null ? defaults.getTimeoutSec() : 10));
        d.setMaxConcurrency(body.getMaxConcurrency() != null ? body.getMaxConcurrency() : (defaults.getMaxConcurrency() != null ? defaults.getMaxConcurrency() : 10));
        d.setSupportedQueryTypes(body.getSupportedQueryTypes());
        d.setRemark(body.getRemark());
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(toDto(deviceRepository.save(d)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ResponseEntity<DeviceDto> update(@PathVariable Long id, @RequestBody DeviceBody body) {
        Device d = deviceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Device not found"));
        SystemSettingsDto.DeviceDefaults defaults = systemSettingsService.getDeviceDefaults();
        if (body.getDeviceName() != null && !body.getDeviceName().isBlank()) {
            d.setDeviceName(body.getDeviceName().trim());
        }
        if (body.getVendor() != null && !body.getVendor().isBlank()) {
            String vendor = body.getVendor().trim();
            d.setVendor(vendor);
            d.setOsType(resolveOsType(vendor));
        }
        if (body.getMgmtIp() != null && !body.getMgmtIp().isBlank()) {
            d.setMgmtIp(body.getMgmtIp().trim());
        }
        d.setSshPort(body.getSshPort() != null ? body.getSshPort() : d.getSshPort());
        if (d.getSshPort() == null && defaults.getSshPort() != null) {
            d.setSshPort(defaultPortByAuthType(defaults, d.getAuthType()));
        }
        if (body.getUsername() != null && !body.getUsername().isBlank()) {
            d.setUsername(body.getUsername().trim());
        } else {
            d.setUsername(defaults.getUsername() != null && !defaults.getUsername().isBlank() ? defaults.getUsername() : "admin");
        }
        if (body.getPassword() != null && !body.getPassword().isEmpty()) {
            d.setPasswordEncrypted(body.getPassword().getBytes(StandardCharsets.UTF_8));
        } else {
            String defaultPassword = defaults.getPassword();
            d.setPasswordEncrypted(defaultPassword != null && !defaultPassword.isBlank()
                    ? defaultPassword.getBytes(StandardCharsets.UTF_8)
                    : null);
        }
        if (body.getAuthType() != null && !body.getAuthType().isBlank()) {
            d.setAuthType(normalizeAuthType(body.getAuthType()));
        } else if (d.getAuthType() == null || d.getAuthType().isBlank()) {
            d.setAuthType(normalizeAuthType(defaults.getAuthType()));
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
        return ResponseEntity.ok(toDto(deviceRepository.save(d)));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ResponseEntity<DeviceDto> updateStatus(@PathVariable Long id, @RequestBody StatusBody body) {
        Device d = deviceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Device not found"));
        int status = body.getStatus() != null && body.getStatus() == 0 ? 0 : 1;
        d.setStatus(status);
        d.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(toDto(deviceRepository.save(d)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        if (!deviceRepository.existsById(id)) throw new IllegalArgumentException("Device not found");
        deviceRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "deleted"));
    }

    @PostMapping("/{id}/login-test")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ResponseEntity<Map<String, Object>> loginTest(@PathVariable Long id, @RequestBody LoginTestBody body) {
        Device device = deviceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Device not found"));
        String command = body != null && body.getCommand() != null ? body.getCommand().trim() : "";
        if (command.isBlank()) {
            throw new IllegalArgumentException("command is required");
        }
        if (!isWhitelistedCommand(command)) {
            throw new IllegalArgumentException("仅允许执行白名单命令：ping / traceroute / show");
        }
        if (containsBlockedTokens(command)) {
            throw new IllegalArgumentException("命令包含不允许的字符");
        }

        SystemSettingsDto.DeviceDefaults defaults = systemSettingsService.getDeviceDefaults();
        String authType = normalizeAuthType(
                device.getAuthType() != null && !device.getAuthType().isBlank()
                        ? device.getAuthType()
                        : defaults.getAuthType()
        );
        int port = device.getSshPort() != null ? device.getSshPort() : defaultPortByAuthType(defaults, authType);
        String requestedUsername = body != null ? body.getUsername() : null;
        String defaultUsername = defaults.getUsername();
        String deviceUsername = device.getUsername();
        String username = (requestedUsername != null && !requestedUsername.isBlank())
                ? requestedUsername.trim()
                : (defaultUsername != null && !defaultUsername.isBlank())
                ? defaultUsername.trim()
                : (deviceUsername != null && !deviceUsername.isBlank())
                ? deviceUsername.trim()
                : "admin";
        String devicePassword = device.getPasswordEncrypted() != null
                ? new String(device.getPasswordEncrypted(), StandardCharsets.UTF_8)
                : null;
        String requestedPassword = body != null ? body.getPassword() : null;
        String credentialSource;
        String defaultPassword = defaults.getPassword();
        String password = (requestedPassword != null && !requestedPassword.isBlank())
                ? requestedPassword
                : (defaultPassword != null && !defaultPassword.isBlank())
                ? defaultPassword
                : devicePassword;
        if (requestedPassword != null && !requestedPassword.isBlank()) {
            credentialSource = "REQUEST_OVERRIDE";
        } else if (defaultPassword != null && !defaultPassword.isBlank()) {
            credentialSource = "SYSTEM_DEFAULT";
        } else {
            credentialSource = "DEVICE";
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("设备未配置可用密码，请先在设备中配置密码");
        }
        if ("TELNET".equals(authType)) {
            throw new IllegalArgumentException("登录测试当前仅支持 SSH 设备，请将登录方式改为 SSH 后重试");
        }

        WorkerTaskRequest task = new WorkerTaskRequest();
        task.setRequestId("ADM" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        task.setMgmtIp(device.getMgmtIp());
        task.setSshPort(port);
        task.setUsername(username);
        task.setPassword(password);
        task.setCommand(command);
        task.setQueryType(inferQueryType(command));
        task.setTimeoutSec(device.getTimeoutSec() != null ? device.getTimeoutSec() : 15);

        WorkerTaskResponse resp = workerClient.execute(task);
        Map<String, Object> result = new HashMap<>();
        result.put("requestId", task.getRequestId());
        result.put("deviceId", device.getId());
        result.put("deviceName", device.getDeviceName());
        result.put("mgmtIp", device.getMgmtIp());
        result.put("authType", authType);
        result.put("sshPort", port);
        result.put("username", username);
        result.put("credentialSource", credentialSource);
        result.put("command", command);
        result.put("status", resp != null && resp.getStatus() != null ? resp.getStatus() : "FAILED");
        result.put("durationMs", resp != null ? resp.getDurationMs() : null);
        result.put("output", resp != null ? resp.getRawText() : null);
        result.put("errorMessage", resp != null ? resp.getErrorMessage() : "Worker 未返回结果");
        return ResponseEntity.ok(result);
    }

    public static class DeviceBody {
        private String deviceCode;
        private String deviceName;
        private String vendor;
        private String osType;
        private String mgmtIp;
        private Integer sshPort;
        private String username;
        private String password;
        private String authType;
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

    public static class StatusBody {
        private Integer status;

        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    public static class LoginTestBody {
        private String command;
        private String username;
        private String password;
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    private DeviceDto toDto(Device d) {
        DeviceDto dto = new DeviceDto();
        dto.setId(d.getId());
        dto.setDeviceCode(d.getDeviceCode());
        dto.setDeviceName(d.getDeviceName());
        dto.setVendor(d.getVendor());
        dto.setOsType(d.getOsType());
        dto.setMgmtIp(d.getMgmtIp());
        dto.setSshPort(d.getSshPort());
        dto.setAuthType(normalizeAuthType(d.getAuthType()));
        dto.setUsername(d.getUsername());
        dto.setStatus(d.getStatus());
        dto.setPriority(d.getPriority());
        dto.setTimeoutSec(d.getTimeoutSec());
        dto.setSupportedQueryTypes(d.getSupportedQueryTypes());
        dto.setRemark(d.getRemark());
        Pop pop = d.getPop();
        dto.setPopCode(pop != null ? pop.getPopCode() : null);
        return dto;
    }

    private Pop resolvePop(Long popId) {
        if (popId != null) {
            return popRepository.findById(popId).orElseThrow(() -> new IllegalArgumentException("POP not found"));
        }
        return popRepository.findAll().stream().filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No POP available, please create POP first"));
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String buildDeviceCode(String code, String name) {
        if (code != null && !code.isBlank()) {
            return code.trim();
        }
        String source = name == null ? "DEVICE" : name;
        String normalized = source.trim().toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) normalized = "DEVICE";
        return normalized + "_" + (System.currentTimeMillis() % 100000);
    }

    private String resolveOsType(String vendor) {
        if (vendor == null) return "GENERIC";
        switch (vendor) {
            case "CISCO_IOS_XR":
                return "IOS-XR";
            case "JUNIPER_JUNOS":
                return "JUNOS";
            case "HUAWEI_VRP":
                return "VRP";
            case "MIKROTIK_ROUTEROS":
                return "ROUTEROS";
            default:
                return "GENERIC";
        }
    }

    private String normalizeAuthType(String authType) {
        String normalized = authType == null ? "SSH" : authType.trim().toUpperCase();
        if ("PASSWORD".equals(normalized) || "SSH_KEY".equals(normalized)) {
            return "SSH";
        }
        if (!"SSH".equals(normalized) && !"TELNET".equals(normalized)) {
            return "SSH";
        }
        return normalized;
    }

    private int defaultPortByAuthType(SystemSettingsDto.DeviceDefaults defaults, String authType) {
        String mode = normalizeAuthType(authType);
        if ("TELNET".equals(mode)) {
            return defaults.getTelnetPort() != null ? defaults.getTelnetPort() : 23;
        }
        return defaults.getSshPort() != null ? defaults.getSshPort() : 22;
    }

    private boolean isWhitelistedCommand(String command) {
        String c = command == null ? "" : command.trim().toLowerCase();
        return c.startsWith("ping ")
                || "ping".equals(c)
                || c.startsWith("traceroute ")
                || "traceroute".equals(c)
                || c.startsWith("show ")
                || "show".equals(c);
    }

    private boolean containsBlockedTokens(String command) {
        if (command == null) return true;
        String c = command;
        return c.contains(";")
                || c.contains("&&")
                || c.contains("||")
                || c.contains("`")
                || c.contains("$(")
                || c.contains("\n")
                || c.contains("\r");
    }

    private String inferQueryType(String command) {
        String c = command == null ? "" : command.trim().toLowerCase();
        if (c.startsWith("ping")) return "PING";
        if (c.startsWith("traceroute")) return "TRACEROUTE";
        return "ADMIN_SHOW";
    }
}
