package com.isp.lg.dto;

public class SystemSettingsDto {

    public static class General {
        private String systemName;
        private Boolean showPopCode;
        private String footerText;
        private String homeIntroText;

        public String getSystemName() { return systemName; }
        public void setSystemName(String systemName) { this.systemName = systemName; }
        public Boolean getShowPopCode() { return showPopCode; }
        public void setShowPopCode(Boolean showPopCode) { this.showPopCode = showPopCode; }
        public String getFooterText() { return footerText; }
        public void setFooterText(String footerText) { this.footerText = footerText; }
        public String getHomeIntroText() { return homeIntroText; }
        public void setHomeIntroText(String homeIntroText) { this.homeIntroText = homeIntroText; }
    }

    public static class DeviceDefaults {
        private String authType;
        private String username;
        private String password;
        private Integer sshPort;
        private Integer telnetPort;
        private Integer timeoutSec;
        private Integer maxConcurrency;

        public String getAuthType() { return authType; }
        public void setAuthType(String authType) { this.authType = authType; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Integer getSshPort() { return sshPort; }
        public void setSshPort(Integer sshPort) { this.sshPort = sshPort; }
        public Integer getTelnetPort() { return telnetPort; }
        public void setTelnetPort(Integer telnetPort) { this.telnetPort = telnetPort; }
        public Integer getTimeoutSec() { return timeoutSec; }
        public void setTimeoutSec(Integer timeoutSec) { this.timeoutSec = timeoutSec; }
        public Integer getMaxConcurrency() { return maxConcurrency; }
        public void setMaxConcurrency(Integer maxConcurrency) { this.maxConcurrency = maxConcurrency; }
    }

    public static class RateLimit {
        private Integer perIpPerMinute;

        public Integer getPerIpPerMinute() { return perIpPerMinute; }
        public void setPerIpPerMinute(Integer perIpPerMinute) { this.perIpPerMinute = perIpPerMinute; }
    }

    public static class Security {
        private Boolean captchaEnabled;
        private Integer logRetainDays;

        public Boolean getCaptchaEnabled() { return captchaEnabled; }
        public void setCaptchaEnabled(Boolean captchaEnabled) { this.captchaEnabled = captchaEnabled; }
        public Integer getLogRetainDays() { return logRetainDays; }
        public void setLogRetainDays(Integer logRetainDays) { this.logRetainDays = logRetainDays; }
    }

    /** LDAP / 认证对接（存储于 system_ldap） */
    public static class LdapSettings {
        private Boolean enabled;
        /** 每行示例：ldap://ldap.example.com:389 或 ldaps://ldap.example.com:636 */
        private String serverUrl;
        private Boolean useTls;
        private String baseDn;
        /** 用于搜索用户的绑定账号，可为空（仅当目录允许匿名搜索时） */
        private String bindDn;
        private String bindPassword;
        private String userSearchBase;
        /** 使用 {0} 表示登录名，例如 (uid={0}) 或 (sAMAccountName={0}) */
        private String userSearchFilter;
        private Integer connectTimeoutMs;
        /** LDAP 校验失败时是否仍允许本地账号密码登录（应急） */
        private Boolean allowLocalFallback;

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public String getServerUrl() { return serverUrl; }
        public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
        public Boolean getUseTls() { return useTls; }
        public void setUseTls(Boolean useTls) { this.useTls = useTls; }
        public String getBaseDn() { return baseDn; }
        public void setBaseDn(String baseDn) { this.baseDn = baseDn; }
        public String getBindDn() { return bindDn; }
        public void setBindDn(String bindDn) { this.bindDn = bindDn; }
        public String getBindPassword() { return bindPassword; }
        public void setBindPassword(String bindPassword) { this.bindPassword = bindPassword; }
        public String getUserSearchBase() { return userSearchBase; }
        public void setUserSearchBase(String userSearchBase) { this.userSearchBase = userSearchBase; }
        public String getUserSearchFilter() { return userSearchFilter; }
        public void setUserSearchFilter(String userSearchFilter) { this.userSearchFilter = userSearchFilter; }
        public Integer getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(Integer connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public Boolean getAllowLocalFallback() { return allowLocalFallback; }
        public void setAllowLocalFallback(Boolean allowLocalFallback) { this.allowLocalFallback = allowLocalFallback; }
    }

    private General general;
    private DeviceDefaults deviceDefaults;
    private RateLimit rateLimit;
    private Security security;
    private LdapSettings ldap;

    public General getGeneral() { return general; }
    public void setGeneral(General general) { this.general = general; }
    public DeviceDefaults getDeviceDefaults() { return deviceDefaults; }
    public void setDeviceDefaults(DeviceDefaults deviceDefaults) { this.deviceDefaults = deviceDefaults; }
    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }
    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }
    public LdapSettings getLdap() { return ldap; }
    public void setLdap(LdapSettings ldap) { this.ldap = ldap; }
}

