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

if [[ "${EUID:-$(id -u)}" -eq 0 ]]; then
  SUDO=""
else
  SUDO="sudo"
fi

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
  $SUDO apt-get update

  # 先尝试系统仓库安装，部分系统没有 docker-compose-plugin 需走回退逻辑
  if $SUDO apt-get install -y docker.io docker-compose-plugin openssl; then
    $SUDO systemctl enable --now docker || true
    return 0
  fi

  warn "系统仓库缺少 docker-compose-plugin，切换到 Docker 官方仓库安装..."
  $SUDO apt-get install -y ca-certificates curl gnupg lsb-release openssl
  $SUDO install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | $SUDO gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  $SUDO chmod a+r /etc/apt/keyrings/docker.gpg

  UBUNTU_CODENAME="$(. /etc/os-release && echo "${VERSION_CODENAME:-jammy}")"
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${UBUNTU_CODENAME} stable" \
    | $SUDO tee /etc/apt/sources.list.d/docker.list >/dev/null

  $SUDO apt-get update
  $SUDO apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  $SUDO systemctl enable --now docker || true

  # 再兜底：如果依旧没有 compose plugin，安装独立 docker-compose
  if ! docker compose version >/dev/null 2>&1; then
    warn "docker compose 插件仍不可用，安装独立 docker-compose..."
    $SUDO apt-get install -y docker-compose || true
  fi
  return 0
}

install_with_dnf_or_yum() {
  if has_cmd dnf; then
    info "检测到 dnf，尝试安装依赖..."
    $SUDO dnf install -y docker docker-compose-plugin openssl || $SUDO dnf install -y docker docker-compose openssl
    $SUDO systemctl enable --now docker || true
    return 0
  fi
  if has_cmd yum; then
    info "检测到 yum，尝试安装依赖..."
    $SUDO yum install -y docker docker-compose openssl || $SUDO yum install -y docker openssl
    $SUDO systemctl enable --now docker || true
    return 0
  fi
  return 1
}

compose_ok() {
  docker compose version >/dev/null 2>&1 || has_cmd docker-compose
}

dc() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  elif has_cmd docker-compose; then
    docker-compose "$@"
  else
    err "未检测到 docker compose / docker-compose。"
    exit 1
  fi
}

ensure_system_deps() {
  info "检查系统依赖..."
  if has_cmd docker && compose_ok && has_cmd openssl; then
    info "系统依赖已满足。"
    return 0
  fi

  install_with_brew || install_with_apt || install_with_dnf_or_yum || warn "未识别的包管理器，请手动安装 Docker / Docker Compose / OpenSSL。"

  if ! has_cmd docker; then
    err "未检测到 docker，请先安装 Docker 后重试。"
    exit 1
  fi
  if ! compose_ok; then
    err "未检测到 docker compose / docker-compose，请先安装后重试。"
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
  dc up -d --build
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
  if docker compose version >/dev/null 2>&1; then
    echo "  - 查看状态: docker compose ps"
    echo "  - 查看日志: docker compose logs -f"
    echo "  - 停止服务: docker compose down"
  else
    echo "  - 查看状态: docker-compose ps"
    echo "  - 查看日志: docker-compose logs -f"
    echo "  - 停止服务: docker-compose down"
  fi
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
