# Compatibility database

`compat.json` records what was actually observed when a game was tried. It starts empty:
every title is **Untested** until someone adds an entry with evidence (CLAUDE.md §13, §20).

## Entry format

```json
{
  "title_id": "4D5307E6",
  "name": "Game name as shown on the box",
  "status": "Intro only",
  "device": "Phone model (SoC)",
  "driver": "GPU driver version from the Device tab",
  "build": "git commit of the APK (About tab)",
  "date": "2026-11-18",
  "tester": "GitHub user name",
  "notes": "What happened, in a sentence or two.",
  "evidence": "compat/evidence/4D5307E6-2026-11-18.txt"
}
```

- `title_id`: 8 hex digits. `status`: one of Playable, Playable with issues, In-game,
  Intro only, Boots but does not progress, Does not boot. ("Untested" is the absence of an
  entry, so it is not a valid status here.)
- `evidence` is required: a path in this repo (diagnostics report, log or screenshot under
  `compat/evidence/`) or an https link. No evidence, no entry.
- Several entries per title are fine (different devices or builds).
- Don't include copyrighted game files, keys or firmware in evidence.

CI runs `scripts/validate-compat.py` and fails on a malformed or evidence-free entry.
The app does not read this file yet; the Library shows every game as Untested.
