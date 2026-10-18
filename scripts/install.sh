#!/usr/bin/env bash
#
# LearningProgram 一键安装脚本（bash：Git Bash / WSL / Linux / macOS）
#
# 做什么：
#   1. 检查运行环境（JDK 21+、Maven、Node 22.18+、包管理器、mysql 客户端）
#   2. 收集 MySQL / Redis 连接信息：环境变量里已有的直接沿用，
#      没指定的（含仍是 .env.example 占位符的）会逐个提示手动输入
#   3. 生成 learning-program-backend/.env 与 learning-program-frontend/.env.local（原文件先备份）
#   4. 建库 learning 并导入 scripts/learning.sql（整库 DDL）
#   5. 打包后端（mvnw package）、安装依赖并构建前端（pnpm/npm）
#
# 用法：
#   bash scripts/install.sh                 # 交互式安装
#   bash scripts/install.sh -y              # 非交互：缺少必填环境变量时直接报错退出
#   bash scripts/install.sh -i              # 强制交互（即使 stdin 不是终端）
#   bash scripts/install.sh --no-db         # 跳过建库与建表
#   bash scripts/install.sh --no-build      # 只做配置与数据库，不编译打包
#   bash scripts/install.sh --no-frontend   # 跳过前端依赖安装与构建
#   bash scripts/install.sh --force-env     # 覆盖已有 .env / .env.local（默认也会备份后覆盖）
#   bash scripts/install.sh --reset-db      # 删库重建后重新导入（会丢数据，需确认）
#   bash scripts/install.sh --no-write-env  # 不写文件，只把 export 语句打印出来
#   bash scripts/install.sh --start         # 安装完成后前台启动后端
#
# 先用环境变量注入（脚本不再提问）：
#   MYSQL_USERNAME=root MYSQL_PASSWORD=secret bash scripts/install.sh -y
#
# 说明：export MYSQL_PASSWORD=（设为空字符串）表示「明确使用空密码」；
#       完全不设置该变量才会触发交互提问。

set -euo pipefail

# ---------------------------------------------------------------- 路径与常量

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# INSTALL_PROJECT_ROOT 仅用于自测：把脚本指向另一个「长得一样」的项目目录
PROJECT_ROOT="${INSTALL_PROJECT_ROOT:-$(cd "$SCRIPT_DIR/.." && pwd)}"
BACKEND_DIR="$PROJECT_ROOT/learning-program-backend"
FRONTEND_DIR="$PROJECT_ROOT/learning-program-frontend"
SCHEMA_FILE="$SCRIPT_DIR/learning.sql"
BACKEND_ENV="$BACKEND_DIR/.env"
FRONTEND_ENV="$FRONTEND_DIR/.env.local"

DEFAULT_DB_NAME="learning"
DEFAULT_BACKEND_PORT="1231"
DEFAULT_FRONTEND_PORT="5173"

# .env.example 里的示例值，出现这些值一律当作「未配置」，转去提问
PLACEHOLDER_VALUES="your_mysql_user your_mysql_password your_redis_password changeme example.com"

# 收集用户确认过的配置，最后统一打印
declare -a CONFIG_SUMMARY=()

# ---------------------------------------------------------------- 参数解析

NON_INTERACTIVE=0
DO_DB=1
DO_BUILD=1
DO_FRONTEND=1
FORCE_ENV=0
RESET_DB=0
WRITE_ENV=1
START_AFTER_INSTALL=0

usage() {
  sed -n '2,29p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
  exit 0
}

while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help) usage ;;
    -y|--yes) NON_INTERACTIVE=1 ;;
    -i|--interactive) FORCE_INTERACTIVE=1 ;;
    --no-db) DO_DB=0 ;;
    --no-build) DO_BUILD=0 ;;
    --no-frontend) DO_FRONTEND=0 ;;
    --force-env) FORCE_ENV=1 ;;
    --reset-db) RESET_DB=1 ;;
    --force-migrate)
      printf '  ! --force-migrate 已废弃：迁移脚本已移除，建表统一由 scripts/learning.sql 完成，本次忽略该参数\n' >&2
      ;;
    --no-write-env) WRITE_ENV=0 ;;
    --start) START_AFTER_INSTALL=1 ;;
    *) echo "未知参数：$1（用 --help 查看用法）" >&2; exit 2 ;;
  esac
  shift
