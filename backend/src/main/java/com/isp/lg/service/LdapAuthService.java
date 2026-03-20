package com.isp.lg.service;

import com.isp.lg.dto.SystemSettingsDto;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLSocketFactory;
import java.net.URI;

@Service
public class LdapAuthService {

    public void authenticateUser(SystemSettingsDto.LdapSettings ldap, String username, String password) throws LDAPException {
        if (ldap == null || !Boolean.TRUE.equals(ldap.getEnabled())) {
            throw new IllegalStateException("LDAP 未启用");
        }
        validateInputs(username, password);
        try (LDAPConnection connection = connect(ldap)) {
            doUserBind(ldap, connection, username.trim(), password);
        }
    }

    /**
     * 使用当前已保存配置测试绑定：先管理员绑定（如配置），再定位用户 DN 并以测试密码绑定。
     */
    public LdapTestResult testUserLogin(SystemSettingsDto.LdapSettings ldap, String testUsername, String testPassword) {
        if (ldap == null) {
            return LdapTestResult.fail("LDAP 配置为空");
        }
        try {
            validateInputs(testUsername, testPassword);
        } catch (IllegalArgumentException e) {
            return LdapTestResult.fail(e.getMessage());
        }
        try (LDAPConnection connection = connect(ldap)) {
            doUserBind(ldap, connection, testUsername.trim(), testPassword);
            return LdapTestResult.ok("LDAP 用户认证成功");
        } catch (LDAPException e) {
            return LdapTestResult.fail("LDAP 错误: " + e.getMessage());
        } catch (Exception e) {
            return LdapTestResult.fail(e.getMessage());
        }
    }

    /** 仅测试与目录的网络连通及管理员绑定（如有） */
    public LdapTestResult testConnection(SystemSettingsDto.LdapSettings ldap) {
        if (ldap == null) {
            return LdapTestResult.fail("LDAP 配置为空");
        }
        try (LDAPConnection connection = connect(ldap)) {
            if (ldap.getBindDn() != null && !ldap.getBindDn().isBlank()) {
                BindRequest bindReq = new SimpleBindRequest(ldap.getBindDn(), ldap.getBindPassword());
                connection.bind(bindReq);
            }
            return LdapTestResult.ok("连接成功" + (ldap.getBindDn() != null && !ldap.getBindDn().isBlank() ? "，服务账号绑定成功" : ""));
        } catch (LDAPException e) {
            return LdapTestResult.fail("LDAP 错误: " + e.getMessage());
        } catch (Exception e) {
            return LdapTestResult.fail(e.getMessage());
        }
    }

    private void doUserBind(SystemSettingsDto.LdapSettings ldap, LDAPConnection connection, String username, String password)
            throws LDAPException {
        if (ldap.getBindDn() != null && !ldap.getBindDn().isBlank()) {
            connection.bind(new SimpleBindRequest(ldap.getBindDn(), ldap.getBindPassword() != null ? ldap.getBindPassword() : ""));
        }
        String base = ldap.getUserSearchBase();
        if (base == null || base.isBlank()) {
            throw new LDAPException(ResultCode.PARAM_ERROR, "请配置用户搜索路径 userSearchBase");
        }
        String pattern = ldap.getUserSearchFilter();
        if (!pattern.contains("{0}")) {
            throw new LDAPException(ResultCode.PARAM_ERROR, "userSearchFilter 须包含占位符 {0} 表示登录名");
        }
        String filterStr = pattern.replace("{0}", Filter.encodeValue(username));
        SearchResult searchResult = connection.search(base, SearchScope.SUB, filterStr, "dn");
        if (searchResult.getEntryCount() == 0) {
            throw new LDAPException(ResultCode.NO_RESULTS_RETURNED, "未找到用户: " + username);
        }
        if (searchResult.getEntryCount() > 1) {
            throw new LDAPException(ResultCode.PARAM_ERROR, "匹配到多个用户，请收紧 userSearchFilter");
        }
        SearchResultEntry entry = searchResult.getSearchEntries().get(0);
        String userDn = entry.getDN();
        connection.bind(new SimpleBindRequest(userDn, password));
    }

    private LDAPConnection connect(SystemSettingsDto.LdapSettings ldap) throws LDAPException {
        try {
            String url = ldap.getServerUrl();
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("请填写 serverUrl");
            }
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null) {
                throw new IllegalArgumentException("serverUrl 需包含协议，如 ldap:// 或 ldaps://");
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("无法解析 LDAP 主机");
            }
            int port = uri.getPort();
            if (port < 1) {
                port = "ldaps".equalsIgnoreCase(scheme) ? 636 : 389;
            }
            boolean ldaps = "ldaps".equalsIgnoreCase(scheme);
            int timeout = ldap.getConnectTimeoutMs() != null ? ldap.getConnectTimeoutMs() : 5000;

            LDAPConnection connection;
            if (ldaps) {
                SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
                SSLSocketFactory sslSocketFactory = sslUtil.createSSLSocketFactory();
                connection = new LDAPConnection(sslSocketFactory);
                connection.connect(host, port, timeout);
            } else {
                connection = new LDAPConnection();
                connection.connect(host, port, timeout);
            }
            if (!ldaps && Boolean.TRUE.equals(ldap.getUseTls())) {
                SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
                SSLSocketFactory sslSocketFactory = sslUtil.createSSLSocketFactory();
                connection.processExtendedOperation(new StartTLSExtendedRequest(sslSocketFactory));
            }
            return connection;
        } catch (LDAPException e) {
            throw e;
        } catch (Exception e) {
            throw new LDAPException(ResultCode.CONNECT_ERROR, e.getMessage(), e);
        }
    }

    private void validateInputs(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
    }

    public record LdapTestResult(boolean success, String message) {
        static LdapTestResult ok(String message) {
            return new LdapTestResult(true, message);
        }

        static LdapTestResult fail(String message) {
            return new LdapTestResult(false, message);
        }
    }
}
