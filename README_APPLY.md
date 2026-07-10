# Postman-style redesign + error fix — apply guide

| File | Destination |
|---|---|
| module-info.java | thundercall-frontend/src/main/java/ |
| main.fxml | thundercall-frontend/src/main/resources/views/ |
| main.css | thundercall-frontend/src/main/resources/css/ |
| light.css | thundercall-frontend/src/main/resources/css/  (NEW file) |
| MainController.java | .../ui/controllers/ |
| ThemeManager.java | .../ui/utils/  (NEW file) |
| AlertUtils.java | .../ui/utils/ |

## 1. The error — fixed with one line
Your tables showed "No content in table" and the log was full of
IllegalAccessException because module-info.java opened
com.roze.thundercall.ui.controllers only to javafx.fxml, but
PropertyValueFactory lives in javafx.base and reflects on your
MainController$KeyValuePair inner class. Fixed:

    opens com.roze.thundercall.ui.controllers to javafx.fxml, javafx.base;

## 2. Postman layout — which menu is where (matching your screenshot 1)
- Row 1: MenuBar — File (New Collection / New Environment / Import / Export /
  Settings / Logout / Exit), View (Dark Theme / Light Theme), Help (About).
- Row 2: logo · Home · Workspaces ▾ (My Workspace, Create Workspace…) ·
  API Network — right side: Connected pill · ⚙ settings · User ▾.
- Row 3 (workspace bar): "My Workspace" · New ▾ (Collection / Environment /
  Request) · Import — right side: the environment selector (Postman position).
- Left edge: icon rail with Collections / Environments / History buttons that
  switch the sidebar, exactly like Postman's left strip.
- Center: URL bar (method + URL + Send + save + sample), request tabs in
  Postman order (Params, Authorization, Headers, Body, Scripts), response
  section below with Body / Cookies / Headers / Tests and
  "Click Send to get a response" placeholder text.
- Bottom: status bar.

Every fx:id and handler from your old FXML is preserved — verified
programmatically — so ALL existing MainController logic keeps working.

## 3. Workspace modal (like Postman)
Your app ALREADY shows the workspace-setup modal after login when no
workspace exists (showWorkspaceSetup in MainController), and it is
cancellable. What was missing was a way to create one later — now:
Workspaces ▾ → "Create Workspace…" in the header (new handleCreateWorkspace).
With the backend auto-create fix from earlier, users are never blocked either way.

## 4. Settings + Light/White theme
- ⚙ gear, User ▾ → Settings, or File → Settings… opens a Settings dialog
  with Dark/Light radio buttons. View menu has direct Dark/Light items.
- ThemeManager persists the choice (Preferences) — the app reopens in the
  last theme. light.css overrides only the color tokens, so both themes share
  one layout. Colors are smoother in both: soft neutrals, one orange accent.
- AlertUtils now themes every popup, so no more bright default dialogs.

## Build order
1. Copy all files (light.css and ThemeManager.java are new).
2. mvn clean package in thundercall-frontend.
3. Run → tables now accept rows (module fix) → View → Light Theme to test
   switching → ⚙ to test the Settings dialog → Workspaces ▾ → Create Workspace.
