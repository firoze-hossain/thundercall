# Monitors — full feature

## Where these go
```
backend/entity/*.java                    → .../api/entity/         (2 new)
backend/repository/*.java                → .../api/repository/     (2 new)
backend/dto/Monitor*.java                → .../api/dto/            (3 new)
backend/config/TaskSchedulerConfig.java  → .../api/config/         (new)
backend/utils/MonitorVariableResolver.java → .../api/utils/        (new)
backend/MonitorService.java              → .../api/service/        (new)
backend/service/impl/MonitorServiceImpl.java → .../api/service/impl/ (new)
backend/controller/MonitorController.java → .../api/controller/    (new)

frontend/models/Monitor*.java            → .../ui/models/          (3 new)
frontend/services/MonitorService.java    → .../ui/services/        (new)
frontend/controllers/MainController.java → .../ui/controllers/     (modified)
frontend/views/main.fxml                 → thundercall-frontend/src/main/resources/views/ (modified)
```
Both need rebuilding. No SQL migration needed this time — Monitors are
entirely new tables, nothing collides with an existing constraint.

## What it does
New **"Monitors"** icon in the sidebar. Create a monitor, point it at a
collection (and optionally an environment for {{variables}}), pick how
often it runs, and it fires on schedule automatically — even with the
app closed, since this runs on your backend, not the desktop client.

- **Schedule presets**: 5/10/15/30 min, hourly, every 6/12 hours, daily
- **Runs every request in the collection**, including nested folders,
  not just the top-level ones
- **Run history** — every run's pass/fail counts, average response
  time, and overall result, kept per monitor
- **Run Now** — trigger an off-schedule run immediately, same
  execution path a real scheduled firing uses
- **Email on failure** — reuses your existing Mail Server Settings
  (the same SMTP config Teams/verification already use); defaults to
  your own account email if you don't set a specific notify address
- **Enable/disable** without deleting — edit the monitor and toggle it
  off, its schedule stops immediately
- Survives app/backend restarts — every enabled monitor gets its
  schedule rebuilt automatically on startup, since the DB (not memory)
  is the source of truth for what should be running

## A deliberate design choice worth knowing about
Monitor runs do **not** show up in your manual Request History. They
use their own execution path rather than reusing the same one Send
uses — reasonable on a 5-minute interval, since every firing would
otherwise flood your manual testing history with automated noise. Run
results live in the monitor's own history instead, which is what you
actually want to look at for this.

## How scheduling works, briefly
Spring's `@Scheduled` only handles fixed, compile-time schedules — no
good for monitors that get created, edited, and deleted by users at
runtime. This uses a real `ThreadPoolTaskScheduler` bean directly:
each monitor gets its own `ScheduledFuture`, tracked in memory,
cancelled and replaced whenever you edit or delete it.

## Test
1. Rebuild both backend and frontend.
2. Make sure Mail Server Settings are configured if you want failure
   emails (Monitors will still run and log results without it — you
   just won't get notified).
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
