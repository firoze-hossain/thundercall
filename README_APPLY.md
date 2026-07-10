# Round 6 — all reported bugs fixed

| File | Destination |
|---|---|
| MainController.java | .../ui/controllers/ |
| AuthController.java | .../ui/controllers/ |
| WorkspaceSetupDialog.java | .../ui/dialogs/ |
| WorkspaceService.java | .../ui/services/  (FRONTEND service) |
| CredentialStore.java | .../ui/utils/  (NEW) |
| main.fxml | src/main/resources/views/ |
| login.fxml | src/main/resources/views/ |
| register.fxml | src/main/resources/views/ |
| main.css | src/main/resources/css/ |

## Console exception — FIXED
"Unable to coerce response-preview to interface java.util.Collection":
WebView is constructed through a Builder by FXMLLoader, and builder classes
cannot take styleClass as an attribute. Removed the attribute from main.fxml.

## Squeezed bar showing "… …▾ …" (image 2) — FIXED
main.css had `.environment-combo { -fx-pref-width: 10000; }` (from the old
sidebar layout). In the new workspace bar it made the environment selector
swallow the whole row, crushing "My Workspace", New and Import into "…".
Rule removed; the combo uses its normal 200px width.

## Workspace popup on EVERY login — FIXED
WorkspaceManager.hasWorkspace() was an in-memory flag, always false at app
start, so the setup dialog opened each login. Now the app asks the server
(GET /workspaces): if you have workspaces, the first becomes current, the
Workspaces ▾ menu is filled with the REAL list, and no popup appears. The
popup only shows for a genuinely new account with zero workspaces.

## New workspace not selected + duplicate "Getting started" — FIXED
- Creating a workspace now SELECTS it: the workspace-bar name updates, the
  Workspaces ▾ menu shows a ✓ next to it, and the sidebar reloads.
- The collections tree now shows ONLY the current workspace's collections
  (Postman behaviour) — the duplicate "Getting started" entries were
  collections from your other workspaces all mixed together.
- Switch workspaces any time from Workspaces ▾.
- The "Create sample requests" checkbox in the dialog is now honored
  (it was silently ignored — every workspace got sample data).

## Folder under folder — honest behaviour
Your backend's Folder has no parent-folder field, so nested folders cannot
exist in the data model (that's why nothing appeared). Now: using "Add
folder" from a folder creates it in the enclosing collection and the tree
refreshes immediately. True nested folders need a backend change (parent_id
column + recursion) — say the word and I'll build it next round.

## Remember me — NEW
Login page has a "Remember me" checkbox. When checked, your username and
password are saved (Base64-obfuscated in OS user preferences) and pre-filled
next launch — one click to sign in. Unchecking clears them. Note in code:
for public distribution this should move to the OS keychain.

## Build & test
1. Frontend only this round: copy files → mvn clean package → run.
2. Test: login (no popup!) → Workspaces ▾ shows your real workspaces with ✓ →
   create workspace → it becomes selected, sidebar shows only its collections
   → uncheck "Create sample requests" → no extra "Getting started" →
   folder "Add folder" works from any tree item → logout, login with
   Remember me → fields pre-filled → console: no coerce exception.
