export function normalizeQueryType(queryType?: string): string {
  const q = (queryType || '').trim().toUpperCase().replace(/[-\s]+/g, '_');
  if (!q) return '';
  if (q === 'PING') return 'IPV4_PING';
  if (q === 'TRACEROUTE') return 'IPV4_TRACEROUTE';
  if (q === 'BGP_PREFIX' || q === 'BGP_ASN' || q === 'ROUTE_LOOKUP') return 'IPV4_BGP_ROUTE';
  if (q === 'IPV4PING') return 'IPV4_PING';
  if (q === 'IPV6PING') return 'IPV6_PING';
  if (q === 'IPV4TRACEROUTE') return 'IPV4_TRACEROUTE';
  if (q === 'IPV6TRACEROUTE') return 'IPV6_TRACEROUTE';
  if (q === 'IPV4BGPROUTE') return 'IPV4_BGP_ROUTE';
  if (q === 'IPV6BGPROUTE') return 'IPV6_BGP_ROUTE';
  return q;
}

const QUERY_TYPE_ORDER = [
  'IPV4_PING',
  'IPV6_PING',
  'IPV4_TRACEROUTE',
  'IPV6_TRACEROUTE',
  'IPV4_BGP_ROUTE',
  'IPV6_BGP_ROUTE',
];

export function normalizeQueryTypes(queryTypes: string[] = []): string[] {
  const legacy = queryTypes.map((q) => (q || '').trim().toUpperCase());
  const hasLegacyPing = legacy.includes('PING');
  const hasLegacyTraceroute = legacy.includes('TRACEROUTE');
  const hasLegacyBgp = legacy.includes('BGP_PREFIX') || legacy.includes('BGP_ASN') || legacy.includes('ROUTE_LOOKUP');

  const set = new Set<string>();
  queryTypes.forEach((q) => {
    const normalized = normalizeQueryType(q);
    if (normalized) set.add(normalized);
  });

  // 兼容旧后端仅返回 PING/TRACEROUTE/BGP_* 的场景，自动补齐 IPv6 类型。
  if (hasLegacyPing) {
    set.add('IPV4_PING');
    set.add('IPV6_PING');
  }
  if (hasLegacyTraceroute) {
    set.add('IPV4_TRACEROUTE');
    set.add('IPV6_TRACEROUTE');
  }
  if (hasLegacyBgp) {
    set.add('IPV4_BGP_ROUTE');
    set.add('IPV6_BGP_ROUTE');
  }

  return Array.from(set).sort((a, b) => {
    const ia = QUERY_TYPE_ORDER.indexOf(a);
    const ib = QUERY_TYPE_ORDER.indexOf(b);
    if (ia !== -1 && ib !== -1) return ia - ib;
    if (ia !== -1) return -1;
    if (ib !== -1) return 1;
    return a.localeCompare(b);
  });
}

export function formatQueryType(queryType?: string): string {
  const q = normalizeQueryType(queryType);
  if (!q) return '-';

  const labels: Record<string, string> = {
    IPV4_PING: 'IPv4 Ping',
    IPV6_PING: 'IPv6 Ping',
    IPV4_TRACEROUTE: 'IPv4 Traceroute',
    IPV6_TRACEROUTE: 'IPv6 Traceroute',
    IPV4_BGP_ROUTE: 'IPv4 BGP Route',
    IPV6_BGP_ROUTE: 'IPv6 BGP Route',
  };
  return labels[q] || q.replace(/_/g, ' ');
}
