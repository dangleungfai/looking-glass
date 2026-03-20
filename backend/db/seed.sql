-- 初始化角色
INSERT INTO roles (id, role_name, role_code, description) VALUES
(1, '管理员', 'ADMIN', '系统管理员'),
(2, '运维', 'OPS', '运维管理员'),
(3, '只读', 'READONLY', '只读用户')
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name);

-- 默认管理员需在应用首次启动时通过 DataLoader 创建 (admin / admin123)，此处仅确保角色存在

-- 示例 POP
INSERT INTO pops (pop_code, pop_name, country, city, is_public, status, sort_order, remark) VALUES
('HKG1', 'Hong Kong 1', 'HK', 'Hong Kong', 1, 1, 10, 'Example POP'),
('SZX1', 'Shenzhen 1', 'CN', 'Shenzhen', 1, 1, 20, 'Example POP')
ON DUPLICATE KEY UPDATE pop_name=VALUES(pop_name);

-- 示例设备 (密码明文仅用于开发，生产环境应加密)
INSERT INTO devices (device_name, device_code, vendor, os_type, mgmt_ip, ssh_port, username, password_encrypted, auth_type, pop_id, status, priority, timeout_sec, supported_query_types) VALUES
('HKG1-R1', 'HKG1.R1', 'CISCO_IOS_XR', 'IOS-XR', '127.0.0.1', 22, 'admin', UNHEX(HEX('admin')), 'PASSWORD', (SELECT id FROM pops WHERE pop_code='HKG1' LIMIT 1), 1, 10, 15, 'IPV4_PING,IPV6_PING,IPV4_TRACEROUTE,IPV6_TRACEROUTE,IPV4_BGP_ROUTE,IPV6_BGP_ROUTE')
ON DUPLICATE KEY UPDATE device_name=VALUES(device_name);

-- 命令模板 (Cisco IOS-XR / Juniper JunOS / Huawei VRP)
INSERT INTO command_templates (vendor, os_type, query_type, template_name, command_text, parameter_schema, is_public, status) VALUES
('CISCO_IOS_XR', 'IOS-XR', 'IPV4_PING', 'ipv4 ping', 'ping ${target} count ${count}', '{"target":{"type":"string"},"count":{"type":"integer","default":5}}', 1, 1),
('CISCO_IOS_XR', 'IOS-XR', 'IPV6_PING', 'ipv6 ping', 'ping ipv6 ${target} count ${count}', '{"target":{"type":"string"},"count":{"type":"integer","default":5}}', 1, 1),
('CISCO_IOS_XR', 'IOS-XR', 'IPV4_TRACEROUTE', 'ipv4 traceroute', 'traceroute ${target} max-hop ${max_hop}', '{"target":{"type":"string"},"max_hop":{"type":"integer","default":30}}', 1, 1),
('CISCO_IOS_XR', 'IOS-XR', 'IPV6_TRACEROUTE', 'ipv6 traceroute', 'traceroute ipv6 ${target} max-hop ${max_hop}', '{"target":{"type":"string"},"max_hop":{"type":"integer","default":30}}', 1, 1),
('CISCO_IOS_XR', 'IOS-XR', 'IPV4_BGP_ROUTE', 'ipv4 bgp route', 'show bgp ipv4 unicast ${prefix}', '{"prefix":{"type":"string"}}', 1, 1),
('CISCO_IOS_XR', 'IOS-XR', 'IPV6_BGP_ROUTE', 'ipv6 bgp route', 'show bgp ipv6 unicast ${prefix}', '{"prefix":{"type":"string"}}', 1, 1),
('JUNIPER_JUNOS', 'JUNOS', 'IPV4_PING', 'ipv4 ping', 'ping count ${count} ${target}', '{"target":{"type":"string"},"count":{"type":"integer","default":5}}', 1, 1),
('JUNIPER_JUNOS', 'JUNOS', 'IPV6_PING', 'ipv6 ping', 'ping inet6 ${target} count ${count}', '{"target":{"type":"string"},"count":{"type":"integer","default":5}}', 1, 1),
('JUNIPER_JUNOS', 'JUNOS', 'IPV4_TRACEROUTE', 'ipv4 traceroute', 'traceroute ${target}', '{"target":{"type":"string"}}', 1, 1),
('JUNIPER_JUNOS', 'JUNOS', 'IPV6_TRACEROUTE', 'ipv6 traceroute', 'traceroute inet6 ${target}', '{"target":{"type":"string"}}', 1, 1),
('JUNIPER_JUNOS', 'JUNOS', 'IPV4_BGP_ROUTE', 'ipv4 bgp route', 'show route protocol bgp ${prefix}', '{"prefix":{"type":"string"}}', 1, 1),
('JUNIPER_JUNOS', 'JUNOS', 'IPV6_BGP_ROUTE', 'ipv6 bgp route', 'show route table inet6.0 protocol bgp ${prefix}', '{"prefix":{"type":"string"}}', 1, 1),
('HUAWEI_VRP', 'VRP', 'IPV4_PING', 'ipv4 ping', 'ping -c ${count} ${target}', '{"target":{"type":"string"},"count":{"type":"integer","default":5}}', 1, 1),
('HUAWEI_VRP', 'VRP', 'IPV6_PING', 'ipv6 ping', 'ping ipv6 -c ${count} ${target}', '{"target":{"type":"string"},"count":{"type":"integer","default":5}}', 1, 1),
('HUAWEI_VRP', 'VRP', 'IPV4_TRACEROUTE', 'ipv4 traceroute', 'tracert ${target}', '{"target":{"type":"string"}}', 1, 1),
('HUAWEI_VRP', 'VRP', 'IPV6_TRACEROUTE', 'ipv6 traceroute', 'tracert ipv6 ${target}', '{"target":{"type":"string"}}', 1, 1),
('HUAWEI_VRP', 'VRP', 'IPV4_BGP_ROUTE', 'ipv4 bgp route', 'display bgp routing-table ${prefix}', '{"prefix":{"type":"string"}}', 1, 1),
('HUAWEI_VRP', 'VRP', 'IPV6_BGP_ROUTE', 'ipv6 bgp route', 'display bgp ipv6 routing-table ${prefix}', '{"prefix":{"type":"string"}}', 1, 1)
ON DUPLICATE KEY UPDATE command_text=VALUES(command_text);

-- 系统参数
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('ping_default_count', '5', 'Ping 默认次数'),
('traceroute_max_hops', '30', 'Traceroute 最大跳数'),
('system_rate_limit', '{"perIpPerMinute":20}', '公网限流配置'),
('system_general', '{"systemName":"ISP Looking Glass","showPopCode":true,"footerText":"","homeIntroText":""}', '系统通用配置')
ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value);
