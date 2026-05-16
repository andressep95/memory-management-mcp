#!/usr/bin/env python3
"""
Post-commit extractor — dual-write to ChromaDB + Oracle MCP server.

Granularity: 1 record per file per commit.
  - Chroma : embedded semantic desc (what/why) — fast local search
  - Oracle : same data via REST batch API — persistent, VECTOR index

Run modes:
  python3 extract_changes.py              # index HEAD (post-commit hook)
  python3 extract_changes.py --ref <sha>  # index a specific commit
  python3 extract_changes.py --all        # replay full git history

Diff strategy (--all):
  1. Ask Oracle which commits are already indexed (GET /api/memory/commits)
  2. Ask Chroma which commits are already indexed
  3. Only process commits missing from each store
  Oracle is authoritative for the diff — avoids re-embedding on restart.
"""

import os
os.environ["HF_HUB_DISABLE_TELEMETRY"] = "1"
os.environ["TOKENIZERS_PARALLELISM"] = "false"
os.environ["HF_HUB_DISABLE_IMPLICIT_TOKEN"] = "1"

import warnings
warnings.filterwarnings("ignore", message=".*unauthenticated.*")
warnings.filterwarnings("ignore", message=".*HF_TOKEN.*")

import json
import re
import subprocess
import sys
from pathlib import Path

CHROMA_BATCH      = 100
MCP_BATCH_SIZE    = 10         # entries per /api/memory/batch call
MAX_HUNK_CONTENT  = 12_000

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
        if line.startswith("what:"):      what     = line[5:].strip()
        elif line.startswith("why:"):     why      = line[4:].strip()
        elif line.startswith("breaking:"): breaking = line[9:].strip().lower() == "true"
    return what, why, breaking


def parse_hunks(diff: str) -> list[dict]:
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
                "diff":        [],
            }
        elif current is not None:
            if line.startswith("+") and not line.startswith("+++"):
                current["adds"] = True
                current["diff"].append(line)
            elif line.startswith("-") and not line.startswith("---"):
                current["dels"] = True
                current["diff"].append(line)
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


# ── Oracle MCP REST helpers ──────────────────────────────────────────────────

def oracle_get_indexed_commits(project_id: str, base_url: str) -> set[str]:
    """Returns set of commit hashes already indexed in Oracle."""
    try:
        import urllib.request
        url = f"{base_url}/api/memory/commits?projectId={project_id}"
        with urllib.request.urlopen(url, timeout=10) as resp:
            return set(json.loads(resp.read()))
    except Exception as e:
        print(f"[oracle] Warning: could not fetch indexed commits: {e}", file=sys.stderr)
        return set()


def oracle_batch_push(project_id: str, entries: list[dict], base_url: str) -> tuple[int, int]:
    """POST a batch of entries to /api/memory/batch. Returns (inserted, skipped)."""
    if not entries:
        return 0, 0
    try:
        import urllib.request
        payload = json.dumps({"projectId": project_id, "entries": entries}).encode()
        req = urllib.request.Request(
            f"{base_url}/api/memory/batch",
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=60) as resp:
            body = json.loads(resp.read())
            return body.get("inserted", 0), body.get("skipped", 0)
    except Exception as e:
        print(f"[oracle] Batch push failed: {e}", file=sys.stderr)
        return 0, 0


def oracle_register_user(git_username: str, base_url: str) -> str | None:
    """Register or get user, returns userId."""
    try:
        import urllib.request
        payload = json.dumps({"gitUsername": git_username}).encode()
        req = urllib.request.Request(
            f"{base_url}/api/users/register",
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read())["userId"]
    except Exception as e:
        print(f"[oracle] Could not register user: {e}", file=sys.stderr)
        return None


def oracle_get_or_create_project(name: str, user_id: str, base_url: str) -> str | None:
    """Get or create project, returns projectId."""
    try:
        import urllib.request
        payload = json.dumps({
            "name": name, "description": f"Git project: {name}", "createdBy": user_id
        }).encode()
        req = urllib.request.Request(
            f"{base_url}/api/projects",
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read())["projectId"]
    except Exception as e:
        print(f"[oracle] Could not resolve project: {e}", file=sys.stderr)
        return None


# ── Core commit processing ───────────────────────────────────────────────────

