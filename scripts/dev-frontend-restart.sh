#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="${ROOT_DIR}/frontend-preview"
PORT="${FRONTEND_PORT:-5666}"
HOST="${FRONTEND_HOST:-0.0.0.0}"
PROXY_TARGET="${VITE_PROXY_TARGET:-http://localhost:8080}"
LOG_DIR="${ROOT_DIR}/output/dev-logs"
LOG_FILE="${LOG_DIR}/frontend-${PORT}.log"
PID_FILE="${LOG_DIR}/frontend-${PORT}.pid"
TMUX_SESSION="mtravel-frontend-${PORT}"

mkdir -p "${LOG_DIR}"

stop_port_process() {
  if command -v tmux >/dev/null 2>&1 && tmux has-session -t "${TMUX_SESSION}" 2>/dev/null; then
    echo "Stopping tmux session: ${TMUX_SESSION}"
    tmux kill-session -t "${TMUX_SESSION}" || true
  fi

  # 兼容之前手工启动时留下的旧会话，避免 5666 被异常 Vite 进程占住。
  if command -v tmux >/dev/null 2>&1 && tmux has-session -t "yanxue_frontend" 2>/dev/null; then
    echo "Stopping legacy tmux session: yanxue_frontend"
    tmux kill-session -t "yanxue_frontend" || true
  fi

  local pids
  pids="$(lsof -tiTCP:"${PORT}" -sTCP:LISTEN || true)"
  if [[ -z "${pids}" ]]; then
    return 0
  fi

  echo "Stopping frontend process on ${PORT}: ${pids}"
  kill ${pids} >/dev/null 2>&1 || true

  for _ in {1..20}; do
    if [[ -z "$(lsof -tiTCP:"${PORT}" -sTCP:LISTEN || true)" ]]; then
      return 0
    fi
    sleep 0.25
  done

  pids="$(lsof -tiTCP:"${PORT}" -sTCP:LISTEN || true)"
  if [[ -n "${pids}" ]]; then
    echo "Force stopping frontend process on ${PORT}: ${pids}"
    kill -9 ${pids} >/dev/null 2>&1 || true
  fi
}

wait_frontend() {
  local url="http://127.0.0.1:${PORT}/"
  for _ in {1..80}; do
    if curl -m 2 -fsS "${url}" >/dev/null 2>&1; then
      echo "Frontend is UP: ${url}"
      return 0
    fi
    sleep 0.25
  done

  echo "Frontend did not become available. Last log lines:"
  tail -120 "${LOG_FILE}" || true
  return 1
}

build_start_command() {
  {
    printf 'export VITE_PROXY_TARGET=%q\n' "${PROXY_TARGET}"
    printf 'cd %q\n' "${FRONTEND_DIR}"
    # 不使用 pnpm run dev -- --host ...，否则 Vite 会把 host/port 当成位置参数，导致实际端口跑偏。
    printf 'exec pnpm --filter @vben/web-antd exec vite --mode development --host %q --port %q > %q 2>&1\n' "${HOST}" "${PORT}" "${LOG_FILE}"
  }
}

stop_port_process

echo "Starting frontend on ${PORT}"
echo "Proxy target: ${PROXY_TARGET}"
echo "Log: ${LOG_FILE}"

if command -v tmux >/dev/null 2>&1; then
  # tmux 会话不依赖当前 shell 生命周期，脚本退出后前端仍能稳定留在后台。
  tmux new-session -d -s "${TMUX_SESSION}" "$(build_start_command)"
  echo "${TMUX_SESSION}" >"${PID_FILE}"
else
  (
    cd "${FRONTEND_DIR}"
    VITE_PROXY_TARGET="${PROXY_TARGET}" nohup pnpm --filter @vben/web-antd exec vite --mode development --host "${HOST}" --port "${PORT}" >"${LOG_FILE}" 2>&1 < /dev/null &
    echo "$!" >"${PID_FILE}"
  )
fi

wait_frontend
