#!/bin/bash
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG="$ROOT/logs"
for name in backend worker frontend; do
  if [ -f "$LOG/${name}.pid" ]; then
    pid=$(cat "$LOG/${name}.pid")
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null
      echo "已停止 $name (PID $pid)"
    fi
    rm -f "$LOG/${name}.pid"
  fi
done
