START TRANSACTION;

-- 1) 先把现有常见类型映射到新规范（保留可迁移的）
UPDATE command_templates
SET query_type = CASE UPPER(TRIM(query_type))
    WHEN 'PING' THEN 'IPV4_PING'
    WHEN 'TRACEROUTE' THEN 'IPV4_TRACEROUTE'
    WHEN 'BGP_PREFIX' THEN 'IPV4_BGP_ROUTE'
    WHEN 'IPV4PING' THEN 'IPV4_PING'
    WHEN 'IPV4_TRACEROUTE' THEN 'IPV4_TRACEROUTE'
    WHEN 'IPV4BGPROUTE' THEN 'IPV4_BGP_ROUTE'
    ELSE UPPER(TRIM(query_type))
END;

-- 2) 只保留目标 6 类，其它全部去掉
DELETE FROM command_templates
WHERE UPPER(TRIM(query_type)) NOT IN (
    'IPV4_PING',
    'IPV6_PING',
    'IPV4_TRACEROUTE',
    'IPV6_TRACEROUTE',
    'IPV4_BGP_ROUTE',
    'IPV6_BGP_ROUTE'
);

-- 3) 补齐 IPv6 相关默认模板（按厂商/OS 维度，缺啥补啥）
INSERT INTO command_templates (vendor, os_type, query_type, template_name, command_text, parameter_schema, is_public, status, created_at, updated_at)
SELECT 'CISCO_IOS_XR', 'IOS-XR', 'IPV6_PING', 'ipv6 ping', 'ping ipv6 ${target} count ${count}', '{"target":{"type":"string"},"count":{"type":"integer","default":5}}', 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM command_templates
    WHERE vendor='CISCO_IOS_XR' AND os_type='IOS-XR' AND query_type='IPV6_PING'
);

INSERT INTO command_templates (vendor, os_type, query_type, template_name, command_text, parameter_schema, is_public, status, created_at, updated_at)
SELECT 'CISCO_IOS_XR', 'IOS-XR', 'IPV6_TRACEROUTE', 'ipv6 traceroute', 'traceroute ipv6 ${target} max-hop ${max_hop}', '{"target":{"type":"string"},"max_hop":{"type":"integer","default":30}}', 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM command_templates
    WHERE vendor='CISCO_IOS_XR' AND os_type='IOS-XR' AND query_type='IPV6_TRACEROUTE'
);

INSERT INTO command_templates (vendor, os_type, query_type, template_name, command_text, parameter_schema, is_public, status, created_at, updated_at)
SELECT 'CISCO_IOS_XR', 'IOS-XR', 'IPV6_BGP_ROUTE', 'ipv6 bgp route', 'show bgp ipv6 unicast ${prefix}', '{"prefix":{"type":"string"}}', 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM command_templates
    WHERE vendor='CISCO_IOS_XR' AND os_type='IOS-XR' AND query_type='IPV6_BGP_ROUTE'
);

INSERT INTO command_templates (vendor, os_type, query_type, template_name, command_text, parameter_schema, is_public, status, created_at, updated_at)
SELECT 'JUNIPER_JUNOS', 'JUNOS', 'IPV6_PING', 'ipv6 ping', 'ping inet6 ${target} count ${count}', '{"target":{"type":"string"},"count":{"type":"integer","default":5}}', 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM command_templates
    WHERE vendor='JUNIPER_JUNOS' AND os_type='JUNOS' AND query_type='IPV6_PING'
);

INSERT INTO command_templates (vendor, os_type, query_type, template_name, command_text, parameter_schema, is_public, status, created_at, updated_at)
SELECT 'JUNIPER_JUNOS', 'JUNOS', 'IPV6_TRACEROUTE', 'ipv6 traceroute', 'traceroute inet6 ${target}', '{"target":{"type":"string"}}', 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM command_templates
    WHERE vendor='JUNIPER_JUNOS' AND os_type='JUNOS' AND query_type='IPV6_TRACEROUTE'
);

INSERT INTO command_templates (vendor, os_type, query_type, template_name, command_text, parameter_schema, is_public, status, created_at, updated_at)
SELECT 'JUNIPER_JUNOS', 'JUNOS', 'IPV6_BGP_ROUTE', 'ipv6 bgp route', 'show route table inet6.0 protocol bgp ${prefix}', '{"prefix":{"type":"string"}}', 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM command_templates
    WHERE vendor='JUNIPER_JUNOS' AND os_type='JUNOS' AND query_type='IPV6_BGP_ROUTE'
);