done

# 无 TTY（CI、被管道调用）时自动进入非交互模式；--interactive 可强制走交互
if [ ! -t 0 ] && [ "${FORCE_INTERACTIVE:-0}" != "1" ]; then
  NON_INTERACTIVE=1
fi

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

# 是否是占位符/空值
is_placeholder() {
  local value="${1:-}"
  [ -z "$value" ] && return 0
  for placeholder in $PLACEHOLDER_VALUES; do
    [ "$value" = "$placeholder" ] && return 0
  done
  return 1
}

# 取「可用的」变量值：空值或占位符都视为未配置
usable_value() {
  local value="${1:-}"
  if is_placeholder "$value"; then
    printf ''
  else
    printf '%s' "$value"
  fi
}

# 变量是否被显式设置过（含显式设为空字符串）
is_explicitly_set() {
  local name="$1"
  [ -n "${!name+x}" ]
}

# 敏感值打码
mask() {
  local value="${1:-}"
  [ -z "$value" ] && { printf '(空)'; return; }
  printf '******（%s 位）' "${#value}"
}

remember() {
  CONFIG_SUMMARY+=("$1 = $2")
}

# 写 .env 时对值做转义加引号，避免密码里的空格/引号/美元符破坏 source
env_quote() {
  local value="${1-}"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//\$/\\\$}"
  value="${value//\`/\\\`}"
  printf '"%s"' "$value"
}

# ---------------------------------------------------------------- 工具链检查

version_ge() { # version_ge 当前 需要
  [ "$(printf '%s\n%s\n' "$2" "$1" | sort -V | head -1)" = "$2" ]
}

check_toolchain() {
  step "检查运行环境"

  # JDK
  if command -v java >/dev/null 2>&1; then
    local java_version
    java_version="$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+)(\.[0-9._]*)?".*/\1/')"
    if [ -n "$java_version" ] && [ "$java_version" -ge 21 ] 2>/dev/null; then
      ok "JDK $java_version"
    else
      warn "检测到 Java ${java_version:-未知}，项目需要 JDK 21+，请升级后再编译"
    fi
  else
    warn "未找到 java，后端无法编译运行（需要 JDK 21+）"
  fi

  # Maven：优先项目自带 wrapper
  if [ -x "$BACKEND_DIR/mvnw" ]; then
    ok "Maven Wrapper：learning-program-backend/mvnw"
  elif command -v mvn >/dev/null 2>&1; then
    ok "Maven：$(mvn -v 2>/dev/null | head -1)"
  else
    warn "未找到 mvnw / mvn，后端无法打包（需要 Maven 3.9+）"
  fi

  # Node
  if command -v node >/dev/null 2>&1; then
    local node_version
    node_version="$(node --version | sed 's/^v//')"
    if version_ge "$node_version" "22.18.0"; then
      ok "Node.js v$node_version"
    else
      warn "Node.js v$node_version 低于项目要求（^22.18.0 || >=24.12.0）"
    fi
  else
    warn "未找到 node，前端无法安装依赖与构建（需要 Node 22.18+）"
  fi

  # 前端包管理器：有 pnpm-lock.yaml 就优先 pnpm
  if [ -f "$FRONTEND_DIR/pnpm-lock.yaml" ] && command -v pnpm >/dev/null 2>&1; then
    FRONTEND_PM="pnpm"
    ok "前端包管理器：pnpm $(pnpm --version 2>/dev/null)"
  elif command -v npm >/dev/null 2>&1; then
    FRONTEND_PM="npm"
    ok "前端包管理器：npm $(npm --version 2>/dev/null)"
  elif command -v pnpm >/dev/null 2>&1; then
    FRONTEND_PM="pnpm"
    ok "前端包管理器：pnpm $(pnpm --version 2>/dev/null)"
  else
    FRONTEND_PM=""
    warn "未找到 npm / pnpm，前端无法安装依赖"
  fi

  # mysql 客户端：装不了就跳过建库，交给用户手工执行
  if command -v mysql >/dev/null 2>&1; then
    MYSQL_CLIENT="$(command -v mysql)"
    ok "mysql 客户端：$MYSQL_CLIENT"
  else
    MYSQL_CLIENT=""
    warn "未找到 mysql 客户端，将跳过建库与建表，稍后按提示手工执行 SQL"
  fi

  # redis 只做提示，密码用不上 redis-cli
  if ! command -v redis-cli >/dev/null 2>&1; then
    info "未找到 redis-cli（不影响安装，仅少一步连通性自检）"
  fi
}

