const API_BASE = '/api';

function getToken(): string | null {
  return localStorage.getItem('token');
}

export async function login(username: string, password: string) {
  const res = await fetch(`${API_BASE}/admin/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Login failed');
  return data;
}

export async function getPops() {
  const res = await fetch(`${API_BASE}/public/pops`);
  if (!res.ok) throw new Error('Failed to fetch POPs');
  return res.json();
}

export async function getQueryTypes() {
  const res = await fetch(`${API_BASE}/public/query-types`);
  if (!res.ok) throw new Error('Failed to fetch query types');
  return res.json();
}

export async function getSystemName() {
  const res = await fetch(`${API_BASE}/public/system-name`);
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error('Failed to fetch system name');
  return (data as { systemName?: string }).systemName || 'LOOKING GLASS';
}

export async function getPublicUiConfig() {
  const res = await fetch(`${API_BASE}/public/system-name`);
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error('Failed to fetch public ui config');
  const parsed = data as {
    systemName?: string;
    showPopCode?: boolean;
    footerText?: string;
    homeIntroText?: string;
    logoUrl?: string;
    hasCustomLogo?: boolean;
    clientIp?: string;
    captchaEnabled?: boolean;
  };
  return {
    systemName: parsed.systemName || 'LOOKING GLASS',
    showPopCode: parsed.showPopCode !== false,
    footerText: parsed.footerText || '',
    homeIntroText: parsed.homeIntroText || '',
    logoUrl: parsed.logoUrl || '/api/public/logo',
    hasCustomLogo: parsed.hasCustomLogo === true,
    clientIp: parsed.clientIp || '',
    captchaEnabled: parsed.captchaEnabled === true,
  };
}

export async function getPublicCaptcha() {
  const res = await fetch(`${API_BASE}/public/captcha`);
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error('Failed to fetch captcha');
  const parsed = data as { enabled?: boolean; captchaId?: string; question?: string };
  return {
    enabled: parsed.enabled === true,
    captchaId: parsed.captchaId || '',
    question: parsed.question || '',
  };
}

export async function submitQuery(body: {
  popCode: string;
  queryType: string;
  target: string;
  params?: Record<string, unknown>;
  captchaToken?: string;
}) {
  const res = await fetch(`${API_BASE}/public/query`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Query failed');
  return data;
}

function authFetch(url: string, options: RequestInit = {}) {
  const token = getToken();
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
  if (options.body && typeof options.body === 'string' && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  return fetch(url, { ...options, headers });
}

async function authJson<T>(url: string, options: RequestInit = {}): Promise<T> {
  const res = await authFetch(url, options);
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    if (res.status === 401 || res.status === 403) {
      // token 失效或无权限，清空并跳转登录页
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      localStorage.removeItem('lg_role');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    throw new Error((data as { error?: string }).error || `HTTP ${res.status}`);
  }
  return data as T;
}

export const admin = {
  pops: {
    list: () => authJson<any[]>(`${API_BASE}/admin/pops`),
    create: (body: Record<string, unknown>) =>
      authFetch(`${API_BASE}/admin/pops`, { method: 'POST', body: JSON.stringify(body) }).then((r) => r.json()),
    update: (id: number, body: Record<string, unknown>) =>
      authFetch(`${API_BASE}/admin/pops/${id}`, { method: 'PUT', body: JSON.stringify(body) }).then((r) => r.json()),
    delete: (id: number) =>
      authFetch(`${API_BASE}/admin/pops/${id}`, { method: 'DELETE' }).then((r) => r.json()),
  },
  devices: {
    list: () => authJson<any[]>(`${API_BASE}/admin/devices`),
    create: (body: Record<string, unknown>) =>
      authFetch(`${API_BASE}/admin/devices`, { method: 'POST', body: JSON.stringify(body) }).then((r) => r.json()),
    update: (id: number, body: Record<string, unknown>) =>
      authFetch(`${API_BASE}/admin/devices/${id}`, { method: 'PUT', body: JSON.stringify(body) }).then((r) => r.json()),
    updateStatus: (id: number, status: number) =>
      authFetch(`${API_BASE}/admin/devices/${id}/status`, { method: 'POST', body: JSON.stringify({ status }) }).then((r) => r.json()),
    loginTest: (id: number, body: { command: string; username?: string; password?: string }) =>
      authJson<{
        requestId: string;
        deviceId: number;
        deviceName: string;
        mgmtIp: string;
        command: string;
        status: string;
        durationMs?: number;
        output?: string;
        errorMessage?: string;
      }>(`${API_BASE}/admin/devices/${id}/login-test`, {
        method: 'POST',
        body: JSON.stringify(body),
      }),
    delete: (id: number) =>
      authFetch(`${API_BASE}/admin/devices/${id}`, { method: 'DELETE' }).then((r) => r.json()),
  },
  templates: {
    list: () => authJson<any[]>(`${API_BASE}/admin/command-templates`),
    create: (body: Record<string, unknown>) =>
      authFetch(`${API_BASE}/admin/command-templates`, { method: 'POST', body: JSON.stringify(body) }).then((r) => r.json()),
    update: (id: number, body: Record<string, unknown>) =>
      authFetch(`${API_BASE}/admin/command-templates/${id}`, { method: 'PUT', body: JSON.stringify(body) }).then((r) => r.json()),
    delete: (id: number) =>
      authFetch(`${API_BASE}/admin/command-templates/${id}`, { method: 'DELETE' }).then((r) => r.json()),
  },
  queryLogs: {
    list: (page = 0, size = 20) =>
      authJson<{ content: any[]; totalElements: number }>(`${API_BASE}/admin/query-logs?page=${page}&size=${size}`),
  },
  dashboard: {
    visualization: (days = 7, securityHours = 24) =>
      authJson<{
        days: number;
        trend: { bucket: string; total: number; success: number; failed: number }[];
        topTargets: { target: string; count: number }[];
        security: {
          hours: number;
          totalQueries: number;
          failedQueries: number;
          successRate: number;
          uniqueSourceIps: number;
          blacklistActive: number;
          ldapUsers: number;
          localUsers: number;
          topFailSourceIps: { sourceIp: string; count: number }[];
        };
      }>(`${API_BASE}/admin/dashboard/visualization?days=${days}&securityHours=${securityHours}`),
  },
  users: {
    list: () => authJson<
      {
        id: number;
        username: string;
        email?: string;
        mobile?: string;
        userType?: string;
        roleCode: string;
        roleName?: string;
        status: number;
        createdAt?: string;
        updatedAt?: string;
      }[]
    >(`${API_BASE}/admin/users`),
    create: (body: Record<string, unknown>) =>
      authJson<unknown>(`${API_BASE}/admin/users`, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: number, body: Record<string, unknown>) =>
      authJson<unknown>(`${API_BASE}/admin/users/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    delete: (id: number) =>
      authJson<{ ok?: boolean }>(`${API_BASE}/admin/users/${id}`, { method: 'DELETE' }),
  },
  roles: {
    list: () => authJson<{ id: number; roleCode: string; roleName: string }[]>(`${API_BASE}/admin/roles`),
  },
  ldap: {
    testConnection: () =>
      authJson<{ success: boolean; message?: string; error?: string }>(`${API_BASE}/admin/ldap/test-connection`, {
        method: 'POST',
      }),
    testUser: (body: { testUsername: string; testPassword: string }) =>
      authJson<{ success: boolean; message?: string; error?: string }>(`${API_BASE}/admin/ldap/test-user`, {
        method: 'POST',
        body: JSON.stringify(body),
      }),
  },
  systemAssets: {
    uploadLogo: async (file: File) => {
      const fd = new FormData();
      fd.append('file', file);
      return authJson<{ ok: boolean; message?: string }>(`${API_BASE}/admin/system-assets/logo`, {
        method: 'POST',
        body: fd,
      });
    },
    resetLogo: () =>
      authJson<{ ok: boolean; message?: string }>(`${API_BASE}/admin/system-assets/logo`, {
        method: 'DELETE',
      }),
    uploadNginxSsl: async (fullchain: File, privkey: File) => {
      const fd = new FormData();
      fd.append('fullchain', fullchain);
      fd.append('privkey', privkey);
      return authJson<{ ok: boolean; message?: string }>(`${API_BASE}/admin/system-assets/nginx-ssl/upload`, {
        method: 'POST',
        body: fd,
      });
    },
    resetNginxSelfSigned: () =>
      authJson<{ ok: boolean; message?: string }>(`${API_BASE}/admin/system-assets/nginx-ssl/reset-self-signed`, {
        method: 'POST',
      }),
  },
};

export interface SystemSettingsDto {
  general: {
    systemName: string;
    showPopCode: boolean;
    footerText: string;
    homeIntroText: string;
  };
  deviceDefaults: {
    authType: string;
    username: string;
    password: string;
    sshPort: number;
    telnetPort: number;
    timeoutSec: number;
    maxConcurrency: number;
  };
  rateLimit: {
    perIpPerMinute: number;
  };
  security: {
    captchaEnabled: boolean;
    logRetainDays: number;
  };
  ldap: {
    enabled: boolean;
    serverUrl: string;
    useTls: boolean;
    baseDn: string;
    bindDn: string;
    bindPassword: string;
    userSearchBase: string;
    userSearchFilter: string;
    connectTimeoutMs: number;
    allowLocalFallback: boolean;
  };
}

export const systemSettings = {
  get: () => authJson<SystemSettingsDto>(`${API_BASE}/admin/system-settings`),
  update: (body: SystemSettingsDto) =>
    authJson<SystemSettingsDto>(`${API_BASE}/admin/system-settings`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
};
