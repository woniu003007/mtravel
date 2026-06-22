#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCAL_ENV_FILE="${ROOT_DIR}/.env.local"
EXAMPLE_ENV_FILE="${ROOT_DIR}/.env.example"

if [[ -f "${LOCAL_ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${LOCAL_ENV_FILE}"
  set +a
else
  echo "缺少本地环境文件：${LOCAL_ENV_FILE}"
  echo "请先复制示例文件并填写数据库、Redis 密码："
  echo "  cp ${EXAMPLE_ENV_FILE} ${LOCAL_ENV_FILE}"
  echo "  open ${LOCAL_ENV_FILE}"
  exit 1
fi

export SERVER_PORT="${SERVER_PORT:-8080}"
export DB_HOST="${DB_HOST:-}"
export DB_PORT="${DB_PORT:-5432}"
export DB_NAME="${DB_NAME:-mtravel}"
export DB_USER="${DB_USER:-}"
export REDIS_HOST="${REDIS_HOST:-}"
export REDIS_PORT="${REDIS_PORT:-6379}"
export REDIS_DATABASE="${REDIS_DATABASE:-0}"
export FLYWAY_ENABLED="${FLYWAY_ENABLED:-false}"

required_vars=(
  DB_HOST
  DB_USER
  DB_PASSWORD
  REDIS_HOST
  REDIS_PASSWORD
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "环境变量 ${var_name} 为空，请检查 ${LOCAL_ENV_FILE}"
    exit 1
  fi
done

exec "${ROOT_DIR}/scripts/dev-backend-restart.sh"
