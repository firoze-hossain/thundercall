# Round 5 — workspace 409 fix, Postman tree, search

| File | Destination |
|---|---|
| WorkspaceServiceImpl.java | thundercall-backend/.../api/service/impl/  (BACKEND) |
| MainController.java | thundercall-frontend/.../ui/controllers/ |
| main.fxml | thundercall-frontend/src/main/resources/views/ |
| main.css | thundercall-frontend/src/main/resources/css/ |

(Keep light.css, ThemeManager.java, AlertUtils.java, module-info.java from the
previous delivery — they are unchanged and still required.)

## 1. Create Workspace error (HTTP 409) — FIXED
Root cause: registration now auto-creates a default workspace, but
setupInitialWorkspace still threw "User already has workspace" — so
Create Workspace could never succeed for anyone. Fix (backend
WorkspaceServiceImpl): users can now create ADDITIONAL workspaces, exactly
like Postman. No frontend change needed; the same dialog now succeeds.

## 2. Postman-style collections tree
- Requests show a COLORED method badge before the name: GET green,
  POST amber, PUT blue, DEL red, PAT purple — same as Postman.
- Collections and folders show their icon.
- Hovering a collection/folder row reveals  +  and  ⋯  buttons like Postman:
  + adds a request there, ⋯ opens the actions menu.
- Right-click (or ⋯) shows the Postman menu: Add request, Add folder | Run |
  Share, Copy link | Rename (Ctrl+E), Duplicate (Ctrl+D), Delete (Del, in red).
  Run/Share/Copy link are visible but disabled (not implemented yet) —
  Rename/Duplicate/Delete/Add all work through your existing logic.
- The ⋯ sidebar menu now reads "Import Collection…" / "Export Collection…".

## 3. Search — both kinds
- SIDEBAR SEARCH: a "Search collections" box above the tree filters it live,
  keeping matching branches expanded (Postman behaviour). Clearing the box
  restores the full tree. Actions still work on filtered results.
- GLOBAL SEARCH: press Ctrl+K anywhere, or use the "Search Thundercall"
  box centered in the header. A dialog searches every collection, folder and
  request, shows "TYPE · Collection / Folder / path", and double-click or
  Enter jumps to the item in the tree (expands parents, scrolls, selects).

## Build & test
1. Backend: replace WorkspaceServiceImpl.java → mvn clean package → restart.
2. Frontend: replace the 3 files → mvn clean package → run.
3. Test: Workspaces ▾ → Create Workspace… (no more 409) · hover a folder →
   + and ⋯ appear · right-click a folder → Postman menu · type in
   "Search collections" · press Ctrl+K and jump to any request.
