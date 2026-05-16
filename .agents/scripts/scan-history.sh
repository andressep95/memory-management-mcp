#!/usr/bin/env bash
# Replays full git history into Chroma.
# Delegates to extract_changes.py --all, which loads the embedding model once
# and processes every commit in chronological order.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$ROOT"

total=$(git log --oneline | wc -l | tr -d ' ')
echo "Replaying $total commits into Chroma..."

python3 "$SCRIPT_DIR/extract_changes.py" --all
