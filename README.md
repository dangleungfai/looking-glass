# ISP Looking Glass 系统

面向 ISP / IDC / 云网络运营场景的受控 Looking Glass 平台：公网提供 Ping、Traceroute、BGP 等查询，后台管理 POP、设备、命令模板、审计与系统参数。

## 技术栈

- **前端**: React + TypeScript + Ant Design，Vite 构建
- **后端**: Spring Boot 3 (Java 21)，REST API，JWT 鉴权
- **执行 Worker**: Python 3.11 + FastAPI，Paramiko SSH 执行设备命令
- **数据库**: MySQL 8 / MariaDB 10.6+
- **监控**: Actuator + Prometheus 指标

## 目录结构

```
looking-glass/
├── backend/          # Spring Boot API
├── frontend/         # React 前台与后台
├── worker/            # Python 执行器
├── db/                # 数据库脚本在 backend/db/
├── docker-compose.yml
└── README.md
```

## 快速开始（本地）

### 1. 数据库

创建库并执行脚本（在 `backend/db/` 下）：

```bash
mysql -u root -p -e "CREATE DATABASE looking_glass CHARACTER SET utf8mb4;"
mysql -u root -p looking_glass < backend/db/schema.sql
mysql -u root -p looking_glass < backend/db/seed.sql
```

默认账号（由应用首次启动自动创建）：**admin / admin123**。

### 2. 后端

```bash
cd backend
# 需已安装 JDK 21 与 Gradle，或使用 IDE 运行
gradle bootJar   # 或从 IDE 运行 LookingGlassBackendApplication
# 若已生成 jar:
java -jar build/libs/looking-glass-backend-0.0.1-SNAPSHOT.jar
```

配置 `application.yml` 中数据源与 `looking-glass.worker.url`（默认 `http://localhost:8000`）。

### 3. Worker

```bash
cd worker
python3 -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

### 4. 前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问：公网首页 `http://localhost:3000`，后台 `http://localhost:3000/admin`（需先登录）。

## Docker Compose

1. 构建前端静态资源与后端 jar（或使用 Dockerfile 内 Gradle 构建）：

```bash
cd frontend && npm ci && npm run build && cd ..
cd backend && gradle bootJar -x test && cd ..
```

2. 启动：

```bash
docker compose up -d
```

- 公网/后台: `http://localhost:3000`（Nginx 反代后端 API）
- 后端 API: `http://localhost:8080`
- Worker: `http://localhost:8000`
- MySQL: `localhost:3306`，库 `looking_glass`，用户 `lg_user` / `lg_password`

## 主要 API

- 公网  
  - `GET /api/public/pops` 可用 POP 列表  
  - `GET /api/public/query-types` 查询类型  
  - `POST /api/public/query` 发起查询（body: popCode, queryType, target, params?）
- 后台（需 Header `Authorization: Bearer <token>`）  
  - `POST /api/admin/auth/login` 登录  
  - `GET/POST/PUT/DELETE /api/admin/pops` POP 管理  
  - `GET/POST/PUT/DELETE /api/admin/devices` 设备管理  
  - `GET/POST/PUT/DELETE /api/admin/command-templates` 命令模板  
  - `GET /api/admin/query-logs` 查询日志  
  - `GET /api/admin/settings`、`PUT /api/admin/settings/{key}` 系统设置  
- 监控: `GET /actuator/prometheus` Prometheus 指标

## 安全与限流

- 公网查询：按 IP 每分钟请求数限流（可在系统设置中配置 `rate_limit_per_minute`）
- 黑名单：在 `ip_blacklist` 表配置 IP/CIDR，生效后该来源禁止访问公网查询
- 命令通过模板与参数校验生成，禁止前端传入任意命令片段
- 设备密码仅用于 Worker 执行，接口不返回

## 扩展

- **新增厂商/模板**: 在后台「命令模板」中按厂商、OS、查询类型添加模板，占位符支持 `${target}`, `${count}`, `${max_hop}`, `${prefix}`, `${asn}` 等
- **多 POP 对比 / BGP-LS / 拓扑**: 当前架构已预留扩展点，可增加新查询类型与解析器
- **对外 API**: 可基于 `/api/public/info` 与现有公网接口扩展 API Key 鉴权与限流

## 验收要点（参考文档 22 章）

- 前台可执行 Ping、Traceroute、BGP Prefix（及 ASN）查询并展示结果
- 后台可管理 POP、设备、命令模板，查询日志可查
- 无法通过输入注入任意命令；公网接口有限流；后台需登录；设备凭证非明文存储
- 新增厂商仅需新增模板与解析逻辑，无需改核心业务代码

## 许可证

按项目约定。
