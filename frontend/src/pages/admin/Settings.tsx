import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Card,
  Form,
  Input,
  InputNumber,
  Select,
  Switch,
  Button,
  message,
  Spin,
  Typography,
  Divider,
  Modal,
  Space,
} from 'antd';
import { admin, systemSettings, type SystemSettingsDto } from '../../services/api';
import { SettingsUsersPanel } from './SettingsUsersPanel';
import { resolveCurrentRole } from '../../utils/role';
import { bumpLogoVersion } from '../../hooks/useLogoUrl';
import { DEFAULT_HOME_INTRO_TEXT } from '../../constants/systemContent';

const { Title, Text } = Typography;
const DEFAULT_FOOTER_TEXT = '© 2026 ISP Looking Glass. All rights reserved.';

type SettingsSection = 'general' | 'device' | 'security' | 'users' | 'auth';
const SETTINGS_SECTION_CACHE_KEY = 'lg_admin_settings_section';

function isValidSettingsSection(v: string | null): v is SettingsSection {
  return v === 'general' || v === 'device' || v === 'security' || v === 'users' || v === 'auth';
}

export const Settings: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [data, setData] = useState<SystemSettingsDto | null>(null);
  const [activeSection, setActiveSection] = useState<SettingsSection>(() => {
    const cached = localStorage.getItem(SETTINGS_SECTION_CACHE_KEY);
    return isValidSettingsSection(cached) ? cached : 'general';
  });
  const [form] = Form.useForm<SystemSettingsDto>();
  const [ldapTestOpen, setLdapTestOpen] = useState(false);
  const [ldapTestLoading, setLdapTestLoading] = useState(false);
  const [ldapConnLoading, setLdapConnLoading] = useState(false);
  const [ldapTestForm] = Form.useForm<{ testUsername: string; testPassword: string }>();
  const [logoUploadLoading, setLogoUploadLoading] = useState(false);
  const [sslUploadLoading, setSslUploadLoading] = useState(false);
  const [devicePasswordVisible, setDevicePasswordVisible] = useState(false);
  const [ldapBindPasswordVisible, setLdapBindPasswordVisible] = useState(false);
  const [ldapTestPasswordVisible, setLdapTestPasswordVisible] = useState(false);
  const [sslModalOpen, setSslModalOpen] = useState(false);
  const [sslFullchainFile, setSslFullchainFile] = useState<File | null>(null);
  const [sslPrivkeyFile, setSslPrivkeyFile] = useState<File | null>(null);
  const logoInputRef = useRef<HTMLInputElement | null>(null);
  const sslFullchainInputRef = useRef<HTMLInputElement | null>(null);
  const sslPrivkeyInputRef = useRef<HTMLInputElement | null>(null);

  const roleCode = useMemo(() => resolveCurrentRole(), []);
  const isAdmin = roleCode === 'ADMIN';
  const canEditSettings = roleCode === 'ADMIN';

  useEffect(() => {
    if (!isAdmin && (activeSection === 'users' || activeSection === 'auth')) {
      setActiveSection('general');
    }
  }, [activeSection, isAdmin]);

  useEffect(() => {
    localStorage.setItem(SETTINGS_SECTION_CACHE_KEY, activeSection);
  }, [activeSection]);

  const defaults = useMemo<SystemSettingsDto>(
    () => ({
      general: {
        systemName: 'LOOKING GLASS',
        showPopCode: true,
        footerText: DEFAULT_FOOTER_TEXT,
        homeIntroText: DEFAULT_HOME_INTRO_TEXT,
      },
      deviceDefaults: {
        authType: 'SSH',
        username: 'admin',
        password: '',
        sshPort: 22,
        telnetPort: 23,
        timeoutSec: 15,
        maxConcurrency: 10,
      },
      rateLimit: {
        perIpPerMinute: 20,
      },
      security: {
        captchaEnabled: false,
        logRetainDays: 30,
      },
      ldap: {
        enabled: false,
        serverUrl: 'ldap://127.0.0.1:389',
        useTls: false,
        baseDn: '',
        bindDn: '',
        bindPassword: '',
        userSearchBase: '',
        userSearchFilter: '(uid={0})',
        connectTimeoutMs: 5000,
        allowLocalFallback: true,
      },
    }),
    [],
  );

  const load = () => {
    setLoading(true);
    systemSettings
      .get()
      .then((d) => {
        const currentSystemName = d.general?.systemName || 'LOOKING GLASS';
        const currentShowPopCode = typeof d.general?.showPopCode === 'boolean' ? d.general.showPopCode : true;
        const currentFooterText = d.general?.footerText?.trim() || DEFAULT_FOOTER_TEXT;
        const currentHomeIntroText = d.general?.homeIntroText?.trim() || DEFAULT_HOME_INTRO_TEXT;
        const ldap = {
          ...defaults.ldap,
          ...d.ldap,
          bindPassword: '',
        };
        const normalized: SystemSettingsDto = {
          ...d,
          general: {
            systemName: currentSystemName,
            showPopCode: currentShowPopCode,
            footerText: currentFooterText,
            homeIntroText: currentHomeIntroText,
          },
          deviceDefaults: {
            ...d.deviceDefaults,
            authType: d.deviceDefaults?.authType?.toUpperCase() === 'TELNET' ? 'TELNET' : 'SSH',
            sshPort: d.deviceDefaults?.sshPort || 22,
            telnetPort: d.deviceDefaults?.telnetPort || 23,
          },
          ldap,
        };
        setData(normalized);
        form.setFieldsValue(normalized);
      })
      .catch((e) => {
        message.error(e.message || '加载失败');
        setData(defaults);
        form.setFieldsValue(defaults);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const onSave = async () => {
    const values = await form.validateFields();
    const loadedGeneral = data?.general || defaults.general;
    const loadedDeviceDefaults = data?.deviceDefaults || defaults.deviceDefaults;
    const loadedRateLimit = data?.rateLimit || defaults.rateLimit;
    const loadedSecurity = data?.security || defaults.security;
    const loadedLdap = data?.ldap || defaults.ldap;
    const ldapIn = values.ldap;
    const normalizedLdap: SystemSettingsDto['ldap'] = {
      ...loadedLdap,
      ...ldapIn,
      serverUrl: ldapIn?.serverUrl?.trim() || loadedLdap.serverUrl || defaults.ldap.serverUrl,
      baseDn: ldapIn?.baseDn?.trim() || loadedLdap.baseDn || '',
      bindDn: ldapIn?.bindDn?.trim() || loadedLdap.bindDn || '',
      bindPassword: ldapIn?.bindPassword?.trim() || '',
      userSearchBase: ldapIn?.userSearchBase?.trim() || loadedLdap.userSearchBase || '',
      userSearchFilter: ldapIn?.userSearchFilter?.trim() || loadedLdap.userSearchFilter || defaults.ldap.userSearchFilter,
      connectTimeoutMs: ldapIn?.connectTimeoutMs || loadedLdap.connectTimeoutMs || defaults.ldap.connectTimeoutMs,
      enabled: ldapIn?.enabled ?? loadedLdap.enabled ?? false,
      useTls: ldapIn?.useTls ?? loadedLdap.useTls ?? false,
      allowLocalFallback: ldapIn?.allowLocalFallback ?? loadedLdap.allowLocalFallback ?? true,
    };

    const normalizedValues: SystemSettingsDto = {
      ...values,
      general: {
        systemName: values.general?.systemName?.trim() || loadedGeneral.systemName || 'LOOKING GLASS',
        showPopCode: values.general?.showPopCode ?? loadedGeneral.showPopCode ?? true,
        footerText: values.general?.footerText?.trim() || loadedGeneral.footerText || DEFAULT_FOOTER_TEXT,
        homeIntroText: values.general?.homeIntroText?.trim() || loadedGeneral.homeIntroText || DEFAULT_HOME_INTRO_TEXT,
      },
      deviceDefaults: {
        ...loadedDeviceDefaults,
        ...values.deviceDefaults,
        authType: values.deviceDefaults?.authType?.toUpperCase() === 'TELNET'
          ? 'TELNET'
          : (loadedDeviceDefaults?.authType?.toUpperCase() === 'TELNET' ? 'TELNET' : 'SSH'),
        username: values.deviceDefaults?.username?.trim() || loadedDeviceDefaults?.username || 'admin',
        password: values.deviceDefaults?.password ?? loadedDeviceDefaults?.password ?? '',
        sshPort: values.deviceDefaults?.sshPort || loadedDeviceDefaults?.sshPort || 22,
        telnetPort: values.deviceDefaults?.telnetPort || loadedDeviceDefaults?.telnetPort || 23,
        timeoutSec: values.deviceDefaults?.timeoutSec || loadedDeviceDefaults?.timeoutSec || 15,
        maxConcurrency: values.deviceDefaults?.maxConcurrency || loadedDeviceDefaults?.maxConcurrency || 10,
      },
      rateLimit: {
        perIpPerMinute:
          values.rateLimit?.perIpPerMinute
          || loadedRateLimit?.perIpPerMinute
          || defaults.rateLimit.perIpPerMinute,
      },
      security: {
        captchaEnabled:
          values.security?.captchaEnabled
          ?? loadedSecurity?.captchaEnabled
          ?? defaults.security.captchaEnabled,
        logRetainDays:
          values.security?.logRetainDays
          || loadedSecurity?.logRetainDays
          || defaults.security.logRetainDays,
      },
      ldap: normalizedLdap,
    };
    setSaving(true);
    systemSettings
      .update(normalizedValues)
      .then((saved) => {
        message.success('已保存');
        const ldapReload = {
          ...defaults.ldap,
          ...saved.ldap,
          bindPassword: '',
        };
        const normalized: SystemSettingsDto = {
          ...saved,
          ldap: ldapReload,
          general: {
            systemName: normalizedValues.general.systemName,
            showPopCode: normalizedValues.general.showPopCode,
            footerText: normalizedValues.general.footerText,
            homeIntroText: normalizedValues.general.homeIntroText,
          },
        };
        setData(normalized);
        form.setFieldsValue(normalized);
        localStorage.setItem('lg_system_name', normalizedValues.general.systemName);
        localStorage.setItem('lg_show_pop_code', normalizedValues.general.showPopCode ? 'true' : 'false');
        localStorage.setItem('lg_footer_text', normalizedValues.general.footerText);
        localStorage.setItem('lg_home_intro_text', normalizedValues.general.homeIntroText);
        window.dispatchEvent(new CustomEvent('lg-system-name-changed', { detail: normalizedValues.general.systemName }));
        window.dispatchEvent(
          new CustomEvent('lg-show-pop-code-changed', { detail: { showPopCode: normalizedValues.general.showPopCode } }),
        );
        window.dispatchEvent(
          new CustomEvent('lg-footer-text-changed', { detail: { footerText: normalizedValues.general.footerText } }),
        );
        window.dispatchEvent(
          new CustomEvent('lg-home-intro-text-changed', { detail: { homeIntroText: normalizedValues.general.homeIntroText } }),
        );
      })
      .catch((e) => message.error(e.message || '保存失败'))
      .finally(() => setSaving(false));
  };

  const handleLdapTestConnection = () => {
    setLdapConnLoading(true);
    admin.ldap
      .testConnection()
      .then((r) => {
        if (r.success) message.success(r.message || '连接成功');
        else message.error(r.error || '连接失败');
      })
      .catch((e) => message.error(e.message || '测试失败'))
      .finally(() => setLdapConnLoading(false));
  };

  const handleLdapTestUser = () => {
    ldapTestForm
      .validateFields()
      .then((v) => {
        setLdapTestLoading(true);
        return admin.ldap
          .testUser({
            testUsername: v.testUsername.trim(),
            testPassword: v.testPassword,
          })
          .then((r) => {
            if (r.success) {
              message.success(r.message || '用户认证成功');
              setLdapTestOpen(false);
              ldapTestForm.resetFields();
            } else {
              message.error(r.error || '认证失败');
            }
          })
          .catch((e) => message.error(e.message || '测试失败'))
          .finally(() => setLdapTestLoading(false));
      });
  };

  const triggerLogoUpload = () => logoInputRef.current?.click();
  const triggerSslUpload = () => {
    setSslFullchainFile(null);
    setSslPrivkeyFile(null);
    setSslModalOpen(true);
  };

  const onLogoFilePicked = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.currentTarget.value = '';
    if (!file) return;
    setLogoUploadLoading(true);
    try {
      const r = await admin.systemAssets.uploadLogo(file);
      bumpLogoVersion();
      message.success(r.message || 'logo 已更新');
    } catch (err: any) {
      message.error(err?.message || '上传 logo 失败');
    } finally {
      setLogoUploadLoading(false);
    }
  };

  const resetLogo = async () => {
    setLogoUploadLoading(true);
    try {
      const r = await admin.systemAssets.resetLogo();
      bumpLogoVersion();
      message.success(r.message || '已恢复默认 logo');
    } catch (err: any) {
      message.error(err?.message || '重置 logo 失败');
    } finally {
      setLogoUploadLoading(false);
    }
  };

  const pickFullchainFile = () => sslFullchainInputRef.current?.click();
  const pickPrivkeyFile = () => sslPrivkeyInputRef.current?.click();

  const onFullchainFilePicked = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0] || null;
    e.currentTarget.value = '';
    setSslFullchainFile(f);
  };

  const onPrivkeyFilePicked = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0] || null;
    e.currentTarget.value = '';
    setSslPrivkeyFile(f);
  };

  const submitNginxSslUpload = async () => {
    if (!sslFullchainFile || !sslPrivkeyFile) {
      message.warning('请同时选择 fullchain.pem 与 privkey.pem');
      return;
    }
    setSslUploadLoading(true);
    try {
      const r = await admin.systemAssets.uploadNginxSsl(sslFullchainFile, sslPrivkeyFile);
      message.success(r.message || '证书已替换，请重载 Nginx');
      setSslModalOpen(false);
    } catch (err: any) {
      message.error(err?.message || '上传证书失败');
    } finally {
      setSslUploadLoading(false);
    }
  };

  const resetSelfSignedNginxCert = async () => {
    setSslUploadLoading(true);
    try {
      const r = await admin.systemAssets.resetNginxSelfSigned();
      message.success(r.message || '已重置为自签名证书，请重载 Nginx');
    } catch (err: any) {
      message.error(err?.message || '重置自签名证书失败');
    } finally {
      setSslUploadLoading(false);
    }
  };

  const confirmResetSelfSignedNginxCert = () => {
    Modal.confirm({
      title: '确认重置为自签名 SSL 证书？',
      content: '将覆盖当前 Nginx 证书文件。重置后请重载 Nginx 才会生效。',
      okText: '确认重置',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: resetSelfSignedNginxCert,
    });
  };

  const menuButton = (id: SettingsSection, label: string) => (
    <button
      type="button"
      onClick={() => setActiveSection(id)}
      style={{
        textAlign: 'left',
        padding: '11px 14px',
        borderRadius: 10,
        border: 'none',
        fontSize: 15,
        lineHeight: 1.45,
        fontWeight: activeSection === id ? 600 : 500,
        background: activeSection === id ? 'rgba(37,99,235,0.08)' : 'transparent',
        color: activeSection === id ? '#1d4ed8' : '#111827',
        cursor: 'pointer',
      }}
    >
      {label}
    </button>
  );

  if (loading || !data) {
    return (
      <div style={{ padding: 24 }}>
        <Spin />
      </div>
    );
  }

  return (
    <div>
      <Title level={5} style={{ marginBottom: 4 }}>
        系统设置
      </Title>
      <Text type="secondary">
        配置全局参数与默认值，新建设备和前台展示都会自动应用（也可以在各模块单独覆盖）。
      </Text>
      <div style={{ height: 20 }} />

      <div
        style={{
          display: 'flex',
          alignItems: 'flex-start',
          gap: 24,
        }}
      >
        <div
          style={{
            flex: '0 0 228px',
            borderRight: '1px solid #e5e7eb',
            paddingRight: 16,
          }}
        >
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: 8,
            }}
          >
            {menuButton('general', '通用设置')}
            {menuButton('device', '设备配置')}
            {menuButton('security', '安全设置')}
            {isAdmin && menuButton('users', '用户管理')}
            {isAdmin && menuButton('auth', '认证设置')}
          </div>
        </div>

        <div style={{ flex: '1 1 0' }}>
          {activeSection === 'users' && isAdmin ? (
            <Card title="用户管理" style={{ marginBottom: 16 }}>
              <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
                仅管理员可操作：新增、编辑、删除后台用户及角色分配。
              </Text>
              <SettingsUsersPanel />
            </Card>
          ) : (
            <Form form={form} layout="vertical" initialValues={defaults} disabled={!canEditSettings}>
              {activeSection === 'general' && (
                <Card title="通用设置" style={{ marginBottom: 16 }}>
                  <Form.Item
                    name={['general', 'systemName']}
                    label="系统名称"
                    rules={[{ required: true, message: '请输入系统名称' }]}
                  >
                    <Input placeholder="LOOKING GLASS" maxLength={64} />
                  </Form.Item>
                  <Form.Item name={['general', 'showPopCode']} label="前端显示POP编码" valuePropName="checked">
                    <Switch checkedChildren="显示" unCheckedChildren="隐藏" />
                  </Form.Item>
                  <Form.Item name={['general', 'footerText']} label="页脚信息">
                    <Input.TextArea
                      rows={2}
                      placeholder="例如：© 2026 ISP Looking Glass. All rights reserved."
                      maxLength={240}
                    />
                  </Form.Item>
                  <Form.Item name={['general', 'homeIntroText']} label="首页文案">
                    <Input.TextArea
                      rows={14}
                      placeholder="这里填写首页展示文案，支持多行内容。"
                      maxLength={6000}
                      showCount
                    />
                  </Form.Item>
                  {isAdmin && (
                    <>
                      <Divider orientation="horizontal" titlePlacement="start" plain>
                        系统资源
                      </Divider>
                      <Space wrap>
                        <Button onClick={triggerLogoUpload} loading={logoUploadLoading}>
                          上传 Logo
                        </Button>
                        <Button onClick={resetLogo} loading={logoUploadLoading}>
                          重置默认 Logo
                        </Button>
                        <Button onClick={triggerSslUpload} loading={sslUploadLoading}>
                          上传替换 SSL 证书
                        </Button>
                        <Button onClick={confirmResetSelfSignedNginxCert} loading={sslUploadLoading}>
                          重置自签名 SSL 证书
                        </Button>
                      </Space>
                      <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
                        Nginx 证书目录：`nginx/certs`。替换/重置后请执行 `docker compose restart frontend` 生效。
                      </Text>
                      <input
                        ref={logoInputRef}
                        type="file"
                        accept="image/*"
                        style={{ display: 'none' }}
                        onChange={onLogoFilePicked}
                      />
                      <input
                        ref={sslFullchainInputRef}
                        type="file"
                        accept=".pem"
                        style={{ display: 'none' }}
                        onChange={onFullchainFilePicked}
                      />
                      <input
                        ref={sslPrivkeyInputRef}
                        type="file"
                        accept=".pem,.key"
                        style={{ display: 'none' }}
                        onChange={onPrivkeyFilePicked}
                      />
                    </>
                  )}
                </Card>
              )}

              {activeSection === 'device' && (
                <Card title="设备配置" style={{ marginBottom: 16 }}>
                  <Form.Item name={['deviceDefaults', 'authType']} label="登录方式" rules={[{ required: true }]}>
                    <Select
                      options={[
                        { value: 'SSH', label: 'SSH' },
                        { value: 'TELNET', label: 'TELNET' },
                      ]}
                    />
                  </Form.Item>
                  <Form.Item name={['deviceDefaults', 'username']} label="全局用户名" rules={[{ required: true }]}>
                    <Input />
                  </Form.Item>
                  <Form.Item name={['deviceDefaults', 'password']} label="全局密码">
                    <Input
                      type={devicePasswordVisible ? 'text' : 'password'}
                      placeholder="留空则新建设备需要单独填写密码"
                      suffix={(
                        <button
                          type="button"
                          onMouseDown={(e) => e.preventDefault()}
                          onClick={() => setDevicePasswordVisible((v) => !v)}
                          aria-label={devicePasswordVisible ? '隐藏密码' : '显示密码'}
                          style={{ border: 'none', background: 'transparent', cursor: 'pointer', padding: 0 }}
                        >
                          {devicePasswordVisible ? '🙈' : '👁'}
                        </button>
                      )}
                    />
                  </Form.Item>
                  <Form.Item name={['deviceDefaults', 'sshPort']} label="默认 SSH 端口" rules={[{ required: true }]}>
                    <InputNumber min={1} max={65535} style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name={['deviceDefaults', 'telnetPort']} label="默认 Telnet 端口" rules={[{ required: true }]}>
                    <InputNumber min={1} max={65535} style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name={['deviceDefaults', 'timeoutSec']} label="默认超时(秒)" rules={[{ required: true }]}>
                    <InputNumber min={3} max={120} style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name={['deviceDefaults', 'maxConcurrency']} label="默认并发上限" rules={[{ required: true }]}>
                    <InputNumber min={1} max={200} style={{ width: '100%' }} />
                  </Form.Item>
                </Card>
              )}

              {activeSection === 'security' && (
                <Card title="安全设置" style={{ marginBottom: 16 }}>
                  <Form.Item name={['rateLimit', 'perIpPerMinute']} label="每 IP 每分钟请求数" rules={[{ required: true }]}>
                    <InputNumber min={1} max={1000} style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name={['security', 'captchaEnabled']} label="Captcha 开关" valuePropName="checked">
                    <Switch />
                  </Form.Item>
                  <Form.Item name={['security', 'logRetainDays']} label="日志保留天数" rules={[{ required: true }]}>
                    <InputNumber min={1} max={3650} style={{ width: '100%' }} />
                  </Form.Item>
                </Card>
              )}

              {activeSection === 'auth' && isAdmin && (
                <Card title="LDAP 认证" style={{ marginBottom: 16 }}>
                  <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
                    启用后，登录将优先使用目录认证；用户仍需在本系统「用户管理」中存在且状态为启用，才能进入后台。
                    绑定密码不会在加载时回显；留空保存表示不修改已保存的密码。
                  </Text>
                  <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
                    「测试连接 / 测试用户」使用<strong>当前已保存</strong>的 LDAP 配置；修改后请先点击「保存」。
                  </Text>
                  <Form.Item name={['ldap', 'enabled']} label="启用 LDAP" valuePropName="checked">
                    <Switch checkedChildren="开" unCheckedChildren="关" />
                  </Form.Item>
                  <Form.Item
                    name={['ldap', 'serverUrl']}
                    label="服务器 URL"
                    rules={[{ required: true, message: '例如 ldap://host:389 或 ldaps://host:636' }]}
                  >
                    <Input placeholder="ldap://127.0.0.1:389" />
                  </Form.Item>
                  <Form.Item name={['ldap', 'useTls']} label="LDAP + StartTLS（ldap:// 时使用）" valuePropName="checked">
                    <Switch />
                  </Form.Item>
                  <Form.Item name={['ldap', 'baseDn']} label="Base DN（可选，部分环境需要）">
                    <Input placeholder="dc=example,dc=com" />
                  </Form.Item>
                  <Form.Item name={['ldap', 'bindDn']} label="绑定 DN（目录服务账号）">
                    <Input placeholder="cn=admin,dc=example,dc=com" />
                  </Form.Item>
                  <Form.Item name={['ldap', 'bindPassword']} label="绑定密码">
                    <Input
                      type={ldapBindPasswordVisible ? 'text' : 'password'}
                      placeholder="留空保存则不修改已保存的密码"
                      autoComplete="new-password"
                      suffix={(
                        <button
                          type="button"
                          onMouseDown={(e) => e.preventDefault()}
                          onClick={() => setLdapBindPasswordVisible((v) => !v)}
                          aria-label={ldapBindPasswordVisible ? '隐藏密码' : '显示密码'}
                          style={{ border: 'none', background: 'transparent', cursor: 'pointer', padding: 0 }}
                        >
                          {ldapBindPasswordVisible ? '🙈' : '👁'}
                        </button>
                      )}
                    />
                  </Form.Item>
                  <Form.Item name={['ldap', 'userSearchBase']} label="用户搜索 Base">
                    <Input placeholder="ou=users,dc=example,dc=com" />
                  </Form.Item>
                  <Form.Item
                    name={['ldap', 'userSearchFilter']}
                    label="用户搜索过滤器"
                    rules={[{ required: true, message: '须包含占位符 {0}，表示登录名' }]}
                    extra="示例：(sAMAccountName={0}) 或 (uid={0})"
                  >
                    <Input placeholder="(uid={0})" />
                  </Form.Item>
                  <Form.Item name={['ldap', 'connectTimeoutMs']} label="连接超时 (毫秒)" rules={[{ required: true }]}>
                    <InputNumber min={1000} max={60000} style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item
                    name={['ldap', 'allowLocalFallback']}
                    label="LDAP 失败时允许本地账号登录"
                    valuePropName="checked"
                  >
                    <Switch />
                  </Form.Item>
                  <Space wrap style={{ marginBottom: 8 }}>
                    <Button onClick={handleLdapTestConnection} loading={ldapConnLoading}>
                      测试连接
                    </Button>
                    <Button onClick={() => setLdapTestOpen(true)}>测试用户登录</Button>
                  </Space>
                </Card>
              )}

              {activeSection !== 'users' && canEditSettings && (
                <div style={{ marginTop: 16 }}>
                  <Button type="primary" onClick={onSave} loading={saving}>
                    保存
                  </Button>
                </div>
              )}
            </Form>
          )}
        </div>
      </div>

      <Modal
        title="LDAP 用户登录测试"
        open={ldapTestOpen}
        onOk={handleLdapTestUser}
        onCancel={() => {
          setLdapTestOpen(false);
          ldapTestForm.resetFields();
        }}
        confirmLoading={ldapTestLoading}
        destroyOnClose
      >
        <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
          使用已保存的 LDAP 配置验证用户名/密码（不会创建或修改本系统用户）。
        </Text>
        <Form form={ldapTestForm} layout="vertical">
          <Form.Item name="testUsername" label="用户名" rules={[{ required: true, message: '请输入要测试的用户名' }]}>
            <Input autoComplete="off" />
          </Form.Item>
          <Form.Item name="testPassword" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input
              type={ldapTestPasswordVisible ? 'text' : 'password'}
              autoComplete="new-password"
              suffix={(
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => setLdapTestPasswordVisible((v) => !v)}
                  aria-label={ldapTestPasswordVisible ? '隐藏密码' : '显示密码'}
                  style={{ border: 'none', background: 'transparent', cursor: 'pointer', padding: 0 }}
                >
                  {ldapTestPasswordVisible ? '🙈' : '👁'}
                </button>
              )}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="上传替换 Nginx SSL 证书"
        open={sslModalOpen}
        onOk={submitNginxSslUpload}
        onCancel={() => setSslModalOpen(false)}
        confirmLoading={sslUploadLoading}
        okText="上传并替换"
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <Space>
            <Button onClick={pickFullchainFile}>选择 fullchain.pem</Button>
            <Text type="secondary">{sslFullchainFile?.name || '未选择'}</Text>
          </Space>
          <Space>
            <Button onClick={pickPrivkeyFile}>选择 privkey.pem</Button>
            <Text type="secondary">{sslPrivkeyFile?.name || '未选择'}</Text>
          </Space>
        </Space>
      </Modal>
    </div>
  );
};