# ---------------------------------------------------------------- 读取已有配置

load_existing_env() {
  step "读取已有配置"
  local loaded=0

  if [ -f "$BACKEND_ENV" ]; then
    # .env 由本项目生成（export KEY=value），来源可信；解析失败不阻断安装
    if (set -a; . "$BACKEND_ENV") >/dev/null 2>&1; then
      loaded=1
      info "已从 learning-program-backend/.env 载入已有配置（作为默认值）"
    else
      warn "learning-program-backend/.env 解析失败，将忽略其中的值"
    fi
  fi

  if [ "$loaded" = "0" ]; then
    info "未发现可复用的 .env，所有连接信息将逐个询问"
  fi
}

# ---------------------------------------------------------------- 环境变量收集

# prompt_var 变量名 说明 默认值 是否敏感 是否必填
prompt_var() {
  local name="$1" label="$2" default="${3:-}" secret="${4:-0}" required="${5:-1}"
  local env_value="${!name-}"
  local value

  # 1) 环境变量已指定（非占位符）→ 直接沿用
  value="$(usable_value "$env_value")"
  if [ -n "$value" ]; then
    export "$name=$value"
    if [ "$secret" = "1" ]; then
      ok "$label：沿用环境变量（$(mask "$value")）"
      remember "$name" "$(mask "$value")"
    else
      ok "$label：沿用环境变量（$value）"
      remember "$name" "$value"
    fi
    return 0
  fi

  # 2) 敏感变量被显式设为空 → 表示「就是空值」（如本机 Redis 无密码）
  if [ "$secret" = "1" ] && is_explicitly_set "$name" && [ -z "$env_value" ]; then
    export "$name="
    ok "$label：环境变量已显式设为空（按无密码处理）"
    remember "$name" "(空)"
    return 0
  fi

  # 3) 非交互模式：能用默认值就用；必填项没有值才报错
  if [ "$NON_INTERACTIVE" = "1" ]; then
    if [ -n "$default" ]; then
      export "$name=$default"
      ok "$label：使用默认值（$default）"
      remember "$name" "$default"
      return 0
    fi
    if [ "$required" = "1" ]; then
      err "$label 未指定：请先 export $name=... 或去掉 -y 交互安装"
      MISSING_VARS+=("$name")
      return 1
    fi
    export "$name="
    ok "$label：未指定，按空值处理"
    remember "$name" "(空)"
    return 0
  fi

  # 4) 交互模式：提示手动输入
  local hint="$name"
  [ -n "$default" ] && hint="$hint，回车使用默认值 $default"
  [ "$secret" = "1" ] && hint="$hint，输入不回显"
  local attempts=0
  while :; do
    local answer=''
    if [ "$secret" = "1" ]; then
      printf '  %s请输入 %s%s ' "$C_BOLD" "$label" "$C_RESET" >&2
      read -rs answer || true
      printf '\n' >&2
    else
      printf '  %s请输入 %s%s（%s）: ' "$C_BOLD" "$label" "$C_RESET" "$hint" >&2
      read -r answer || true
    fi

    # 回车：有默认值就用默认值
    if [ -z "$answer" ] && [ -n "$default" ]; then
      answer="$default"
    fi

    if [ -n "$answer" ]; then
      export "$name=$answer"
      if [ "$secret" = "1" ]; then
        remember "$name" "$(mask "$answer")"
      else
        remember "$name" "$answer"
      fi
      return 0
    fi

    # 空输入：敏感且非必填（Redis 密码）直接用空；敏感且必填（MySQL 密码）确认一次
    if [ "$secret" = "1" ]; then
      if [ "$required" != "1" ]; then
        export "$name="
        ok "$label：按空值处理"
        remember "$name" "(空)"
        return 0
      fi
      local confirm=''
      printf '  %s%s 为空，确认使用空密码？(y/N): %s' "$C_YELLOW" "$label" "$C_RESET" >&2
      read -r confirm || true
      if [ "${confirm,,}" = "y" ]; then
        export "$name="
        remember "$name" "(空)"
        return 0
      fi
    elif [ "$required" != "1" ]; then
      export "$name="
      ok "$label：按空值处理"
      remember "$name" "(空)"
      return 0
    fi

    attempts=$((attempts + 1))
    if [ "$attempts" -ge 3 ]; then
      err "$label 连续 3 次为空，安装中止"
      exit 1
    fi
    warn "$label 不能为空，请重新输入"
  done
}

