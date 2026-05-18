#!/usr/bin/env bash
# Hook SessionStart — MCP Memory Management
#
# Detects the project stack from build files and injects it as context.
#
# Input:  JSON on stdin (ignored — we read the filesystem)
# Output: JSON on stdout with additionalContext

set -uo pipefail
cat > /dev/null  # drain stdin

ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
STACK=""

if [ -f "$ROOT/pom.xml" ]; then
  JAVA_VER=$(grep -oP '(?<=<java.version>)[^<]+' "$ROOT/pom.xml" 2>/dev/null || \
             grep -oP '(?<=<maven.compiler.source>)[^<]+' "$ROOT/pom.xml" 2>/dev/null || echo "?")
  STACK="Java ${JAVA_VER} + Maven"
  grep -q "spring-boot" "$ROOT/pom.xml" 2>/dev/null && STACK="$STACK + Spring Boot"
  grep -q "postgresql\|postgres" "$ROOT/pom.xml" 2>/dev/null && STACK="$STACK + PostgreSQL"
  grep -q "mysql" "$ROOT/pom.xml" 2>/dev/null && STACK="$STACK + MySQL"
  grep -q "flyway" "$ROOT/pom.xml" 2>/dev/null && STACK="$STACK + Flyway"
  grep -q "liquibase" "$ROOT/pom.xml" 2>/dev/null && STACK="$STACK + Liquibase"
elif [ -f "$ROOT/build.gradle" ] || [ -f "$ROOT/build.gradle.kts" ]; then
  STACK="Java + Gradle"
  grep -q "spring" "$ROOT/build.gradle"* 2>/dev/null && STACK="$STACK + Spring Boot"
elif [ -f "$ROOT/package.json" ]; then
  STACK="Node.js"
  grep -q "typescript" "$ROOT/package.json" 2>/dev/null && STACK="TypeScript + Node.js"
  grep -q "next" "$ROOT/package.json" 2>/dev/null && STACK="$STACK + Next.js"
  grep -q "react" "$ROOT/package.json" 2>/dev/null && STACK="$STACK + React"
  grep -q "express" "$ROOT/package.json" 2>/dev/null && STACK="$STACK + Express"
elif [ -f "$ROOT/Cargo.toml" ]; then
  STACK="Rust + Cargo"
elif [ -f "$ROOT/go.mod" ]; then
  STACK="Go"
elif [ -f "$ROOT/requirements.txt" ] || [ -f "$ROOT/pyproject.toml" ]; then
  STACK="Python"
fi

[ -f "$ROOT/Dockerfile" ] && STACK="$STACK + Docker"
[ -f "$ROOT/docker-compose.yml" ] || [ -f "$ROOT/docker-compose.yaml" ] && STACK="$STACK + Compose"

BUILD_CMD="?"
TEST_CMD="?"
if [ -f "$ROOT/pom.xml" ]; then
  BUILD_CMD="mvn package"; TEST_CMD="mvn test"
elif [ -f "$ROOT/build.gradle" ] || [ -f "$ROOT/build.gradle.kts" ]; then
  BUILD_CMD="./gradlew build"; TEST_CMD="./gradlew test"
elif [ -f "$ROOT/package.json" ]; then
  BUILD_CMD="npm run build"; TEST_CMD="npm test"
elif [ -f "$ROOT/Cargo.toml" ]; then
  BUILD_CMD="cargo build"; TEST_CMD="cargo test"
elif [ -f "$ROOT/go.mod" ]; then
  BUILD_CMD="go build ./..."; TEST_CMD="go test ./..."
fi

# ── Memory Bootstrap ───────────────────────────────────────────────────────
MEMORY_STATE="$ROOT/.agents/memory.state.json"
MEMORY_LOG="$ROOT/.agents/memory.log"
CONFIG="$ROOT/.agents/config.json"
SCRIPTS="$ROOT/.agents/scripts"
MEM_STATUS=""

