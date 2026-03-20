export const DEFAULT_HOME_INTRO_TEXT = `🌐 Looking Glass 网络诊断平台说明

本 Looking Glass 系统为用户提供对我司骨干网络的可视化诊断能力，使您能够从我司网络视角，实时查询路由信息与网络连通性，获得与我司客户一致的网络透明度。

⸻

🔍 支持的查询功能

BGP 查询（Border Gateway Protocol）
用于查看从指定 PoP（节点）到目标 IP 地址或前缀的路由信息，包括最佳路径及其他可用路径（如 AS_PATH、Next-Hop 等）。

Ping 测试
用于检测从指定 PoP 到目标 IP 地址或域名的连通性及延迟（RTT）。

Traceroute 路由追踪
用于显示数据包从指定 PoP 到目标地址所经过的路径，并展示每一跳的延迟信息。

POP-to-POP 测试（可选）
用于查看我司不同 PoP 节点之间的网络延迟表现（仅限内部网络）。

⸻

📍 使用方法
1. 选择测试节点（PoP / 城市 / 路由器）
2. 选择查询类型（BGP / Ping / Trace）
3. 输入目标 IP 地址或域名
4. 点击查询执行测试

⸻

⚠️ 注意事项
• 本系统仅提供有限的网络诊断命令（BGP / Ping / Trace），不支持自定义命令执行
• 查询结果反映的是从我司网络视角到目标的路径及性能，可能与用户本地测试结果不同
• DNS 解析结果可能因解析节点不同而存在差异，建议优先使用 IP 地址进行测试
• 为保障系统稳定性，查询频率及并发可能受到限制

⸻

📩 问题反馈
如您在使用过程中遇到问题或发现异常，请通过我们的技术支持渠道与我们联系。

⸻

🌐 Looking Glass Network Diagnostic Tool

This Looking Glass platform provides visibility into our backbone routing and network performance, allowing users to perform real-time diagnostics from our network perspective.

⸻

🔍 Available Queries

BGP (Border Gateway Protocol)
Displays routing information from the selected PoP to the specified IP address or prefix, including best path and alternative routes (AS_PATH, next-hop, etc.).

Ping
Measures latency and packet reachability between the selected PoP and the destination IP address or hostname.

Traceroute
Shows the path taken by packets across the network from the selected PoP to the destination, including per-hop latency.

POP-to-POP(Optional)
Measures latency between different PoPs within our network (internal use).

⸻

📍 How to Use
1. Select a PoP (Point of Presence) or node
2. Choose a query type (BGP / Ping / Trace)
3. Enter a destination IP address or hostname
4. Click Run to execute

⸻

⚠️ Notes
• This system allows only a limited set of diagnostic commands (BGP, Ping, Trace)
• Results reflect routing and performance from our network perspective, which may differ from your local network
• DNS resolution may vary depending on the resolver location; using an IP address is recommended
• Query rate and concurrency may be limited for system stability

⸻

📩 Support
If you encounter any issues or would like to report a problem, please contact our support team.`;
