#!/usr/bin/env python3
"""Validates compat/compat.json (CLAUDE.md §13). Exit code 1 on any problem."""
import datetime
import json
import os
import re
import sys

STATUSES = {
    "Playable", "Playable with issues", "In-game", "Intro only",
    "Boots but does not progress", "Does not boot",
}
REQUIRED = ["title_id", "name", "status", "device", "driver", "build", "date", "tester", "notes", "evidence"]


def validate(path, repo_root):
    errors = []
    try:
        with open(path, encoding="utf-8") as f:
            db = json.load(f)
    except (OSError, ValueError) as e:
        return [f"cannot read {path}: {e}"]
    if not isinstance(db, dict) or db.get("schema") != 1 or not isinstance(db.get("entries"), list):
        return ['top level must be {"schema": 1, "entries": [...]}']
    for i, e in enumerate(db["entries"]):
        where = f"entry {i}"
        if not isinstance(e, dict):
            errors.append(f"{where}: not an object")
            continue
        for k in REQUIRED:
            if not isinstance(e.get(k), str) or not e[k].strip():
                errors.append(f"{where}: missing or empty '{k}'")
        extra = set(e) - set(REQUIRED)
        if extra:
            errors.append(f"{where}: unknown keys {sorted(extra)}")
        if isinstance(e.get("title_id"), str) and not re.fullmatch(r"[0-9A-F]{8}", e["title_id"]):
            errors.append(f"{where}: title_id must be 8 uppercase hex digits")
        if isinstance(e.get("status"), str) and e["status"] not in STATUSES:
            errors.append(f"{where}: status '{e['status']}' is not one of {sorted(STATUSES)}")
        if isinstance(e.get("build"), str) and not re.fullmatch(r"[0-9a-f]{7,40}", e["build"]):
            errors.append(f"{where}: build must be a git commit hash")
        if isinstance(e.get("date"), str):
            try:
                datetime.date.fromisoformat(e["date"])
            except ValueError:
                errors.append(f"{where}: date must be YYYY-MM-DD")
        ev = e.get("evidence")
        if isinstance(ev, str) and ev.strip() and not ev.startswith("https://"):
            full = os.path.normpath(os.path.join(repo_root, ev))
            if not full.startswith(os.path.join(repo_root, "compat", "evidence") + os.sep):
                errors.append(f"{where}: evidence must be an https link or a file under compat/evidence/")
            elif not os.path.isfile(full):
                errors.append(f"{where}: evidence file '{ev}' does not exist")
    return errors


def self_test():
    import tempfile
    with tempfile.TemporaryDirectory() as root:
        os.makedirs(os.path.join(root, "compat", "evidence"))
        open(os.path.join(root, "compat", "evidence", "a.txt"), "w").close()
        good = {"title_id": "4D5307E6", "name": "n", "status": "Intro only", "device": "d", "driver": "v",
                "build": "abc1234", "date": "2026-11-18", "tester": "t", "notes": "x",
                "evidence": "compat/evidence/a.txt"}

        def run(entries):
            p = os.path.join(root, "c.json")
            with open(p, "w") as f:
                json.dump({"schema": 1, "entries": entries}, f)
            return validate(p, root)

        assert run([]) == []
        assert run([good]) == [], run([good])
        for key, bad in [("status", "Untested"), ("status", "Perfect"), ("title_id", "xyz"),
                         ("evidence", ""), ("evidence", "compat/evidence/missing.txt"),
                         ("evidence", "../secret"), ("date", "18/11/2026"), ("build", "latest")]:
            assert run([dict(good, **{key: bad})]), (key, bad)
        assert run([dict(good, fps="60")])
    print("validate-compat self-test ok")


if __name__ == "__main__":
    if sys.argv[1:] == ["--self-test"]:
        self_test()
        sys.exit(0)
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    problems = validate(os.path.join(root, "compat", "compat.json"), root)
    for p in problems:
        print("compat.json:", p)
    print("compat.json: ok" if not problems else f"compat.json: {len(problems)} problem(s)")
    sys.exit(1 if problems else 0)
