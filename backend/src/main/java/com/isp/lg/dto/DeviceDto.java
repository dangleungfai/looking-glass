package com.isp.lg.dto;

public class DeviceDto {

    private Long id;
    private String deviceCode;
    private String deviceName;
    private String vendor;
    private String osType;
    private String mgmtIp;
    private Integer sshPort;
    private String authType;
    private String username;
    private Integer status;
    private Integer priority;
    private Integer timeoutSec;
    private String supportedQueryTypes;
    private String remark;
    private String popCode;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Integer getTimeoutSec() { return timeoutSec; }
    public void setTimeoutSec(Integer timeoutSec) { this.timeoutSec = timeoutSec; }
    public String getSupportedQueryTypes() { return supportedQueryTypes; }
    public void setSupportedQueryTypes(String supportedQueryTypes) { this.supportedQueryTypes = supportedQueryTypes; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getPopCode() { return popCode; }
    public void setPopCode(String popCode) { this.popCode = popCode; }
}

