package com.isp.lg.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_name", nullable = false, length = 128)
    private String deviceName;

    @Column(name = "device_code", nullable = false, length = 64, unique = true)
    private String deviceCode;

    @Column(name = "vendor", nullable = false, length = 64)
    private String vendor;

    @Column(name = "os_type", nullable = false, length = 64)
    private String osType;

    @Column(name = "mgmt_ip", nullable = false, length = 64)
    private String mgmtIp;

    @Column(name = "ssh_port", nullable = false)
    private Integer sshPort;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @JsonIgnore
    @Column(name = "password_encrypted")
    private byte[] passwordEncrypted;

    @JsonIgnore
    @Column(name = "private_key_encrypted")
    private byte[] privateKeyEncrypted;

    @Column(name = "auth_type", nullable = false, length = 32)
    private String authType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pop_id", nullable = false)
    private Pop pop;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "timeout_sec")
    private Integer timeoutSec;

    @Column(name = "max_concurrency")
    private Integer maxConcurrency;

    @Column(name = "supported_query_types", length = 255)
    private String supportedQueryTypes;

    @Column(name = "remark", length = 255)
    private String remark;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
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
    public byte[] getPasswordEncrypted() { return passwordEncrypted; }
    public void setPasswordEncrypted(byte[] passwordEncrypted) { this.passwordEncrypted = passwordEncrypted; }
    public byte[] getPrivateKeyEncrypted() { return privateKeyEncrypted; }
    public void setPrivateKeyEncrypted(byte[] privateKeyEncrypted) { this.privateKeyEncrypted = privateKeyEncrypted; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public Pop getPop() { return pop; }
    public void setPop(Pop pop) { this.pop = pop; }
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
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
