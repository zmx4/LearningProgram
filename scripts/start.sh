#!/usr/bin/env bash
#
# LearningProgram 一键启动脚本（bash：Git Bash / WSL / Linux / macOS）
#
# 做什么：
#   1. source learning-program-backend/.env，注入 MySQL / Redis 等环境变量
#   2. 端口预检：后端 1231 / 前端 5173 已被监听时跳过对应组件并提示
#   3. 前端依赖缺失时自动 pnpm install（本项目约定必须用 pnpm）
#   4. 后台启动后端（mvnw spring-boot:run），前台启动前端，日志都打在当前终端
#   5. Ctrl+C 一次性停掉后端与前端
#
# 用法：
#   bash scripts/start.sh                 # 启动后端 + 前端（默认，SPA 模式）
#   bash scripts/start.sh --backend       # 只启动后端
#   bash scripts/start.sh --frontend      # 只启动前端
#   bash scripts/start.sh --ssr           # 前端以 SSR 模式启动（node server.mjs）
#
# 说明：
#   - 首次使用请先运行 bash scripts/install.sh 生成 .env 等配置
#   - JAVA_HOME 未设置时会优先固定到系统的 JDK 21（默认 java 可能是编译不了的 25）
#   - 可用环境变量覆盖端口：BACKEND_PORT（默认 1231）、FRONTEND_PORT（默认 5173）

set -euo pipefail

# ---------------------------------------------------------------- 路径与常量

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/learning-program-backend"
FRONTEND_DIR="$PROJECT_ROOT/learning-program-frontend"
BACKEND_ENV="$BACKEND_DIR/.env"
BACKEND_PORT="${BACKEND_PORT:-1231}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"

# ---------------------------------------------------------------- 参数解析

START_BACKEND=1
START_FRONTEND=1
FRONTEND_SSR=0

usage() {
  sed -n '2,21p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
  exit 0
}

while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help) usage ;;
    --backend) START_FRONTEND=0 ;;
    --frontend) START_BACKEND=0 ;;
    --ssr) FRONTEND_SSR=1 ;;
    *) echo "未知参数：$1（用 --help 查看用法）" >&2; exit 2 ;;
  esac
  shift
done

# ---------------------------------------------------------------- 输出工具

if [ -t 1 ]; then
  C_RESET=$'\033[0m'; C_BOLD=$'\033[1m'; C_DIM=$'\033[2m'
  C_RED=$'\033[31m'; C_GREEN=$'\033[32m'; C_YELLOW=$'\033[33m'; C_BLUE=$'\033[36m'
else
  C_RESET=''; C_BOLD=''; C_DIM=''; C_RED=''; C_GREEN=''; C_YELLOW=''; C_BLUE=''
fi

step()  { printf '\n%s==> %s%s\n' "$C_BOLD$C_BLUE" "$*" "$C_RESET"; }
ok()    { printf '  %s✓%s %s\n' "$C_GREEN" "$C_RESET" "$*"; }
info()  { printf '  %s·%s %s\n' "$C_DIM" "$C_RESET" "$*"; }
warn()  { printf '  %s!%s %s\n' "$C_YELLOW" "$C_RESET" "$*" >&2; }
err()   { printf '  %s✗%s %s\n' "$C_RED" "$C_RESET" "$*" >&2; }

die() { err "$*"; exit 1; }

# ---------------------------------------------------------------- 小工具

# 端口是否已被监听（bash 内建 /dev/tcp，不依赖 ss / lsof）
port_in_use() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

# 递归杀进程树（先杀子进程再杀自己，防止父进程退出后子进程失联）
kill_tree() {
  local pid="$1" child
  if command -v pgrep >/dev/null 2>&1; then
    for child in $(pgrep -P "$pid" 2>/dev/null); do
      kill_tree "$child"
    done
  fi
  kill "$pid" 2>/dev/null || true
}

# WSL/Ubuntu 的默认 java 常是残缺的 JDK 25 JRE，Maven 编译必须用 JDK 21：
# 仅在用户没有显式设置 JAVA_HOME 且系统里存在 JDK 21 时才固定
pin_java_home() {
  [ -n "${JAVA_HOME:-}" ] && return 0
  local candidate
  for candidate in /usr/lib/jvm/java-21-openjdk-amd64 /usr/lib/jvm/java-21-openjdk-arm64 \
                   /usr/lib/jvm/java-21 /opt/homebrew/opt/openjdk@21 /usr/local/opt/openjdk@21; do
    if [ -d "$candidate" ]; then
      export JAVA_HOME="$candidate"
      info "JAVA_HOME 未设置，已固定为 $candidate"
      return 0
    fi
  done
  if command -v java >/dev/null 2>&1; then
    if ! java -version 2>&1 | head -n1 | grep -q 'version "21'; then
      warn "JAVA_HOME 未设置且未找到 JDK 21，当前默认 java 可能无法编译（项目要求 JDK 21）"
    fi
  fi
}

# ---------------------------------------------------------------- 预检与启动

precheck_backend() {
  [ -d "$BACKEND_DIR" ] || die "未找到后端目录：$BACKEND_DIR"
  if [ ! -f "$BACKEND_ENV" ]; then
    die "未找到 $BACKEND_ENV，请先运行 bash scripts/install.sh 生成配置"
  fi
}

