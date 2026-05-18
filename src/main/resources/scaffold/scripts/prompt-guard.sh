#!/usr/bin/env bash
# prompt-guard.sh — PreToolUse hook for Claude Code
#
# Fires before Bash/Write/Edit/MultiEdit.
# Emits a reminder if querySkills/queryMemory haven't been called yet this session.
# Uses a temp marker file to avoid repeating after the first tool call.

set -uo pipefail
cat > /dev/null  # drain stdin

MARKER="/tmp/.mcp-guard-$$-$(date +%Y%m%d)"
ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
CONFIG="$ROOT/.agents/config.json"

# If marker exists, agent already got the reminder — stay silent
if [ -f "$MARKER" ]; then
  echo '{}'
  exit 0
fi

# Create marker so we only remind once per session
touch "$MARKER"

# Check if config exists
if [ ! -f "$CONFIG" ]; then
  echo '{}'
  exit 0
fi

PROJECT_ID=$(python3 -c "import json; print(json.load(open('$CONFIG')).get('projectId',''))" 2>/dev/null || echo "")

if [ -z "$PROJECT_ID" ]; then
  echo '{}'
  exit 0
fi

# Emit reminder as hook output
python3 -c "
import json, sys
msg = '''⚠️  MANDATORY: Before proceeding, you MUST call:
1. querySkills(\"<your task>\", \"${PROJECT_ID}\", 5)
2. queryMemory(\"<your task>\", \"${PROJECT_ID}\", 5)

Do NOT write code or execute commands until you have reviewed the results.'''
print(json.dumps({
    'hookSpecificOutput': {
        'hookEventName': 'PreToolUse',
        'additionalContext': msg
    }
}))
"
