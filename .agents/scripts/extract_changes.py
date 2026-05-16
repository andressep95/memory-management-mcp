#!/usr/bin/env python3
"""
Post-commit extractor — indexes each modified file directly into ChromaDB.

Granularity: 1 record per file per commit (not per hunk).
  - document  : semantic description (what/why) — this is what gets embedded
  - hunk_content : raw diff text for the file — stored as metadata, not embedded
  - hunks_count  : number of @@ blocks in the file change

Run modes:
  python3 extract_changes.py              # index HEAD (post-commit hook)
  python3 extract_changes.py --ref <sha>  # index a specific commit
  python3 extract_changes.py --all        # replay full git history (model loaded once)
"""

import os
os.environ["HF_HUB_DISABLE_TELEMETRY"] = "1"
os.environ["TOKENIZERS_PARALLELISM"] = "false"
os.environ["HF_HUB_DISABLE_IMPLICIT_TOKEN"] = "1"

import warnings
warnings.filterwarnings("ignore", message=".*unauthenticated.*")
warnings.filterwarnings("ignore", message=".*HF_TOKEN.*")

import re
import subprocess
import sys
from pathlib import Path

BATCH             = 100
MAX_HUNK_CONTENT  = 12_000  # chars stored in Chroma metadata (keep metadata lean)

LANGUAGE_MAP = {
    ".java": "java", ".kt": "kotlin", ".scala": "scala",
    ".py": "python", ".go": "go", ".rs": "rust",
    ".ts": "typescript", ".tsx": "typescript",
    ".js": "javascript", ".jsx": "javascript",
    ".cs": "csharp", ".rb": "ruby", ".php": "php",
    ".c": "c", ".cpp": "cpp", ".h": "c",
    ".sh": "shell", ".bash": "shell", ".zsh": "shell",
    ".yaml": "yaml", ".yml": "yaml",
    ".json": "json", ".toml": "toml",
    ".sql": "sql", ".md": "markdown",
    ".css": "css", ".scss": "scss",
}


def run(cmd: str) -> str:
    return subprocess.run(cmd, shell=True, capture_output=True, text=True).stdout.strip()


def detect_language(path: str) -> str:
    return LANGUAGE_MAP.get(Path(path).suffix.lower(), "other")


def file_kind(path: str) -> str:
    ext  = Path(path).suffix.lower()
    name = Path(path).stem.lower()
    if ext in {".sh", ".bash", ".zsh"}:
        return "script"
    if ext in {".py", ".go", ".ts", ".tsx", ".js", ".jsx", ".rs",
               ".java", ".kt", ".c", ".cpp", ".rb", ".cs"}:
        return "test" if "test" in name or "spec" in name else "source"
    if ext in {".json", ".yaml", ".yml", ".toml", ".env", ".ini", ".cfg"}:
        return "config"
    if ext in {".md", ".txt", ".rst", ".adoc"}:
        return "doc"
    if ext in {".css", ".scss", ".sass", ".less"}:
        return "style"
    return "other"


def parse_commit_parts(intent: str) -> tuple[str, str]:
    m = re.match(r"^(\w+)(?:\(([\w/.-]+)\))?:", intent)
    return (m.group(1) if m else "", m.group(2) if m and m.group(2) else "")


def parse_body(body: str) -> tuple[str, str, bool]:
    what, why, breaking = "", "", False
    for line in body.splitlines():
        if line.startswith("what:"):     what     = line[5:].strip()
        elif line.startswith("why:"):    why      = line[4:].strip()
        elif line.startswith("breaking:"): breaking = line[9:].strip().lower() == "true"
    return what, why, breaking


def parse_hunks(diff: str) -> list[dict]:
    """Parse @@ blocks into a list of hunk metadata dicts."""
    hunks, current = [], None
    for line in diff.splitlines():
        if line.startswith("@@"):
            if current:
                hunks.append(current)
            m = re.match(r"@@\s+-\d+(?:,\d+)?\s+\+(\d+)(?:,(\d+))?\s+@@(.*)", line)
            if not m:
                continue
            start = int(m.group(1))
            count = int(m.group(2)) if m.group(2) is not None else 1
            current = {
                "symbol":      m.group(3).strip(),
                "lines_start": start,
                "lines_end":   start + max(count - 1, 0),
                "adds":        False,
                "dels":        False,
            }
        elif current is not None:
            if line.startswith("+") and not line.startswith("+++"):
                current["adds"] = True
            elif line.startswith("-") and not line.startswith("---"):
                current["dels"] = True
    if current:
        hunks.append(current)
    return hunks


def overall_change_type(hunks: list[dict]) -> str:
    has_adds = any(h["adds"] for h in hunks)
    has_dels = any(h["dels"] for h in hunks)
    if has_adds and has_dels:
        return "modification"
    return "addition" if has_adds else "deletion"


def semantic_desc(what: str, why: str, intent: str, file: str) -> str:
    if what and why:
        return f"{what} — {why}"
    if what:
        return f"{what} ({file})"
    if why:
        return f"{intent} — {why}"
    return f"{intent} ({file})"


