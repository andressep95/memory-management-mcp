#!/usr/bin/env bash
# validate-setup.sh — Runtime gate for setup integrity
# Exit 0 = OK, Exit 1 = incomplete (stdout = fix instructions for agent)

set -uo pipefail

ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
FAILURES=""

[ ! -f "$ROOT/.agents/config.json" ] && \
  FAILURES="$FAILURES\n- MISSING: .agents/config.json → re-run setupProject"

if [ ! -f "$ROOT/.git/hooks/post-commit" ]; then
  FAILURES="$FAILURES\n- MISSING: .git/hooks/post-commit → fix: cp .agents/scripts/post-commit .git/hooks/post-commit && chmod +x .git/hooks/post-commit"
elif [ ! -x "$ROOT/.git/hooks/post-commit" ]; then
  FAILURES="$FAILURES\n- NOT EXECUTABLE: .git/hooks/post-commit → fix: chmod +x .git/hooks/post-commit"
fi

for script in session-start.sh post-commit extract_changes.py validate-commit.sh; do
  if [ -f "$ROOT/.agents/scripts/$script" ] && [ ! -x "$ROOT/.agents/scripts/$script" ]; then
    FAILURES="$FAILURES\n- NOT EXECUTABLE: .agents/scripts/$script → fix: chmod +x .agents/scripts/$script"
  fi
done

[ ! -L "$ROOT/CLAUDE.md" ] && \
  FAILURES="$FAILURES\n- MISSING SYMLINK: CLAUDE.md → fix: ln -sf .agents/rules.md CLAUDE.md"
[ ! -L "$ROOT/AGENTS.md" ] && \
  FAILURES="$FAILURES\n- MISSING SYMLINK: AGENTS.md → fix: ln -sf .agents/rules.md AGENTS.md"
[ ! -L "$ROOT/.kiro/steering/project-rules.md" ] && \
  FAILURES="$FAILURES\n- MISSING SYMLINK: .kiro/steering/project-rules.md → fix: ln -sf ../../.agents/rules.md .kiro/steering/project-rules.md"
[ ! -L "$ROOT/.kiro/skills/commit/SKILL.md" ] && \
  FAILURES="$FAILURES\n- MISSING SYMLINK: .kiro/skills/commit/SKILL.md → fix: mkdir -p .kiro/skills/commit && ln -sf ../../../.agents/skills/commit/SKILL.md .kiro/skills/commit/SKILL.md"

[ ! -f "$ROOT/.agents/skills/commit/SKILL.md" ] && \
  FAILURES="$FAILURES\n- MISSING: .agents/skills/commit/SKILL.md → re-run setupProject"

[ ! -f "$ROOT/.agents/memory.state.json" ] && \
  FAILURES="$FAILURES\n- MISSING: .agents/memory.state.json"

if [ -n "$FAILURES" ]; then
  printf "⚠️ SETUP INCOMPLETE — apply these fixes before continuing:%b\n" "$FAILURES"
  exit 1
fi

exit 0