collect_env() {
  step "配置数据库与 Redis 连接（未指定时手动输入）"

  MISSING_VARS=()
  prompt_var MYSQL_USERNAME "MySQL 用户名" "" 0 1 || true
  prompt_var MYSQL_PASSWORD "MySQL 密码" "" 1 1 || true
  prompt_var MYSQL_HOST "MySQL 地址" "127.0.0.1" 0 0 || true
  prompt_var MYSQL_PORT "MySQL 端口" "3306" 0 0 || true
  prompt_var REDIS_HOST "Redis 地址" "127.0.0.1" 0 0 || true
  prompt_var REDIS_PORT "Redis 端口" "6379" 0 0 || true
  prompt_var REDIS_PASSWORD "Redis 密码（无密码直接回车）" "" 1 0 || true

  # 端口必须是数字
  for port_var in MYSQL_PORT REDIS_PORT; do
    local port_value="${!port_var-}"
    if ! [[ "$port_value" =~ ^[0-9]+$ ]]; then
      die "$port_var 必须是数字，当前值：$port_value"
    fi
  done

  if [ "${#MISSING_VARS[@]}" -gt 0 ]; then
    err "缺少必填环境变量：${MISSING_VARS[*]}"
    err "示例：MYSQL_USERNAME=root MYSQL_PASSWORD=secret bash scripts/install.sh -y"
    exit 1
  fi
}

# ---------------------------------------------------------------- 写配置文件

backup_file() {
  local path="$1"
  local backup="$path.bak.$(date +%Y%m%d%H%M%S)"
  cp "$path" "$backup"
  info "已备份原文件 → $backup"
}

