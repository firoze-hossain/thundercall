<div align="center">

# 📜 Thundercall — Changelog

**A running record of feature work as it lands, newest first.**

</div>

---

## What this file is

Thundercall is built feature by feature rather than in big-bang
releases. Each entry below documents one unit of work the same way a
proper pull request description would: what changed, why it was built
that way, and — critically — how to actually verify it works, not
just that it compiles. If you're deciding whether to pull the latest
`main` into something you depend on, this is the file that tells you
what you're actually getting.

Entries are grouped by feature area and dated. "Where these go" lists
every file touched, in case you're applying work as a patch rather
than a plain `git pull`.

---

## 2026-08-14 — README: screenshots section + project status

### Where these go
```
README.md                       (modified)
docs/screenshots/README.md      (new)
```
Docs only — nothing to build or rebuild.

### What it does
- Added a **📌 Project status** section right after the intro, stating
  plainly where the project actually stands (pre-1.0, core workflows
  daily-driver-stable, newer conveniences still landing) instead of
  leaving a first-time visitor to guess from a badge.
- Added a **📸 Screenshots** section with a 2×2 image grid referencing
  four fixed paths under `docs/screenshots/`. The paths are live now
  even though the image files aren't checked in yet — drop a PNG in
  with the matching filename and it appears with no further README
  edits. `docs/screenshots/README.md` documents exactly which four
  filenames are expected and what each should show.
- Added a **💬 Feedback & Suggestions** section pointing to GitHub
  Issues.

### Deliberate design choices worth knowing about
- **The image links are real, not commented out**, on purpose — until
  the four PNGs are added, GitHub will show broken-image placeholders
  rather than nothing. That's the intentional trade-off for "add the
  files later and it just works" versus keeping the README's rendered
  state clean in the interim.

### Test
1. After adding the four screenshots from
   `docs/screenshots/README.md`'s table (exact filenames matter), view
   `README.md` on GitHub (or any Markdown previewer) and confirm all
   four images render in the 2×2 grid with their captions underneath.

---

## 2026-08-14 — Request/Response editor: Postman-parity polish

### Where these go
```
thundercall-frontend/src/main/java/com/roze/thundercall/ui/utils/JsonCommentStripper.java   (new)
thundercall-frontend/src/main/java/com/roze/thundercall/ui/utils/JsonSyntaxHighlighter.java  (modified)
thundercall-frontend/src/main/java/com/roze/thundercall/ui/utils/AutoPairing.java             (new)
thundercall-frontend/src/main/java/com/roze/thundercall/ui/utils/JsonPrettyPrinter.java       (new)
thundercall-frontend/src/main/java/com/roze/thundercall/ui/utils/PopupDismissal.java          (new)
thundercall-frontend/src/main/java/com/roze/thundercall/ui/utils/SetVariablePopup.java        (modified)
thundercall-frontend/src/main/java/com/roze/thundercall/ui/utils/CommentThreadPopup.java      (modified)
thundercall-frontend/src/main/java/com/roze/thundercall/ui/utils/EditorContextMenu.java       (modified)
thundercall-frontend/src/main/java/com/roze/thundercall/ui/controllers/MainController.java    (modified)
thundercall-frontend/src/main/resources/css/main.css                                          (modified)
```
Frontend-only. No backend changes, no schema changes, no rebuild
needed on `thundercall-backend`.

### What it does
A cluster of small, real annoyances in the raw body editor, fixed
together since they all touch the same code paths:

- **Comments no longer break JSON validation.** `//` and `/* */`
  inside the raw body — used for notes, or to disable a field like
  `"fatherName": ""` while filling in a form — used to throw a hard
  "Invalid JSON format" error and block sending. Validation now
  strips comments (and any trailing comma left behind) before
  checking, but the literal text you typed — comments included — is
  still exactly what gets sent, matching how Postman's own raw editor
  behaves.
- **Comments get their own color.** Previously a commented-out
  `"fatherName": ""` line was highlighted as if it were live JSON.
  Comments now render in a dedicated grey/italic style.
- **Line numbers**, finally, on the request body, GraphQL
  query/variables, and response viewer — RichTextFX ships the
  capability but nothing in the app was using it.
- **Auto-closing quotes and brackets.** Typing `"`, `'`, `(`, or `[`
  inserts the matching close with the caret in between; typing the
  close yourself right next to its own auto-inserted partner "types
  through" instead of duplicating it; Backspace on an empty pair
  removes both sides in one go.
- **Ctrl+Shift+F formats the whole body** — 2-space indent, original
  field order preserved, no selection required first.
- **Click outside to close.** The right-click menu, the "Comment"
  composer, and the "Set as new variable" card previously only closed
  on Escape; a genuine outside click didn't reliably register as an
  "autohide" event on every window manager. All three now close on
  the very next click anywhere else in the window too.
- **Bug fix:** a request body that was a top-level JSON *array*
  always failed the "Invalid JSON" check, even when it was perfectly
  valid — the check only ever tried parsing it as an object.

### Deliberate design choices worth knowing about
- **Ctrl+Shift+F does not preserve comments through a reformat.**
  Comments aren't part of JSON's data model, so no mainstream JSON
  formatter preserves them either — this one is no exception. If the
  body had comments, the status bar says "Formatted JSON (comments
  removed)" so it's never a silent surprise. See the Roadmap in
  `README.md` for the comment-preserving version this could grow
  into.
