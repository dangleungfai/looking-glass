package com.isp.lg.dto;

import jakarta.validation.constraints.NotBlank;

public class LdapTestRequest {
    @NotBlank
    private String testUsername;
    @NotBlank
    private String testPassword;

    public String getTestUsername() {
        return testUsername;
    }

    public void setTestUsername(String testUsername) {
        this.testUsername = testUsername;
    }

    public String getTestPassword() {
        return testPassword;
    }

    public void setTestPassword(String testPassword) {
        this.testPassword = testPassword;
    }
}
