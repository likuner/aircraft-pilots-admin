#!/usr/bin/env bash
# =============================================
# 无人机驾驶员管理后台 - 一键启动脚本
# 用法: ./scripts/start.sh [backend|frontend|all|stop]
# =============================================
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_LOG="${ROOT}/logs/backend.log"
FRONTEND_LOG="${ROOT}/logs/frontend.log"

JAVA_HOME_CANDIDATES=(
  "/usr/local/opt/openjdk@17"
  "/opt/homebrew/opt/openjdk@17"
  "$HOME/.sdkman/candidates/java/current"
)

find_java17() {
  for c in "${JAVA_HOME_CANDIDATES[@]}"; do
    if [ -x "$c/bin/java" ] && "$c/bin/java" -version 2>&1 | grep -q "17"; then
      echo "$c"
      return 0
    fi
  done
  # fallback: PATH 中的 java
  if java -version 2>&1 | grep -q "17"; then
    echo ""
    return 0
  fi
  echo ""
}

NODE_BIN="$(command -v node || true)"
if [ -z "$NODE_BIN" ]; then
  for n in "$HOME/.workbuddy/binaries/node/versions/"*/bin/node; do
    [ -x "$n" ] && NODE_BIN="$n" && break
  done
fi
NPM_BIN="$(dirname "$NODE_BIN" 2>/dev/null)/npm"

start_backend() {
  echo ">>> 启动后端 (Spring Boot :8080) ..."
  mkdir -p "$ROOT/logs"
  JH="$(find_java17)"
  if [ -n "$JH" ]; then
    export JAVA_HOME="$JH"
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
  # 清除可能干扰端口的环境变量
  unset SERVER__PORT SERVER_PORT 2>/dev/null || true
  cd "$ROOT/backend"
  nohup ./mvnw -q spring-boot:run > "$BACKEND_LOG" 2>&1 &
  echo "PID: $!"
  echo "后端日志: $BACKEND_LOG"
}

start_frontend() {
  echo ">>> 启动前端 (Vite :5173) ..."
  mkdir -p "$ROOT/logs"
  if [ -n "$NPM_BIN" ]; then
    cd "$ROOT/frontend"
    nohup "$NPM_BIN" run dev > "$FRONTEND_LOG" 2>&1 &
    echo "PID: $!"
    echo "前端日志: $FRONTEND_LOG"
  else
    echo "!!! 未找到 node/npm，请手动启动前端"
  fi
}

stop_all() {
  echo ">>> 停止前后端进程 ..."
  lsof -ti :8080 | xargs kill 2>/dev/null || true
  lsof -ti :5173 | xargs kill 2>/dev/null || true
  echo "已停止。"
}

case "${1:-all}" in
  backend)  start_backend ;;
  frontend) start_frontend ;;
  all)      start_backend; start_frontend ;;
  stop)     stop_all ;;
  *) echo "用法: $0 [backend|frontend|all|stop]"; exit 1 ;;
esac
