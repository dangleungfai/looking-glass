export type RoleCode = 'ADMIN' | 'OPS' | 'READONLY' | '';

export function normalizeRoleCode(raw?: string | null): RoleCode {
  const v = (raw || '').trim().toUpperCase();
  if (v === 'ROLE_ADMIN' || v === 'SUPER_ADMIN') return 'ADMIN';
  if (v === 'ROLE_OPS' || v === 'NETWORK_ADMIN') return 'OPS';
  if (v === 'ROLE_READONLY' || v === 'READONLY_ADMIN' || v === 'AUDITOR') return 'READONLY';
  if (v === 'ADMIN' || v === 'OPS' || v === 'READONLY') return v;
  return '';
}

export function resolveCurrentRole(): RoleCode {
  const fromStorage = normalizeRoleCode(localStorage.getItem('lg_role'));
  if (fromStorage) return fromStorage;
  const token = localStorage.getItem('token') || '';
  const payloadPart = token.split('.')[1];
  if (!payloadPart) return '';
  try {
    const normalized = payloadPart.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4);
    const payload = JSON.parse(window.atob(padded)) as { role?: string };
    const role = normalizeRoleCode(payload.role);
    if (role) localStorage.setItem('lg_role', role);
    return role;
  } catch {
    return '';
  }
}

export function canManageResources(role: RoleCode): boolean {
  return role === 'ADMIN' || role === 'OPS';
}
