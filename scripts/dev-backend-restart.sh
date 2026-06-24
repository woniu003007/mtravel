#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/backend"
PORT="${SERVER_PORT:-8080}"
LOG_DIR="${ROOT_DIR}/output/dev-logs"
LOG_FILE="${LOG_DIR}/backend-${PORT}.log"
PID_FILE="${LOG_DIR}/backend-${PORT}.pid"
TMUX_SESSION="mtravel-backend-${PORT}"

mkdir -p "${LOG_DIR}"

inherit_running_env() {
  local current_pid env_name env_value
  current_pid="$(lsof -tiTCP:"${PORT}" -sTCP:LISTEN | head -1 || true)"
  if [[ -z "${current_pid}" ]]; then
    return 0
  fi

  # 本地开发经常长时间保留后端进程。手动重启时如果当前 shell 没有数据库或 Redis
  # 环境变量，直接使用 application.yml 默认值会误连 localhost。这里仅从当前正在监听
  # 端口的后端进程继承必要连接配置，避免重启脚本破坏现有联调环境。
  while IFS='=' read -r env_name env_value; do
    if [[ -n "${env_name}" && -z "${!env_name:-}" ]]; then
      export "${env_name}=${env_value}"
    fi
  done < <(
    ps eww -p "${current_pid}" |
      tr ' ' '\n' |
      grep -E '^(DB_HOST|DB_PORT|DB_NAME|DB_USER|DB_PASSWORD|REDIS_HOST|REDIS_PORT|REDIS_PASSWORD|REDIS_DATABASE|REDIS_TIMEOUT|FLYWAY_ENABLED|DEFAULT_TENANT_ID|JWT_SECRET|ACCESS_TOKEN_MINUTES|IDLE_TIMEOUT_MINUTES|DEMO_USERNAME|DEMO_PASSWORD|BAILIAN_API_KEY|BAILIAN_TEXT_MODEL|BAILIAN_VISION_MODEL)=' || true
  )
}

stop_port_process() {
  if command -v tmux >/dev/null 2>&1 && tmux has-session -t "${TMUX_SESSION}" 2>/dev/null; then
    echo "Stopping tmux session: ${TMUX_SESSION}"
    tmux kill-session -t "${TMUX_SESSION}" || true
  fi

  local pids
  pids="$(lsof -tiTCP:"${PORT}" -sTCP:LISTEN || true)"
  if [[ -z "${pids}" ]]; then
    return 0
  fi

  echo "Stopping backend process on ${PORT}: ${pids}"
  kill ${pids} >/dev/null 2>&1 || true

  for _ in {1..20}; do
    if [[ -z "$(lsof -tiTCP:"${PORT}" -sTCP:LISTEN || true)" ]]; then
      return 0
    fi
    sleep 0.25
  done

  pids="$(lsof -tiTCP:"${PORT}" -sTCP:LISTEN || true)"
  if [[ -n "${pids}" ]]; then
    echo "Force stopping backend process on ${PORT}: ${pids}"
    kill -9 ${pids} >/dev/null 2>&1 || true
  fi
}

wait_health() {
  local url="http://127.0.0.1:${PORT}/actuator/health"
  for _ in {1..80}; do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      echo "Backend is UP: ${url}"
      return 0
    fi
    sleep 0.25
  done

  echo "Backend did not become healthy. Last log lines:"
  tail -80 "${LOG_FILE}" || true
  return 1
}

shell_export() {
  local name="$1"
  local value="${!name:-}"
  printf 'export %s=%q\n' "${name}" "${value}"
}

build_start_command() {
  {
    for name in \
      SERVER_PORT DB_HOST DB_PORT DB_NAME DB_USER DB_PASSWORD \
      REDIS_HOST REDIS_PORT REDIS_PASSWORD REDIS_DATABASE REDIS_TIMEOUT FLYWAY_ENABLED \
      DEFAULT_TENANT_ID JWT_SECRET ACCESS_TOKEN_MINUTES IDLE_TIMEOUT_MINUTES \
      DEMO_USERNAME DEMO_PASSWORD \
      BAILIAN_API_KEY BAILIAN_TEXT_MODEL BAILIAN_VISION_MODEL; do
      if [[ -n "${!name:-}" ]]; then
        shell_export "${name}"
      fi
    done
    printf 'cd %q\n' "${BACKEND_DIR}"
    printf 'exec mvn -q -Dmaven.test.skip=true spring-boot:run > %q 2>&1\n' "${LOG_FILE}"
  }
}

inherit_running_env
stop_port_process

echo "Starting backend on ${PORT}"
echo "Log: ${LOG_FILE}"

if command -v tmux >/dev/null 2>&1; then
  # tmux 会话不依赖当前 shell 生命周期，脚本退出后后端仍能稳定留在后台。
  tmux new-session -d -s "${TMUX_SESSION}" "$(build_start_command)"
  echo "${TMUX_SESSION}" >"${PID_FILE}"
else
  (
    cd "${BACKEND_DIR}"
    nohup mvn -q -Dmaven.test.skip=true spring-boot:run >"${LOG_FILE}" 2>&1 < /dev/null &
    echo "$!" >"${PID_FILE}"
  )
fi

wait_health