INSERT INTO command_templates (vendor, os_type, query_type, template_name, command_text, parameter_schema, is_public, status, created_at, updated_at)
SELECT 'HUAWEI_VRP', 'VRP', 'IPV6_PING', 'ipv6 ping', 'ping ipv6 -c ${count} ${target}', '{"target":{"type":"string"},"count":{"type":"integer","default":5}}', 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM command_templates
    WHERE vendor='HUAWEI_VRP' AND os_type='VRP' AND query_type='IPV6_PING'
);

INSERT INTO command_templates (vendor, os_type, query_type, template_name, command_text, parameter_schema, is_public, status, created_at, updated_at)
SELECT 'HUAWEI_VRP', 'VRP', 'IPV6_TRACEROUTE', 'ipv6 traceroute', 'tracert ipv6 ${target}', '{"target":{"type":"string"}}', 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM command_templates
    WHERE vendor='HUAWEI_VRP' AND os_type='VRP' AND query_type='IPV6_TRACEROUTE'
);

INSERT INTO command_templates (vendor, os_type, query_type, template_name, command_text, parameter_schema, is_public, status, created_at, updated_at)
SELECT 'HUAWEI_VRP', 'VRP', 'IPV6_BGP_ROUTE', 'ipv6 bgp route', 'display bgp ipv6 routing-table ${prefix}', '{"prefix":{"type":"string"}}', 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM command_templates
    WHERE vendor='HUAWEI_VRP' AND os_type='VRP' AND query_type='IPV6_BGP_ROUTE'
);

-- 4) 同一厂商/OS/查询类型如果重复，仅保留最新一条
DELETE t1
FROM command_templates t1
JOIN command_templates t2
  ON t1.vendor = t2.vendor
 AND t1.os_type = t2.os_type
 AND t1.query_type = t2.query_type
 AND t1.id < t2.id;

-- 5) 设备支持类型按当前启用模板自动重建（仅保留目标 6 类）
UPDATE devices d
SET supported_query_types = (
    SELECT GROUP_CONCAT(DISTINCT ct.query_type
                        ORDER BY FIELD(ct.query_type,
                            'IPV4_PING',
                            'IPV6_PING',
                            'IPV4_TRACEROUTE',
                            'IPV6_TRACEROUTE',
                            'IPV4_BGP_ROUTE',
                            'IPV6_BGP_ROUTE'
                        ) SEPARATOR ',')
    FROM command_templates ct
    WHERE ct.vendor = d.vendor
      AND ct.os_type = d.os_type
      AND ct.status = 1
      AND ct.is_public = 1
      AND ct.query_type IN (
          'IPV4_PING',
          'IPV6_PING',
          'IPV4_TRACEROUTE',
          'IPV6_TRACEROUTE',
          'IPV4_BGP_ROUTE',
          'IPV6_BGP_ROUTE'
      )
);

-- 6) 历史日志查询类型同步迁移（便于统计口径统一）
UPDATE query_logs
SET query_type = CASE UPPER(TRIM(query_type))
    WHEN 'PING' THEN 'IPV4_PING'
    WHEN 'TRACEROUTE' THEN 'IPV4_TRACEROUTE'
    WHEN 'BGP_PREFIX' THEN 'IPV4_BGP_ROUTE'
    WHEN 'BGP_ASN' THEN 'IPV4_BGP_ROUTE'
    WHEN 'ROUTE_LOOKUP' THEN 'IPV4_BGP_ROUTE'
    WHEN 'IPV4PING' THEN 'IPV4_PING'
    WHEN 'IPV4TRACEROUTE' THEN 'IPV4_TRACEROUTE'
    WHEN 'IPV4BGPROUTE' THEN 'IPV4_BGP_ROUTE'
    WHEN 'IPV6PING' THEN 'IPV6_PING'
    WHEN 'IPV6TRACEROUTE' THEN 'IPV6_TRACEROUTE'
    WHEN 'IPV6BGPROUTE' THEN 'IPV6_BGP_ROUTE'
    ELSE UPPER(TRIM(query_type))
END;

COMMIT;
