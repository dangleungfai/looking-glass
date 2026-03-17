-- 初始化角色
INSERT INTO roles (id, role_name, role_code, description) VALUES
(1, 'Super Admin', 'SUPER_ADMIN', '超级管理员'),
(2, 'Network Admin', 'NETWORK_ADMIN', '网络管理员'),
(3, 'Auditor', 'AUDITOR', '审计员'),
(4, 'Readonly Admin', 'READONLY_ADMIN', '只读管理员')
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name);

-- 默认管理员需在应用首次启动时通过 DataLoader 创建 (admin / admin123)，此处仅确保角色存在

-- 示例 POP
INSERT INTO pops (pop_code, pop_name, country, city, is_public, status, sort_order, remark) VALUES
('HKG1', 'Hong Kong 1', 'HK', 'Hong Kong', 1, 1, 10, 'Example POP'),
('SZX1', 'Shenzhen 1', 'CN', 'Shenzhen', 1, 1, 20, 'Example POP')
ON DUPLICATE KEY UPDATE pop_name=VALUES(pop_name);

-- 示例设备 (密码明文仅用于开发，生产环境应加密)
INSERT INTO devices (device_name, device_code, vendor, os_type, mgmt_ip, ssh_port, username, password_encrypted, auth_type, pop_id, status, priority, timeout_sec, supported_query_types) VALUES
('HKG1-R1', 'HKG1.R1', 'CISCO_IOS_XR', 'IOS-XR', '127.0.0.1', 22, 'admin', UNHEX(HEX('admin')), 'PASSWORD', (SELECT id FROM pops WHERE pop_code='HKG1' LIMIT 1), 1, 10, 15, 'PING,TRACEROUTE,BGP_PREFIX')
ON DUPLICATE KEY UPDATE device_name=VALUES(device_name);

-- 命令模板 (Cisco IOS-XR)
INSERT INTO command_templates (vendor, os_type, query_type, template_name, command_text, parameter_schema, is_public, status) VALUES
('CISCO_IOS_XR', 'IOS-XR', 'PING', 'ping ipv4', 'ping ${target} count ${count}', '{"target":{"type":"string"},"count":{"type":"integer","default":5}}', 1, 1),
('CISCO_IOS_XR', 'IOS-XR', 'TRACEROUTE', 'traceroute ipv4', 'traceroute ${target} max-hop ${max_hop}', '{"target":{"type":"string"},"max_hop":{"type":"integer","default":30}}', 1, 1),
('CISCO_IOS_XR', 'IOS-XR', 'BGP_PREFIX', 'bgp prefix', 'show bgp ipv4 unicast ${prefix}', '{"prefix":{"type":"string"}}', 1, 1),
('CISCO_IOS_XR', 'IOS-XR', 'BGP_ASN', 'bgp asn', 'show bgp summary | include ${asn}', '{"asn":{"type":"string"}}', 1, 1)
ON DUPLICATE KEY UPDATE command_text=VALUES(command_text);

-- 系统参数
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('ping_default_count', '5', 'Ping 默认次数'),
('traceroute_max_hops', '30', 'Traceroute 最大跳数'),
('rate_limit_per_minute', '20', '每 IP 每分钟请求数')
ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value);