if [ -f "$CONFIG" ] && [ -f "$SCRIPTS/extract_changes.py" ]; then
  API_KEY=$(python3 -c "import json; d=json.load(open('$CONFIG')); print(d.get('apiKey',''))" 2>/dev/null || echo "")
  SERVER_URL=$(python3 -c "import json; d=json.load(open('$CONFIG')); print(d.get('serverUrl','http://localhost:8080'))" 2>/dev/null || echo "http://localhost:8080")
  CURRENT_HEAD=$(git rev-parse HEAD 2>/dev/null || echo "")

  # Create state file on first session after project setup
  if [ ! -f "$MEMORY_STATE" ]; then
    printf '{"initialized":false,"last_indexed_commit":null,"last_indexed_at":null}\n' > "$MEMORY_STATE"
  fi

  INITIALIZED=$(python3 -c "import json; d=json.load(open('$MEMORY_STATE')); print(str(d.get('initialized', False)).lower())" 2>/dev/null || echo "false")
  LAST_HASH=$(python3 -c "import json; d=json.load(open('$MEMORY_STATE')); print(d.get('last_indexed_commit') or '')" 2>/dev/null || echo "")

  _save_mem_state() {
    MEM_STATE_FILE="$MEMORY_STATE" MEM_HASH="$CURRENT_HEAD" python3 - <<'PYEOF'
import json, datetime, os
d = {
    "initialized": True,
    "last_indexed_commit": os.environ.get("MEM_HASH", ""),
    "last_indexed_at": datetime.datetime.utcnow().isoformat() + "Z"
}
with open(os.environ["MEM_STATE_FILE"], "w") as f:
    json.dump(d, f, indent=2)
PYEOF
  }

  if [ "$INITIALIZED" = "false" ] || [ -z "$LAST_HASH" ]; then
    # Full bootstrap — never indexed or hash is missing
    (cd "$ROOT" && \
      python3 "$SCRIPTS/extract_changes.py" --all --api-key "$API_KEY" --mcp-url "$SERVER_URL" >> "$MEMORY_LOG" 2>&1 && \
      python3 "$SCRIPTS/sync-skills-to-chroma.py" >> "$MEMORY_LOG" 2>&1 \
    ) &
    disown $! 2>/dev/null || true
    _save_mem_state
    MEM_STATUS="Memory bootstrap started in background (first run). See .agents/memory.log for progress."
  else
    NEW_COUNT=$(git log "${LAST_HASH}..HEAD" --oneline 2>/dev/null | wc -l | tr -d ' ')
    if [ "${NEW_COUNT:-0}" -gt 0 ]; then
      (cd "$ROOT" && python3 "$SCRIPTS/extract_changes.py" \
        --all --api-key "$API_KEY" --mcp-url "$SERVER_URL" >> "$MEMORY_LOG" 2>&1) &
      disown $! 2>/dev/null || true
      _save_mem_state
      MEM_STATUS="Memory sync started in background ($NEW_COUNT new commit(s) since ${LAST_HASH:0:7}). See .agents/memory.log."
    else
      MEM_STATUS="Memory up to date (last indexed: ${LAST_HASH:0:7})."
    fi
  fi
fi

# ── Build context ──────────────────────────────────────────────────────────
if [ -z "$STACK" ]; then
  echo '{}'
  exit 0
fi

MEM_LINE=""
[ -n "$MEM_STATUS" ] && MEM_LINE="
## Memory Status
- ${MEM_STATUS}"

CTX="## Project Stack (auto-detected)
- Stack: ${STACK}
- Build: \`${BUILD_CMD}\`
- Test: \`${TEST_CMD}\`
${MEM_LINE}
Always query memory before implementing. Use the skill that matches the task."

python3 -c "
import json, sys
print(json.dumps({
    'hookSpecificOutput': {
        'hookEventName': 'SessionStart',
        'additionalContext': sys.stdin.read()
    }
}))
" <<< "$CTX"
