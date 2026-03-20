package com.isp.lg.dto;

import jakarta.validation.constraints.Size;

public class AdminUserUpdateRequest {
    /** 留空表示不修改 */
    @Size(min = 6, max = 128)
    private String password;
    private String roleCode;
    private String email;
    private String mobile;
    private Integer status;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
