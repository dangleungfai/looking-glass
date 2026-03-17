#!/bin/bash
# ISP Looking Glass 本地部署脚本
# 使用前请先执行「步骤 1」中的 MySQL 命令（需输入你的 MySQL root 密码）

set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "=== 步骤 1: MySQL（需手动执行一次）==="
echo "若尚未创建库与表，请在本机执行（将 YOUR_MYSQL_PASSWORD 换成实际密码）："
echo ""
echo "  mysql -u root -p -e \"CREATE DATABASE IF NOT EXISTS looking_glass CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\""
echo "  mysql -u root -p looking_glass < $ROOT/backend/db/schema.sql"
echo "  mysql -u root -p looking_glass < $ROOT/backend/db/seed.sql"
echo ""
read -p "已完成上述 MySQL 步骤？(y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  echo "请先完成 MySQL 初始化后重新运行本脚本。"
  exit 1
fi

echo "=== 步骤 2: 后端 (Spring Boot) ==="
cd "$ROOT/backend"
./gradlew bootJar -x test --no-daemon
echo "后端 jar 已构建。"

echo "=== 步骤 3: Worker (Python) ==="
cd "$ROOT/worker"
if [ ! -d .venv ]; then
  python3 -m venv .venv
fi
source .venv/bin/activate
pip install -q -r requirements.txt
echo "Worker 依赖已安装。"

echo "=== 步骤 4: 前端 ==="
cd "$ROOT/frontend"
npm ci
echo "前端依赖已安装。"

echo ""
echo "=== 启动服务 ==="
echo "请在 3 个终端中分别执行以下命令。"
echo ""
echo "终端 1 - 后端:"
echo "  cd $ROOT/backend && java -jar build/libs/looking-glass-backend-0.0.1-SNAPSHOT.jar"
echo ""
echo "终端 2 - Worker:"
echo "  cd $ROOT/worker && source .venv/bin/activate && uvicorn main:app --host 0.0.0.0 --port 8000"
echo ""
echo "终端 3 - 前端:"
echo "  cd $ROOT/frontend && npm run dev"
echo ""
echo "全部启动后访问: 公网/后台 http://localhost:3000  默认账号 admin / admin123"
echo ""

read -p "是否由本脚本在后台启动以上 3 个服务？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  mkdir -p "$ROOT/logs"
  cd "$ROOT/backend" && java -jar build/libs/looking-glass-backend-0.0.1-SNAPSHOT.jar > "$ROOT/logs/backend.log" 2>&1 &
  echo $! > "$ROOT/logs/backend.pid"
  sleep 3
  cd "$ROOT/worker" && source .venv/bin/activate && uvicorn main:app --host 0.0.0.0 --port 8000 > "$ROOT/logs/worker.log" 2>&1 &
  echo $! > "$ROOT/logs/worker.pid"
  sleep 2
  cd "$ROOT/frontend" && npm run dev > "$ROOT/logs/frontend.log" 2>&1 &
  echo $! > "$ROOT/logs/frontend.pid"
  echo "服务已在后台启动，日志见 $ROOT/logs/。访问 http://localhost:3000"
else
  echo "请按上述说明在 3 个终端中手动启动服务。"
fi