- **The formatter doesn't use `org.json`.** `org.json`'s `JSONObject`
  deliberately backs itself with a plain `HashMap` — its own source
  comment says so, "to ensure that elements are unordered" — so
  round-tripping a body through it for pretty-printing would silently
  shuffle field order on every format. `JsonPrettyPrinter` is a small,
  dependency-free, hand-written parser/printer instead, specifically
  so field order is always preserved exactly as written — matching
  Postman's own (JS-based, naturally order-preserving) beautify.
- **The outside-click fix doesn't touch Escape.** Both paths can close
  a popup; whichever fires first wins, the other is just a no-op
  afterward.

### Test
1. Rebuild the frontend: `cd thundercall-frontend && mvn clean javafx:run`.
2. Open a request's **Body → Raw** tab, paste a JSON object, and
   comment out one field with `//` in front of it. Send the request —
   confirm no "Invalid JSON format" popup, and that the sent body
   (check your API's logs, or point it at a request-echo endpoint)
   still contains the comment exactly as typed.
3. Confirm the commented line renders in grey/italic, distinct from
   real keys and strings.
4. Confirm line numbers appear on the left edge of the body editor,
   both GraphQL panes, and the response viewer.
5. In the body editor, type `"` then `test` then `"` — confirm you end
   up with `"test"` and the caret after it, not `"test""`.
6. Paste minified JSON with several fields, press **Ctrl+Shift+F**,
   confirm 2-space indentation with the fields in their original
   order.
7. Add a `//` comment to that body, press **Ctrl+Shift+F** again,
   confirm the comment is gone and the status bar says so.
8. Right-click the body editor, open **Comment** or **Set as
   variable**, then click anywhere else in the window — confirm it
   closes without pressing Escape.
9. Paste a top-level JSON *array* (e.g. `[{"id": 1}]`) as the raw
   body and send it — confirm it's accepted, not flagged as invalid.

---

## 2025 — Monitors (full feature)

### Where these go
```
backend/entity/*.java                        → .../api/entity/         (2 new)
backend/repository/*.java                    → .../api/repository/     (2 new)
backend/dto/Monitor*.java                    → .../api/dto/            (3 new)
backend/config/TaskSchedulerConfig.java      → .../api/config/         (new)
backend/utils/MonitorVariableResolver.java   → .../api/utils/          (new)
backend/MonitorService.java                  → .../api/service/        (new)
backend/service/impl/MonitorServiceImpl.java → .../api/service/impl/   (new)
backend/controller/MonitorController.java    → .../api/controller/     (new)

frontend/models/Monitor*.java            → .../ui/models/          (3 new)
frontend/services/MonitorService.java    → .../ui/services/        (new)
frontend/controllers/MainController.java → .../ui/controllers/     (modified)
frontend/views/main.fxml                 → thundercall-frontend/src/main/resources/views/ (modified)
```
Both backend and frontend need rebuilding. No SQL migration needed —
Monitors are entirely new tables, nothing collides with an existing
constraint.

### What it does
A new **"Monitors"** icon in the sidebar. Create a monitor, point it
at a collection (and optionally an environment for `{{variables}}`),
pick how often it runs, and it fires on schedule automatically — even
with the desktop app closed, since this runs on the backend, not the
client.

- **Schedule presets**: 5/10/15/30 min, hourly, every 6/12 hours, daily
- **Runs every request in the collection**, including nested folders,
  not just the top-level ones
- **Run history** — every run's pass/fail counts, average response
  time, and overall result, kept per monitor
- **Run Now** — trigger an off-schedule run immediately, using the
  same execution path a real scheduled firing uses
- **Email on failure** — reuses your existing Mail Server Settings
  (the same SMTP config Teams/verification already use); defaults to
  your own account email if you don't set a specific notify address
- **Enable/disable** without deleting — edit the monitor and toggle it
  off, its schedule stops immediately
- Survives app/backend restarts — every enabled monitor gets its
  schedule rebuilt automatically on startup, since the database (not
  memory) is the source of truth for what should be running

### Deliberate design choices worth knowing about
- **Monitor runs don't show up in manual Request History.** They use
  their own execution path rather than reusing the one Send uses —
  reasonable on a 5-minute interval, since every firing would
  otherwise flood manual testing history with automated noise. Run
  results live in the monitor's own history instead.
- **Scheduling uses a live `ThreadPoolTaskScheduler` bean, not
  `@Scheduled`.** Spring's `@Scheduled` only handles fixed,
  compile-time schedules — no good for monitors that get created,
  edited, and deleted by users at runtime. Each monitor gets its own
  `ScheduledFuture`, tracked in memory, cancelled and replaced
  whenever it's edited or deleted.

### Test
1. Rebuild both backend and frontend.
2. Configure Mail Server Settings if you want failure emails (Monitors
   still run and log results without it — you just won't be notified).
3. Monitors → Create → pick a real collection, "Every 5 minutes",
   leave notifications on.
4. Click into it → **Run Now** → confirm a row appears in Run History
   with real pass/fail counts matching what you'd expect from that
   collection.
5. Wait past the interval (or shorten it to 5 min and just wait) to
   confirm it fires on its own without you touching anything.
6. Break one request in the collection (e.g. point it at a bad URL),
   Run Now again, confirm the failure shows up and — if configured —
   the notification email arrives.

---

## Have something to add?

If you're extending this file for your own feature work, keep the
format: **Where these go** (file manifest), **What it does** (in
plain, user-facing terms first), **Deliberate design choices** (the
trade-offs a reviewer would otherwise have to reverse-engineer), and
**Test** (concrete, numbered, actually-runnable steps). Newest entry
goes at the top. For anything user-facing, add a line to the relevant
section of `README.md` too — this file is the detailed history,
`README.md` is the current-state summary.
