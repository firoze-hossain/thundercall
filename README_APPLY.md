# Round 16 — Postman Collection (.json) import

FRONTEND ONLY: 1 file. No backend change, no new dependency (uses the
org.json library already in the project — same one that powers your
scripts and headers).

| File | Destination |
|---|---|
| MainController.java | .../ui/controllers/ |

## What it does
File → Import now handles a real Postman v2.1 collection export end to end:

- **Always creates a NEW collection** named after the file's `info.name`
  (auto-suffixed "(2)", "(3)"... if that name already exists in the
  current workspace) — importing never silently merges into or overwrites
  an existing collection.
- **Nested folders at any depth** — Postman "item groups" become your
  nested folders, recursively.
- **Every request**: method, URL (handles both the plain-string and the
  `{"raw": "..."}` url formats Postman uses), enabled headers (disabled
  ones are skipped), and body — `raw` bodies (JSON/text/xml) come through
  as-is; `urlencoded`/`form-data` bodies are rebuilt as `key=value&...`
  (file-upload fields can't be reproduced and are counted as skipped).
- **Scripts**: Postman's `event` blocks are mapped — `listen: "prerequest"`
  → your Pre-request Scripts box, `listen: "test"` → your Tests box —
  using your existing ScriptRunner, so `pm.environment.set(...)` etc. keep
  working immediately after import.
- **Auth**: Bearer and Basic auth blocks map straight onto the
  Authorization tab you got last round (including `{{variable}}` values,
  since Postman uses the same `{{...}}` syntax). A request with no auth of
  its own inherits the collection-level auth block, if the file has one.
  (API key / OAuth2 blocks aren't modeled yet — flagged as a gap below.)
- **Collection variables**: if the file defines `variable: [...]`, a new
  Environment is created with the same name and those key/values, so
  `{{variables}}` used throughout the import resolve immediately instead
  of showing as unresolved.
- A summary alert reports exactly what happened: requests imported,
  folders created, variables imported, and anything skipped.

## Known gaps (flagged honestly, not silently dropped)
- API key and OAuth 2.0 auth blocks aren't mapped yet (only Bearer/Basic) —
  those requests import with no auth rather than a guessed wrong one.
- File-upload form fields can't be reproduced (no file to attach) — counted
  and reported as "skipped" rather than silently vanishing.
- Postman's own `{{variable}}` syntax matches ours exactly, so no
  translation was needed there.

## Test
1. Export any real collection from Postman (Collection → ... → Export →
   Collection v2.1).
2. Rebuild frontend, run, File → Import → pick that `.json`.
3. A new collection appears with the same name (or "(2)" if you already
   had one) — folders nested exactly as in Postman, every request with
   its method/URL/headers/body, scripts in the Scripts tab, auth in the
   Authorization tab, and — if the collection had variables — a new
   Environment holding them. Send a request that uses one of those
   variables and it resolves correctly.
