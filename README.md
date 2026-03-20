# ISP Looking Glass 系统

面向 ISP / IDC / 云网络运营场景的受控 Looking Glass 平台：公网提供 Ping、Traceroute、BGP 等查询，后台管理 POP、设备、命令模板、审计与系统参数。

## 技术栈

- **前端**: React + TypeScript + Ant Design，Vite 构建
- **后端**: Spring Boot 3 (Java 21)，REST API，JWT 鉴权
- **执行 Worker**: Python 3.11 + FastAPI，Paramiko SSH 执行设备命令
- **数据库**: MySQL 8 / MariaDB 10.6+
- **监控**: Actuator + Prometheus 指标

## 运行环境要求

建议在以下系统运行：

- macOS 13+ / 14+ / 15+
- Ubuntu 20.04+ / Debian 11+
- CentOS Stream 8+ / RHEL 8+
- Windows 11（建议使用 WSL2）

基础依赖建议版本：

- Git 2.30+
- Docker Engine / Docker Desktop 24+
- Docker Compose v2+
- OpenSSL 1.1.1+（或 3.x）
- Node.js 20+（仅本地开发前端需要）
- Python 3.11+（仅本地开发 Worker 需要）
- Java 17+（仅本地开发后端需要）

## 安装 Git

### macOS

```bash
# 方式 1: 使用 Homebrew
brew install git

# 方式 2: 安装 Xcode Command Line Tools（会附带 git）
xcode-select --install
```

### Ubuntu / Debian

```bash
sudo apt-get update
sudo apt-get install -y git
```

### CentOS / RHEL / Rocky / AlmaLinux

```bash
sudo dnf install -y git
# 老系统可用: sudo yum install -y git
```

### Windows

- 安装 [Git for Windows](https://git-scm.com/download/win)
- 或在 WSL2 中按 Ubuntu 方式安装

安装后可检查版本：

```bash
git --version
```

## 克隆代码（Git）

### SSH 方式（推荐）

```bash
git clone git@github.com:dangleungfai/looking-glass.git
cd looking-glass
```

#### 首次使用 SSH 克隆（GitHub SSH Key 配置）

1. 生成 SSH Key（建议 ed25519）：

```bash
ssh-keygen -t ed25519 -C "your_email@example.com"
```

2. 启动 ssh-agent 并添加私钥：

```bash
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519
```

3. 复制公钥内容并添加到 GitHub：

```bash
cat ~/.ssh/id_ed25519.pub
```

将输出内容复制到 GitHub：  
`Settings -> SSH and GPG keys -> New SSH key`

4. 测试 SSH 连通性：

```bash
ssh -T git@github.com
```

若看到类似 `Hi <username>! You've successfully authenticated...` 即配置成功。

5. 使用 SSH 地址克隆：

```bash
git clone git@github.com:dangleungfai/looking-glass.git
cd looking-glass
```

### HTTPS 方式

```bash
git clone https://github.com/dangleungfai/looking-glass.git
cd looking-glass
```

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

## 一键部署（推荐）

> 一条命令完成：依赖检查/安装（尽力而为）+ 自签名证书生成 + 前端构建 + Docker Compose 启动。

```bash
chmod +x scripts/local-deploy.sh
./scripts/local-deploy.sh
```

部署完成后：

- 访问 `https://localhost`
- 访问 `http://localhost` 会自动 `301` 跳转到 HTTPS
- 默认使用自签名证书（脚本首次自动生成）
- 证书路径：`nginx/certs/fullchain.pem` 与 `nginx/certs/privkey.pem`
- 默认账号：`admin / admin123`

### 自签名证书策略

- 首次部署若未检测到证书，脚本会自动生成证书
- 证书有效期：`36500` 天（约 100 年，当前常见工具链支持下的“最长年限”）
- 如需替换正式证书，直接覆盖：

```bash
cp /path/to/fullchain.pem nginx/certs/fullchain.pem
cp /path/to/privkey.pem nginx/certs/privkey.pem
docker compose restart frontend
```

## 快速开始（本地开发）

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

## Docker Compose（手动方式）

若你不使用一键脚本，也可手动执行：

1. 准备证书（可用正式证书，或自签名）：

```bash
mkdir -p nginx/certs
openssl req -x509 -newkey rsa:4096 -sha256 -nodes -days 36500 \
  -keyout nginx/certs/privkey.pem \
  -out nginx/certs/fullchain.pem \
  -subj "/C=CN/ST=Default/L=Default/O=LOOKING GLASS/OU=DevOps/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
```

2. 构建前端静态资源：

```bash
cd frontend && npm ci && npm run build && cd ..
```

3. 启动：

```bash
docker compose up -d --build
```

- 公网/后台：`https://localhost`（80 自动跳转到 443）
- 后端 API：`http://backend:8080`（仅容器网络内可见）
- Worker：`http://localhost:8000`
- MySQL：`localhost:3306`，库 `looking_glass`，用户 `lg_user` / `lg_password`

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
  - `GET/PUT /api/admin/system-settings` 系统设置  
- 监控: `GET /actuator/prometheus` Prometheus 指标

## 安全与限流

- 公网查询：按 IP 每分钟请求数限流（可在系统设置中配置 `system_rate_limit`）
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