load_backend_env() {
  # .env 里是 export 语句，set -a 兜底保证变量对子进程可见
  set -a; . "$BACKEND_ENV"; set +a
}

precheck_frontend() {
  [ -d "$FRONTEND_DIR" ] || die "未找到前端目录：$FRONTEND_DIR"
  command -v pnpm >/dev/null 2>&1 || die "未找到 pnpm：本项目前端约定使用 pnpm，请先 npm install -g pnpm 或 corepack enable"
  if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    step "安装前端依赖（pnpm install）"
    (cd "$FRONTEND_DIR" && pnpm install)
  fi
}

start_backend() {
  step "启动后端（spring-boot:run，端口 $BACKEND_PORT）"
  (
    cd "$BACKEND_DIR"
    if [ -x ./mvnw ]; then
      exec ./mvnw spring-boot:run
    else
      exec mvn spring-boot:run
    fi
  ) &
  BACKEND_PID=$!
  info "后端 PID：$BACKEND_PID（首次编译启动需要一些时间）"
  info "接口地址：http://localhost:$BACKEND_PORT"
}

BACKEND_PID=""
FRONTEND_PID=""

cleanup() {
  # 先杀前端进程树（pnpm → sh → node/vite），再处理后端
  if [ -n "$FRONTEND_PID" ]; then
    kill_tree "$FRONTEND_PID"
    FRONTEND_PID=""
  fi
  if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    printf '\n%s==> 正在停止后端（PID %s）%s\n' "$C_BOLD$C_BLUE" "$BACKEND_PID" "$C_RESET"
    kill "$BACKEND_PID" 2>/dev/null || true
    local i=0
    while kill -0 "$BACKEND_PID" 2>/dev/null && [ "$i" -lt 20 ]; do
      sleep 0.5
      i=$((i + 1))
    done
    if kill -0 "$BACKEND_PID" 2>/dev/null; then
      warn "后端未在 10 秒内退出，强制结束"
      kill -9 "$BACKEND_PID" 2>/dev/null || true
    fi
    ok "后端已停止"
  fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

main() {
  printf '%sLearningProgram 一键启动%s\n' "$C_BOLD" "$C_RESET"
  printf '项目目录：%s\n' "$PROJECT_ROOT"

  # 端口预检：已被监听的组件跳过启动，避免端口冲突循环
  if [ "$START_BACKEND" = 1 ] && port_in_use "$BACKEND_PORT"; then
    warn "后端端口 $BACKEND_PORT 已被占用，假定后端已在运行，跳过启动"
    START_BACKEND=0
  fi
  if [ "$START_FRONTEND" = 1 ] && port_in_use "$FRONTEND_PORT"; then
    warn "前端端口 $FRONTEND_PORT 已被占用，假定前端已在运行，跳过启动"
    START_FRONTEND=0
  fi
  if [ "$START_BACKEND" = 0 ] && [ "$START_FRONTEND" = 0 ]; then
    die "后端与前端都无需启动（对应端口均被占用）"
  fi

  if [ "$START_BACKEND" = 1 ]; then
    precheck_backend
    load_backend_env
    pin_java_home
  fi
  if [ "$START_FRONTEND" = 1 ]; then
    precheck_frontend
  fi

  if [ "$START_BACKEND" = 1 ]; then
    start_backend
  fi

  if [ "$START_FRONTEND" = 1 ]; then
    if [ "$FRONTEND_SSR" = 1 ]; then
      step "启动前端（SSR：node server.mjs，端口 $FRONTEND_PORT）"
    else
      step "启动前端（SPA：pnpm dev，端口 $FRONTEND_PORT）"
    fi
    info "页面地址：http://localhost:$FRONTEND_PORT"
    if [ "$START_BACKEND" = 0 ]; then
      info "后端未由本脚本启动，接口代理目标见 vite.config.ts 与 .env.local"
    fi
    # 前端放后台、用 wait 内建等待：信号到达时 trap 能立即执行，
    # 不会卡在「等外部命令结束才处理信号」上（只杀到脚本本身时也能干净退出）
    cd "$FRONTEND_DIR"
    if [ "$FRONTEND_SSR" = 1 ]; then
      node server.mjs &
    else
      pnpm dev &
    fi
    FRONTEND_PID=$!
    info "按 Ctrl+C 停止全部进程"
    set +e
    wait "$FRONTEND_PID"
    frontend_status=$?
    set -e
    # Ctrl+C（130 / 143）不算异常
    if [ "$frontend_status" -ne 0 ] && [ "$frontend_status" -ne 130 ] && [ "$frontend_status" -ne 143 ]; then
      err "前端异常退出（状态码 $frontend_status）"
    fi
    exit "$frontend_status"
  fi

  if [ "$START_BACKEND" = 1 ]; then
    info "按 Ctrl+C 停止后端"
    set +e
    wait "$BACKEND_PID"
    backend_status=$?
    set -e
    if [ "$backend_status" -eq 0 ]; then
      ok "后端已退出"
    else
      err "后端已退出（状态码 $backend_status）"
    fi
    exit "$backend_status"
  fi
}

main "$@"