def resolve_branch(ref: str) -> str:
    if ref == "HEAD":
        return run("git rev-parse --abbrev-ref HEAD")
    raw = run(f"git branch --contains {ref} --format='%(refname:short)' 2>/dev/null")
    candidates = [b.strip().strip("'") for b in raw.splitlines() if b.strip()]
    main_branch = [b for b in candidates if b in ("main", "master")]
    return main_branch[0] if main_branch else (candidates[0] if candidates else "unknown")


def process_commit(ref: str, collection, project: str) -> int:
    """
    Index all file-level changes for one commit into Chroma.
    Returns the number of new records upserted.
    """
    commit = run(f"git log -1 --format=%h {ref}")
    author = run(f"git log -1 --format=%an {ref}")
    ts     = run(f"git log -1 --format=%cI {ref}")[:10]
    intent = run(f"git log -1 --format=%s {ref}")
    body   = run(f"git log -1 --format=%b {ref}")
    branch = resolve_branch(ref)

    what, why, breaking = parse_body(body)
    commit_type, scope  = parse_commit_parts(intent)

    parent = run(f"git rev-parse --verify {ref}~1 2>/dev/null")
    if not parent:
        parent = "4b825dc642cb6eb9a060e54bf8d69288fbee4904"  # empty tree

    all_files = [f for f in run(f"git diff-tree --no-commit-id -r --name-only {ref}").splitlines() if f]
    if not all_files:
        return 0

    # Pre-fetch existing IDs for this commit to skip duplicates
    existing: set[str] = set()
    if collection.count() > 0:
        existing = set(collection.get(where={"commit": commit}, include=[])["ids"])

    ids, documents, metadatas = [], [], []

    for file in all_files:
        rid = f"{commit}:{file}"
        if rid in existing:
            continue

        kind = file_kind(file)
        lang = detect_language(file)

        diff = (run(f'git diff {parent} {ref} -- "{file}" 2>/dev/null')
                or run(f'git show {ref} -- "{file}"'))

        lines    = diff.splitlines()
        start_ix = next((i for i, l in enumerate(lines) if l.startswith("@@")), None)
        diff_body = "\n".join(lines[start_ix:]) if start_ix is not None else ""

        hunks = parse_hunks(diff_body)
        if not hunks:
            hunks = [{
                "symbol": "", "lines_start": 1, "lines_end": 1,
                "adds": any(l.startswith("+") for l in lines),
                "dels": any(l.startswith("-") for l in lines),
            }]

        ctype    = overall_change_type(hunks)
        tags     = [t for t in [commit_type, ctype, kind, scope] if t]
        sdesc    = semantic_desc(what, why, intent, file)

        # Truncate diff for Chroma metadata; full content stored in Oracle via Java service
        hunk_content = diff_body if len(diff_body) <= MAX_HUNK_CONTENT else diff_body[:MAX_HUNK_CONTENT]

        ids.append(rid)
        documents.append(sdesc)
        metadatas.append({
            "file":         file,
            "file_kind":    kind,
            "language":     lang,
            "what":         what,
            "why":          why,
            "intent":       intent,
            "commit_type":  commit_type,
            "scope":        scope,
            "change_type":  ctype,
            "commit":       commit,
            "branch":       branch,
            "project":      project,
            "ts":           ts,
            "author":       author,
            "tags":         ",".join(tags),
            "hunks_count":  len(hunks),
            "hunk_content": hunk_content,
            "breaking":     str(breaking).lower(),
        })

    if not ids:
        return 0

    for i in range(0, len(ids), BATCH):
        collection.upsert(
            ids=ids[i:i + BATCH],
            documents=documents[i:i + BATCH],
            metadatas=metadatas[i:i + BATCH],
        )

    return len(ids)


def main() -> None:
    import argparse

    p = argparse.ArgumentParser()
    p.add_argument("--ref",        default="HEAD", help="Commit ref to index (default: HEAD)")
    p.add_argument("--all",        action="store_true", help="Replay full git history")
    p.add_argument("--chroma",     default=".agents/memory/chroma")
    p.add_argument("--collection", default="changes")
    args = p.parse_args()

    try:
        import chromadb
        from chromadb.utils import embedding_functions
    except ImportError:
        print("ERROR: pip install chromadb", file=sys.stderr)
        sys.exit(1)

    import io, contextlib
    client = chromadb.PersistentClient(path=args.chroma)
    with contextlib.redirect_stderr(io.StringIO()):
        ef = embedding_functions.SentenceTransformerEmbeddingFunction(
            model_name="intfloat/multilingual-e5-small"
        )
    collection = client.get_or_create_collection(
        name=args.collection,
        embedding_function=ef,
        metadata={"hnsw:space": "cosine"},
    )

    project = Path(run("git rev-parse --show-toplevel")).name

    if args.all:
        commits = [c for c in run("git log --reverse --format=%H").splitlines() if c]
        total   = len(commits)
        indexed = 0
        for i, full_hash in enumerate(commits, 1):
            short = full_hash[:7]
            print(f"  [{i}/{total}] {short}", end="\r", flush=True)
            indexed += process_commit(full_hash, collection, project)
        print(f"\n[memory] Done. {total} commits processed, {indexed} file(s) indexed.")
    else:
        n = process_commit(args.ref, collection, project)
        commit = run(f"git log -1 --format=%h {args.ref}")
        print(f"[memory] {n} file(s) → Chroma ({commit})")


if __name__ == "__main__":
    main()
