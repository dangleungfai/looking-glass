#!/bin/bash
# 在后台启动后端、Worker、前端（请先完成 MySQL 初始化）

set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$ROOT/logs"
cd "$ROOT"

echo "启动后端 (8080)..."
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
cd "$ROOT/backend"
java -jar build/libs/looking-glass-backend-0.0.1-SNAPSHOT.jar > "$ROOT/logs/backend.log" 2>&1 &
echo $! > "$ROOT/logs/backend.pid"
sleep 4

echo "启动 Worker (8000)..."
cd "$ROOT/worker"
source .venv/bin/activate
uvicorn main:app --host 0.0.0.0 --port 8000 > "$ROOT/logs/worker.log" 2>&1 &
echo $! > "$ROOT/logs/worker.pid"
sleep 2

echo "启动前端 (3000)..."
cd "$ROOT/frontend"
npm run dev > "$ROOT/logs/frontend.log" 2>&1 &
echo $! > "$ROOT/logs/frontend.pid"

echo "全部已启动。访问 http://localhost:3000  后台账号 admin / admin123"
echo "日志目录: $ROOT/logs/"
echo "停止服务: $ROOT/scripts/stop-all.sh"
