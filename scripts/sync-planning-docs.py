#!/usr/bin/env python3
import subprocess, json, os, sys, datetime, glob

ALWAYS_UPDATE = [
    ".planning/JOURNAL.md",
    ".planning/STATE.md",
]

CONDITIONAL = [
    ".planning/ROADMAP.md",
    ".planning/MILESTONES.md",
    ".planning/codebase/ARCHITECTURE.md",
    ".planning/codebase/STACK.md",
]

def get_commit_info():
    def git(*args):
        return subprocess.check_output(["git"] + list(args), text=True).strip()
    return {
        "hash":    git("rev-parse", "--short", "HEAD"),
        "message": git("log", "-1", "--format=%s"),
        "author":  git("log", "-1", "--format=%an"),
        "date":    datetime.datetime.now().strftime("%Y-%m-%d %H:%M"),
        "files":   git("diff-tree", "--no-commit-id", "-r", "--name-only", "HEAD").splitlines()
    }

def find_active_phase_files():
    """Find .continue-here, SUMMARY, VERIFICATION files in the most recent active phase."""
    results = []
    for pattern in [
        ".planning/phases/*/.continue-here.md",
        ".planning/phases/*/*-SUMMARY.md",
        ".planning/phases/*/*-VERIFICATION.md",
    ]:
        results.extend(glob.glob(pattern))
    return results

def read_file(path):
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            return f.read()
    return ""

def write_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)

def build_prompt(commit, file_contents):
    files_block = "\n\n".join(
        f"=== {path} ===\n{content}"
        for path, content in file_contents.items()
    )

    changed = "\n".join(f"  - {f}" for f in commit["files"])

    return f"""A git commit was just made in the Militopia Java/libGDX game project.

Commit hash: {commit['hash']}
Commit message: {commit['message']}
Date: {commit['date']}
Changed files:
{changed}

Below are the current contents of .planning/ files that may need updating.
Review each one and update ONLY the files that are relevant to this commit.

Rules:
- JOURNAL.md: ALWAYS append a new entry in this format:
  ---
  ### {commit['date']} — `{commit['hash']}`
  **Commit:** {commit['message']}
  **Changed:**
  - (list changed files)
- STATE.md: ALWAYS update last_updated date, Last Session Summary, In-Progress Work, Next Steps. Add/update a "## Cleanup Suggestions" section flagging any .planning/ files that look stale.
- ROADMAP.md: Update ONLY if a phase was completed or a new phase started.
- MILESTONES.md: Update ONLY if a meaningful milestone was reached.
- codebase/ARCHITECTURE.md: Update ONLY if new ECS systems, components, or major patterns were added.
- codebase/STACK.md: Update ONLY if build.gradle or gradle.properties was changed.
- Phase files (.continue-here, SUMMARY, VERIFICATION): Update ONLY if this commit affects that phase.

Respond with a single valid JSON object. Keys are file paths (exactly as shown below), values are the COMPLETE updated file contents. Only include files that actually changed. No commentary, no markdown fences — just raw JSON.

{files_block}
"""

def main():
    try:
        commit = get_commit_info()
    except Exception as e:
        print(f"[sync-planning-docs] Could not get commit info: {e}", file=sys.stderr)
        sys.exit(0)

    candidates = ALWAYS_UPDATE + CONDITIONAL + find_active_phase_files()
    file_contents = {p: read_file(p) for p in candidates}

    prompt = build_prompt(commit, file_contents)

    try:
        result = subprocess.run(
            ["claude", "--print", prompt],
            capture_output=True, text=True, timeout=120
        )
        raw = result.stdout.strip()
    except Exception as e:
        print(f"[sync-planning-docs] claude --print failed: {e}", file=sys.stderr)
        sys.exit(0)

    # Claude sometimes wraps output in ```json ... ``` — strip it
    if raw.startswith("```"):
        raw = "\n".join(raw.splitlines()[1:])
        if raw.endswith("```"):
            raw = raw[:-3].strip()

    try:
        updates = json.loads(raw)
    except json.JSONDecodeError as e:
        print(f"[sync-planning-docs] Failed to parse JSON response: {e}", file=sys.stderr)
        sys.exit(0)

    for path, content in updates.items():
        if path.startswith(".planning/"):
            write_file(path, content)
            print(f"[sync-planning-docs] Updated {path}")

if __name__ == "__main__":
    main()