write_env_files() {
  if [ "$WRITE_ENV" = "0" ]; then
    step "配置输出（--no-write-env：只打印，不写文件）"
    printf 'export MYSQL_USERNAME=%s\n' "$MYSQL_USERNAME"
    printf 'export MYSQL_PASSWORD=%s\n' "$MYSQL_PASSWORD"
    printf 'export MYSQL_HOST=%s\n' "$MYSQL_HOST"
    printf 'export MYSQL_PORT=%s\n' "$MYSQL_PORT"
    printf 'export REDIS_HOST=%s\n' "$REDIS_HOST"
    printf 'export REDIS_PORT=%s\n' "$REDIS_PORT"
    printf 'export REDIS_PASSWORD=%s\n' "$REDIS_PASSWORD"
    return 0
  fi

  step "生成配置文件"

  # 后端：.env（export 语法，source 后即可启动）
  local backend_existing=0
  [ -f "$BACKEND_ENV" ] && backend_existing=1
  if [ "$backend_existing" = "1" ] && [ "$FORCE_ENV" = "0" ]; then
    warn "learning-program-backend/.env 已存在，将备份后按本次输入重写（如需保留请改用 --no-write-env 手工维护）"
  fi
  [ "$backend_existing" = "1" ] && backup_file "$BACKEND_ENV"

  cat > "$BACKEND_ENV" <<EOF
# 由 scripts/install.sh 生成于 $(date '+%Y-%m-%d %H:%M:%S')
# 用法：source .env && ./mvnw spring-boot:run
# 该文件已被 .gitignore 忽略，请勿提交真实账号密码

# MySQL
export MYSQL_USERNAME=$(env_quote "$MYSQL_USERNAME")
export MYSQL_PASSWORD=$(env_quote "$MYSQL_PASSWORD")
export MYSQL_HOST=$(env_quote "$MYSQL_HOST")
export MYSQL_PORT=$(env_quote "$MYSQL_PORT")

# Redis（无密码时留空）
export REDIS_HOST=$(env_quote "$REDIS_HOST")
export REDIS_PORT=$(env_quote "$REDIS_PORT")
export REDIS_PASSWORD=$(env_quote "$REDIS_PASSWORD")
EOF
  chmod 600 "$BACKEND_ENV" 2>/dev/null || true
  ok "已写入 learning-program-backend/.env"

  # 前端：.env.local（Vite 语法）
  if [ "$DO_FRONTEND" = "1" ]; then
    local frontend_existing=0
    [ -f "$FRONTEND_ENV" ] && frontend_existing=1
    [ "$frontend_existing" = "1" ] && backup_file "$FRONTEND_ENV"

    local api_target
    api_target="$(usable_value "${VITE_DEV_API_TARGET:-}")"
    if [ -z "$api_target" ]; then
      api_target="http://127.0.0.1:$DEFAULT_BACKEND_PORT"
      if [ "$NON_INTERACTIVE" = "0" ]; then
        printf '  %s前端 /api 代理目标（VITE_DEV_API_TARGET，回车使用 %s）: %s' \
          "$C_BOLD" "$api_target" "$C_RESET" >&2
        local answer=''
        read -r answer || true
        [ -n "$answer" ] && api_target="$answer"
      fi
    fi
    ok "前端代理目标：$api_target"
    remember "VITE_DEV_API_TARGET" "$api_target"

    cat > "$FRONTEND_ENV" <<EOF
# 由 scripts/install.sh 生成于 $(date '+%Y-%m-%d %H:%M:%S')
# dev / preview 时 vite 把 /api 转发到后端；改完需重启 dev server

VITE_DEV_API_TARGET=$(env_quote "$api_target")

# 前端直连后端地址；留空表示与前端同源（推荐由 nginx 反向代理）
VITE_API_BASE_URL=
EOF
    ok "已写入 learning-program-frontend/.env.local"
  fi
}

# ---------------------------------------------------------------- 数据库

mysql_exec() { # mysql_exec <sql>
  MYSQL_PWD="${MYSQL_PASSWORD:-}" "$MYSQL_CLIENT" \
    -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USERNAME" \
    --default-character-set=utf8mb4 --connect-timeout=8 -N -B -e "$1"
}

mysql_exec_file() { # mysql_exec_file <file> [db]
  local db="${2:-$DEFAULT_DB_NAME}"
  MYSQL_PWD="${MYSQL_PASSWORD:-}" "$MYSQL_CLIENT" \
    -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USERNAME" \
    --default-character-set=utf8mb4 --connect-timeout=8 "$db" < "$1"
}

# scripts/learning.sql 里 CREATE TABLE 的条数，用于导入后核对
expected_table_count() {
  [ -f "$SCHEMA_FILE" ] || { printf '0'; return 0; }
  local n
  n="$(grep -c '^CREATE TABLE' "$SCHEMA_FILE" 2>/dev/null || true)"
  printf '%s' "${n:-0}"
}

table_count() { # 当前库里已有多少张表
  mysql_exec "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DEFAULT_DB_NAME';" 2>/dev/null | tr -d '[:space:]'
}

create_database() {
  mysql_exec "CREATE DATABASE IF NOT EXISTS $DEFAULT_DB_NAME DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" >/dev/null 2>&1
}

