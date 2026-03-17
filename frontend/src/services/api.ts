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

export async function submitQuery(body: {
  popCode: string;
  queryType: string;
  target: string;
  params?: Record<string, unknown>;
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
    list: () => authJson<unknown[]>(`${API_BASE}/admin/pops`),
    create: (body: Record<string, unknown>) =>
      authFetch(`${API_BASE}/admin/pops`, { method: 'POST', body: JSON.stringify(body) }).then((r) => r.json()),
    update: (id: number, body: Record<string, unknown>) =>
      authFetch(`${API_BASE}/admin/pops/${id}`, { method: 'PUT', body: JSON.stringify(body) }).then((r) => r.json()),
    delete: (id: number) =>
      authFetch(`${API_BASE}/admin/pops/${id}`, { method: 'DELETE' }).then((r) => r.json()),
  },
  devices: {
    list: () => authJson<unknown[]>(`${API_BASE}/admin/devices`),
    create: (body: Record<string, unknown>) =>
      authFetch(`${API_BASE}/admin/devices`, { method: 'POST', body: JSON.stringify(body) }).then((r) => r.json()),
    update: (id: number, body: Record<string, unknown>) =>
      authFetch(`${API_BASE}/admin/devices/${id}`, { method: 'PUT', body: JSON.stringify(body) }).then((r) => r.json()),
    delete: (id: number) =>
      authFetch(`${API_BASE}/admin/devices/${id}`, { method: 'DELETE' }).then((r) => r.json()),
  },
  templates: {
    list: () => authJson<unknown[]>(`${API_BASE}/admin/command-templates`),
    create: (body: Record<string, unknown>) =>
      authFetch(`${API_BASE}/admin/command-templates`, { method: 'POST', body: JSON.stringify(body) }).then((r) => r.json()),
    update: (id: number, body: Record<string, unknown>) =>
      authFetch(`${API_BASE}/admin/command-templates/${id}`, { method: 'PUT', body: JSON.stringify(body) }).then((r) => r.json()),
    delete: (id: number) =>
      authFetch(`${API_BASE}/admin/command-templates/${id}`, { method: 'DELETE' }).then((r) => r.json()),
  },
  queryLogs: {
    list: (page = 0, size = 20) =>
      authJson<{ content: unknown[]; totalElements: number }>(`${API_BASE}/admin/query-logs?page=${page}&size=${size}`),
  },
  settings: {
    list: () => authJson<unknown[]>(`${API_BASE}/admin/settings`),
    update: (key: string, value: string) =>
      authFetch(`${API_BASE}/admin/settings/${encodeURIComponent(key)}`, {
        method: 'PUT',
        body: JSON.stringify({ settingValue: value }),
      }).then((r) => r.json()),
  },
};
