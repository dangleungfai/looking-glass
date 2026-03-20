#!/bin/bash
# ISP Looking Glass 一键部署脚本（Docker + HTTPS）
# 功能：
# 1) 自动安装所需系统依赖（Docker / Docker Compose / OpenSSL，尽力而为）
# 2) 自动构建前端静态资源
# 3) 默认生成最长年限自签名证书（100 年，36500 天）
# 4) 一键启动全部服务（HTTP 自动 301 跳转 HTTPS）

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

CERT_DIR="$ROOT/nginx/certs"
CERT_FILE="$CERT_DIR/fullchain.pem"
KEY_FILE="$CERT_DIR/privkey.pem"

info() { echo -e "[INFO] $*"; }
warn() { echo -e "[WARN] $*"; }
err()  { echo -e "[ERROR] $*"; }

has_cmd() {
  command -v "$1" >/dev/null 2>&1
}

install_with_brew() {
  if ! has_cmd brew; then
    return 1
  fi
  info "检测到 Homebrew，尝试安装依赖..."
  if ! has_cmd docker; then
    brew install --cask docker || warn "Docker Desktop 自动安装失败，请手动安装后重试。"
  fi
  has_cmd openssl || brew install openssl || warn "OpenSSL 自动安装失败，请手动安装。"
  return 0
}

install_with_apt() {
  if ! has_cmd apt-get; then
    return 1
  fi
  info "检测到 apt，尝试安装依赖..."
  sudo apt-get update
  sudo apt-get install -y docker.io docker-compose-plugin openssl
  sudo systemctl enable --now docker || true
  return 0
}

install_with_dnf_or_yum() {
  if has_cmd dnf; then
    info "检测到 dnf，尝试安装依赖..."
    sudo dnf install -y docker docker-compose-plugin openssl
    sudo systemctl enable --now docker || true
    return 0
  fi
  if has_cmd yum; then
    info "检测到 yum，尝试安装依赖..."
    sudo yum install -y docker openssl
    sudo systemctl enable --now docker || true
    return 0
  fi
  return 1
}

ensure_system_deps() {
  info "检查系统依赖..."
  if has_cmd docker && docker compose version >/dev/null 2>&1 && has_cmd openssl; then
    info "系统依赖已满足。"
    return 0
  fi

  install_with_brew || install_with_apt || install_with_dnf_or_yum || warn "未识别的包管理器，请手动安装 Docker / Docker Compose / OpenSSL。"

  if ! has_cmd docker; then
    err "未检测到 docker，请先安装 Docker 后重试。"
    exit 1
  fi
  if ! docker compose version >/dev/null 2>&1; then
    err "未检测到 docker compose，请先安装后重试。"
    exit 1
  fi
  if ! has_cmd openssl; then
    err "未检测到 openssl，请先安装后重试。"
    exit 1
  fi
}

ensure_docker_running() {
  info "检查 Docker 服务状态..."
  if docker info >/dev/null 2>&1; then
    info "Docker 运行正常。"
    return 0
  fi
  warn "Docker 当前不可用。"
  warn "macOS 请先启动 Docker Desktop；Linux 请启动 docker 服务。"
  err "Docker 未就绪，无法继续部署。"
  exit 1
}

build_frontend_dist() {
  info "构建前端静态资源（Docker Node 容器）..."
  mkdir -p "$ROOT/frontend/dist"
  docker run --rm \
    --user "$(id -u):$(id -g)" \
    -v "$ROOT/frontend:/app" \
    -w /app \
    node:20-alpine \
    sh -lc "npm ci --cache /tmp/npm-cache && npm run build"
}

ensure_self_signed_cert() {
  mkdir -p "$CERT_DIR"
  if [[ -f "$CERT_FILE" && -f "$KEY_FILE" ]]; then
    info "检测到现有证书，跳过自签名生成。"
    return 0
  fi
  info "生成默认自签名证书（100 年）..."
  openssl req -x509 -newkey rsa:4096 -sha256 -nodes \
    -days 36500 \
    -keyout "$KEY_FILE" \
    -out "$CERT_FILE" \
    -subj "/C=CN/ST=Default/L=Default/O=LOOKING GLASS/OU=DevOps/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
}

start_stack() {
  info "启动容器服务..."
  docker compose up -d --build
}

print_result() {
  echo
  info "部署完成。"
  echo "访问地址："
  echo "  - HTTPS: https://localhost"
  echo "  - HTTP:  http://localhost  (自动跳转到 HTTPS)"
  echo "默认账号：admin / admin123"
  echo
  echo "常用命令："
  echo "  - 查看状态: docker compose ps"
  echo "  - 查看日志: docker compose logs -f"
  echo "  - 停止服务: docker compose down"
}

main() {
  ensure_system_deps
  ensure_docker_running
  ensure_self_signed_cert
  build_frontend_dist
  start_stack
  print_result
}

main "$@"