confirm_reset() {
  local expected
  expected="$(expected_table_count)"
  warn "即将删除数据库 $DEFAULT_DB_NAME（其中数据全部丢失），再按 $(basename "$SCHEMA_FILE") 重建 $expected 张表"
  if [ "$NON_INTERACTIVE" = "1" ]; then
    warn "非交互模式：已通过 --reset-db 显式指定，直接执行"
    return 0
  fi
  printf '  %s确认请输入 yes（其他任意输入取消）: %s' "$C_BOLD" "$C_RESET" >&2
  local answer=''
  read -r answer || true
  [ "$answer" = "yes" ]
}

import_schema() {
  local expected actual
  expected="$(expected_table_count)"
  info "导入 scripts/$(basename "$SCHEMA_FILE")（$expected 张表）"

  if ! mysql_exec_file "$SCHEMA_FILE" >/dev/null 2>&1; then
    err "导入 $(basename "$SCHEMA_FILE") 失败"
    info "可手工执行：mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USERNAME -p $DEFAULT_DB_NAME < scripts/learning.sql"
    return 1
  fi

  actual="$(table_count 2>/dev/null || true)"
  if [ "${actual:-0}" = "$expected" ]; then
    ok "建表完成：$actual 张表"
    return 0
  fi

  warn "建表结果与预期不一致：预期 $expected 张，实际 ${actual:-未知} 张"
  info "请检查 $SCHEMA_FILE 是否完整，或删除数据库后重新执行 bash scripts/install.sh --reset-db --no-build"
  return 1
}

init_database() {
  step "初始化数据库"

  if [ ! -f "$SCHEMA_FILE" ]; then
    err "未找到建表脚本：$SCHEMA_FILE"
    info "该文件是表结构的唯一来源，请确认 scripts/learning.sql 存在"
    return 1
  fi

  if [ -z "$MYSQL_CLIENT" ]; then
    warn "跳过建库与建表：本机没有 mysql 客户端"
    info "请手工执行："
    info "  1) CREATE DATABASE IF NOT EXISTS $DEFAULT_DB_NAME DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    info "  2) mysql -u $MYSQL_USERNAME -p $DEFAULT_DB_NAME < scripts/learning.sql"
    return 0
  fi

  if ! mysql_exec "SELECT 1;" >/dev/null 2>&1; then
    err "MySQL 连接失败：$MYSQL_USERNAME@$MYSQL_HOST:$MYSQL_PORT"
    info "请检查账号密码、服务是否启动、以及该账号是否有远程访问权限"
    return 1
  fi
  ok "MySQL 连接成功（$MYSQL_HOST:$MYSQL_PORT）"

  if create_database; then
    ok "数据库 $DEFAULT_DB_NAME 已就绪"
  else
    err "创建数据库 $DEFAULT_DB_NAME 失败：请确认账号 $MYSQL_USERNAME 具备建库权限"
    return 1
  fi

  local existing
  if ! existing="$(table_count)"; then
    warn "无法读取表清单（权限不足或库不存在），跳过建表"
    info "请手工导入：mysql -u $MYSQL_USERNAME -p $DEFAULT_DB_NAME < scripts/learning.sql"
    return 0
  fi
  existing="${existing:-0}"

  if [ "$existing" != "0" ]; then
    info "数据库 $DEFAULT_DB_NAME 已有 $existing 张表（$(basename "$SCHEMA_FILE") 定义 $(expected_table_count) 张）"
    if [ "$RESET_DB" != "1" ]; then
      info "为避免覆盖现有数据，跳过建表"
      info "需要按 scripts/learning.sql 重建时加 --reset-db（会清空全部数据）"
      return 0
    fi
    if ! confirm_reset; then
      info "已取消，数据库保持原样"
      return 0
    fi
    if ! mysql_exec "DROP DATABASE IF EXISTS $DEFAULT_DB_NAME;" >/dev/null 2>&1; then
      err "删除数据库 $DEFAULT_DB_NAME 失败：请确认账号 $MYSQL_USERNAME 具备删库权限"
      return 1
    fi
    ok "已删除数据库 $DEFAULT_DB_NAME"
    if ! create_database; then
      err "重建数据库 $DEFAULT_DB_NAME 失败"
      return 1
    fi
  else
    info "检测到空数据库，开始建表"
  fi

  import_schema
}

