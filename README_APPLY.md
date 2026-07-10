# Round 7 — THE invisibility bug found (proved by your DB dump) + popup fixes

## What your database revealed
Your dump shows user "firoze" has TWO workspaces:
  ws1 (id 1) "firoze's Workspace"  ← auto-created at registration
  ws2 (id 2) "My Workspace"        ← created via the dialog
Collections: "Getting started"→ws1, "Getting started"→ws2, "Hello"→ws1.

Your UI was showing ws2 ("My Workspace" in the workspace bar), but the
backend's createCollection ALWAYS used your first workspace (ws1) — it had
no idea which workspace the UI selected. So "Hello" (and any folder/request
inside it) landed in ws1 = INVISIBLE while you looked at ws2. Also: your
folders table is empty and no custom request exists — creations were going
into hidden places or being attempted on invisible collections.

## The fix (both sides now agree on the workspace)
- BACKEND CollectionRequest gains `workspaceId`; createCollection uses it
  (verified against your ownership check), falling back to the default.
- FRONTEND automatically sends the SELECTED workspace's id on every
  collection creation (one central place: CollectionService).
- Default workspace is now DETERMINISTIC on both sides: the oldest (lowest
  id), and GET /workspaces returns them sorted — so the UI's startup pick
  and the backend's fallback are the same workspace, always.

Result: create collection → appears instantly in the sidebar. Folders and
requests inside it too (their flow already refreshed correctly — they were
only "missing" because their parent collection was hidden).

## The other two bugs
- "TreeItem [ value: Getting started ]" in the Select Collection dialog:
  it displayed raw TreeItem objects. Now shows collection names, themed.
- Tour popup on EVERY login: the server only marks the tour complete if you
  finish all 6 steps — skipping/closing left it "incomplete" forever. The
  tour now shows ONCE per user per machine (stored in preferences),
  whatever you click. (You can still relaunch it later if we add a Help
  menu item.)

## Files
| File | Destination |
|---|---|
| CollectionRequest.java | BACKEND .../api/dto/ |
| CollectionServiceImpl.java | BACKEND .../api/service/impl/ |
| WorkspaceServiceImpl.java | BACKEND .../api/service/impl/ |
| MainController.java | FRONTEND .../ui/controllers/ |
| CollectionRequest_frontend.java | FRONTEND .../ui/models/CollectionRequest.java (rename!) |
| CollectionService.java | FRONTEND .../ui/services/ |

## Build & test (backend AND frontend this round)
1. Backend: 3 files → mvn clean package → restart.
2. Frontend: 3 files (rename CollectionRequest_frontend.java) → build → run.
3. Test: login → workspace bar shows "firoze's Workspace" (oldest) and you
   should now SEE "Hello"! → create a collection → visible immediately →
   switch to "My Workspace" via Workspaces ▾ → create collection there →
   visible → sidebar + button with nothing selected → dialog shows NAMES →
   logout/login twice → tour appears at most once.

Folder expanding like Postman (nested folders) needs the backend parent_id
migration — bugs are done, so say "do nested folders" and that's next.