def extract_commit_data(ref: str, project: str) -> tuple[str, list[dict]]:
    """
    Extract all file-level change data for a commit.
    Returns (short_hash, list_of_file_entries).
    Does NOT write to any store — caller decides where to write.
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
        parent = "4b825dc642cb6eb9a060e54bf8d69288fbee4904"

    all_files = [f for f in run(f"git diff-tree --no-commit-id -r --name-only {ref}").splitlines() if f]

    entries = []
    for file in all_files:
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
                "diff": [],
            }]

        ctype = overall_change_type(hunks)
        tags  = [t for t in [commit_type, ctype, kind, scope] if t]
        sdesc = semantic_desc(what, why, intent, file)
        hunk_content = diff_body[:MAX_HUNK_CONTENT]

        entries.append({
            "commit":      commit,
            "author":      author,
            "ts":          ts,
            "intent":      intent,
            "branch":      branch,
            "what":        what,
            "why":         why,
            "commit_type": commit_type,
            "scope":       scope,
            "file":        file,
            "file_kind":   kind,
            "language":    lang,
            "change_type": ctype,
            "tags":        tags,
            "breaking":    breaking,
            "sdesc":       sdesc,
            "hunk_content": hunk_content,
            "hunks":       hunks,
        })

    return commit, entries


def push_to_chroma(entries: list[dict], existing: set[str], collection) -> int:
    ids, documents, metadatas = [], [], []
    for e in entries:
        rid = f"{e['commit']}:{e['file']}"
        if rid in existing:
            continue
        ids.append(rid)
        documents.append(e["sdesc"])
        metadatas.append({
            "file":         e["file"],
            "file_kind":    e["file_kind"],
            "language":     e["language"],
            "what":         e["what"],
            "why":          e["why"],
            "intent":       e["intent"],
            "commit_type":  e["commit_type"],
            "scope":        e["scope"],
            "change_type":  e["change_type"],
            "commit":       e["commit"],
            "branch":       e["branch"],
            "project":      e.get("project", ""),
            "ts":           e["ts"],
            "author":       e["author"],
            "tags":         ",".join(e["tags"]),
            "hunks_count":  len(e["hunks"]),
            "hunk_content": e["hunk_content"],
            "breaking":     str(e["breaking"]).lower(),
        })

    if not ids:
        return 0

    for i in range(0, len(ids), CHROMA_BATCH):
        collection.upsert(
            ids=ids[i:i + CHROMA_BATCH],
            documents=documents[i:i + CHROMA_BATCH],
            metadatas=metadatas[i:i + CHROMA_BATCH],
        )
    return len(ids)


def to_oracle_entry(e: dict) -> dict:
    return {
        "commitHash": e["commit"],
        "branch":     e["branch"],
        "author":     e["author"],
        "filePath":   e["file"],
        "intent":     e["commit_type"] or None,
        "what":       e["what"] or e["intent"],
        "why":        e["why"] or None,
        "language":   e["language"],
        "tags":       e["tags"],
        "hunks": [
            {
                "linesStart": h["lines_start"],
                "linesEnd":   h["lines_end"],
                "symbol":     h.get("symbol", ""),
                "changeType": ("addition" if h["adds"] and not h["dels"]
                               else "deletion" if h["dels"] and not h["adds"]
                               else "modification"),
                "hunkDiff":   "\n".join(h.get("diff", [])),
            }
            for h in e["hunks"]
        ],
    }


# ── Main ─────────────────────────────────────────────────────────────────────

def main() -> None:
    import argparse, io, contextlib

    p = argparse.ArgumentParser()
    p.add_argument("--ref",        default="HEAD")
    p.add_argument("--all",        action="store_true")
    p.add_argument("--chroma",     default=".agents/memory/chroma")
    p.add_argument("--collection", default="changes")
    p.add_argument("--mcp-url",    default="http://localhost:8080",
                   help="Base URL of the MCP Spring Boot server")
    p.add_argument("--git-user",   default="",
                   help="Git username for Oracle registration (defaults to git config)")
    p.add_argument("--no-oracle",  action="store_true", help="Skip Oracle writes")
    p.add_argument("--no-chroma",  action="store_true", help="Skip Chroma writes")
    args = p.parse_args()

    # ── Chroma setup ──────────────────────────────────────────────────────
    collection = None
    if not args.no_chroma:
        try:
            import chromadb
            from chromadb.utils import embedding_functions
        except ImportError:
            print("ERROR: pip install chromadb", file=sys.stderr)
            sys.exit(1)
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

    project_name = Path(run("git rev-parse --show-toplevel")).name

    # ── Oracle setup ──────────────────────────────────────────────────────
    oracle_project_id = None
    if not args.no_oracle:
        git_user = args.git_user or run("git config user.name") or "unknown"
        user_id  = oracle_register_user(git_user, args.mcp_url)
        if user_id:
            oracle_project_id = oracle_get_or_create_project(project_name, user_id, args.mcp_url)
        if not oracle_project_id:
            print("[oracle] Could not resolve project — Oracle writes disabled.", file=sys.stderr)

    # ── Indexed sets for diff ─────────────────────────────────────────────
    oracle_indexed: set[str] = set()
    if oracle_project_id:
        print("[oracle] Fetching already-indexed commits...")
        oracle_indexed = oracle_get_indexed_commits(oracle_project_id, args.mcp_url)
        print(f"[oracle] {len(oracle_indexed)} commits already indexed.")

    # ── Process commits ───────────────────────────────────────────────────
    if args.all:
        commits = [c for c in run("git log --reverse --format=%H").splitlines() if c]
        total   = len(commits)
        chroma_total = oracle_total = 0
        oracle_pending: list[dict] = []

        for i, full_hash in enumerate(commits, 1):
            short = full_hash[:7]
            print(f"  [{i}/{total}] {short}", end="\r", flush=True)

            commit_hash, entries = extract_commit_data(full_hash, project_name)
            if not entries:
                continue

            # Chroma
            if collection is not None:
                chroma_existing: set[str] = set()
                if collection.count() > 0:
                    chroma_existing = set(collection.get(
                        where={"commit": commit_hash}, include=[])["ids"])
                for e in entries:
                    e["project"] = project_name
                chroma_total += push_to_chroma(entries, chroma_existing, collection)

            # Oracle — accumulate if not already indexed
            if oracle_project_id and commit_hash not in oracle_indexed:
                oracle_pending.extend(to_oracle_entry(e) for e in entries)

            # Flush Oracle in real chunks of MCP_BATCH_SIZE (not all at once)
            while oracle_project_id and len(oracle_pending) >= MCP_BATCH_SIZE:
                batch = oracle_pending[:MCP_BATCH_SIZE]
                del oracle_pending[:MCP_BATCH_SIZE]
                ins, _ = oracle_batch_push(oracle_project_id, batch, args.mcp_url)
                oracle_total += ins

        # Final Oracle flush — drain any remaining entries
        while oracle_pending and oracle_project_id:
            batch = oracle_pending[:MCP_BATCH_SIZE]
            del oracle_pending[:MCP_BATCH_SIZE]
            ins, _ = oracle_batch_push(oracle_project_id, batch, args.mcp_url)
            oracle_total += ins

        print(f"\n[memory] Done. {total} commits processed.")
        if collection is not None:
            print(f"  Chroma: {chroma_total} file(s) indexed.")
        if oracle_project_id:
            print(f"  Oracle: {oracle_total} file(s) indexed.")

    else:
        commit_hash, entries = extract_commit_data(args.ref, project_name)
        if not entries:
            print(f"[memory] No files changed in {commit_hash}.")
            return

        chroma_n = oracle_n = 0

        if collection is not None:
            chroma_existing: set[str] = set()
            if collection.count() > 0:
                chroma_existing = set(collection.get(
                    where={"commit": commit_hash}, include=[])["ids"])
            for e in entries:
                e["project"] = project_name
            chroma_n = push_to_chroma(entries, chroma_existing, collection)

        if oracle_project_id and commit_hash not in oracle_indexed:
            oracle_entries = [to_oracle_entry(e) for e in entries]
            ins, _ = oracle_batch_push(oracle_project_id, oracle_entries, args.mcp_url)
            oracle_n = ins

        parts = []
        if collection is not None:
            parts.append(f"Chroma={chroma_n}")
        if oracle_project_id:
            parts.append(f"Oracle={oracle_n}")
        print(f"[memory] {commit_hash}: {', '.join(parts)} file(s) indexed.")


if __name__ == "__main__":
    main()