# ---------------------------------------------------------------- 构建

build_backend() {
  step "打包后端"
  local mvn_cmd
  if [ -x "$BACKEND_DIR/mvnw" ]; then
    mvn_cmd="$BACKEND_DIR/mvnw"
  elif command -v mvn >/dev/null 2>&1; then
    mvn_cmd="mvn"
  else
    warn "跳过后端打包：未找到 mvnw / mvn"
    return 0
  fi

  if (cd "$BACKEND_DIR" && "$mvn_cmd" -q -DskipTests package); then
    ok "后端打包完成（产物在 learning-program-backend/target/*.jar）"
  else
    err "后端打包失败，请查看上面的 Maven 输出"
    return 1
  fi
}

build_frontend() {
  step "安装并构建前端"
  if [ -z "$FRONTEND_PM" ]; then
    warn "跳过前端：未找到 npm / pnpm"
    return 0
  fi

  if ! (cd "$FRONTEND_DIR" && "$FRONTEND_PM" install); then
    err "前端依赖安装失败"
    return 1
  fi
  ok "$FRONTEND_PM install 完成"

  if ! (cd "$FRONTEND_DIR" && "$FRONTEND_PM" run build); then
    err "前端构建失败（build 会先做类型检查）"
    return 1
  fi
  ok "前端构建完成（产物在 learning-program-frontend/dist）"
}

# ---------------------------------------------------------------- 收尾

print_summary() {
  step "安装结果"
  local item
  for item in "${CONFIG_SUMMARY[@]:-}"; do
    [ -n "$item" ] && info "$item"
  done

  printf '\n%s下一步：%s\n' "$C_BOLD" "$C_RESET"
  printf '  1) 启动后端：source learning-program-backend/.env && cd learning-program-backend && ./mvnw spring-boot:run\n'
  printf '     接口地址：http://localhost:%s\n' "$DEFAULT_BACKEND_PORT"
  printf '  2) 启动前端：cd learning-program-frontend && %s run dev\n' "${FRONTEND_PM:-npm}"
  printf '     页面地址：http://localhost:%s\n' "$DEFAULT_FRONTEND_PORT"
  printf '  3) 管理员账号与更多说明见 docs/INSTALLATION.md\n\n'
}

start_backend() {
  step "启动后端（Ctrl+C 停止）"
  # shellcheck disable=SC1090
  set -a; . "$BACKEND_ENV"; set +a
  cd "$BACKEND_DIR"
  if [ -x ./mvnw ]; then
    exec ./mvnw spring-boot:run
  else
    exec mvn spring-boot:run
  fi
}

# ---------------------------------------------------------------- 主流程

main() {
  printf '%sLearningProgram 安装脚本%s\n' "$C_BOLD" "$C_RESET"
  printf '项目目录：%s\n' "$PROJECT_ROOT"

  [ -d "$BACKEND_DIR" ] || die "未找到后端目录：$BACKEND_DIR"
  [ -d "$FRONTEND_DIR" ] || die "未找到前端目录：$FRONTEND_DIR"

  if [ "$NON_INTERACTIVE" = "1" ]; then
    info "非交互模式：只使用环境变量与默认值，缺少必填项会直接报错"
  fi

  check_toolchain
  load_existing_env
  collect_env
  write_env_files

  if [ "$DO_DB" = "1" ]; then
    init_database || warn "数据库步骤未完成，可修好后重跑：bash scripts/install.sh --no-build"
  else
    step "初始化数据库"
    info "已按 --no-db 跳过"
  fi

  if [ "$DO_BUILD" = "1" ]; then
    build_backend || true
    [ "$DO_FRONTEND" = "1" ] && { build_frontend || true; }
  else
    step "构建"
    info "已按 --no-build 跳过"
  fi

  print_summary

  if [ "$START_AFTER_INSTALL" = "1" ]; then
    start_backend
  fi
}

main "$@"
