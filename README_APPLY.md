# Round 8 — children now visible + Postman-style request tabs

Frontend only: 3 files.

| File | Destination |
|---|---|
| MainController.java | .../ui/controllers/ |
| main.fxml | src/main/resources/views/ |
| main.css | src/main/resources/css/ |

## Why folders/requests didn't show (your dump proved they SAVED fine)
Your new dump shows everything persisted correctly this time: collection
"hello firsoze" (ws2) contains folders "first" + "et54" and requests
"hi" + "gg". The workspace fix worked — the bug was in LOADING:
the sidebar used GET /collections, whose "short" response contains NO
folders and NO requests, only names. The children were never in the data
the tree rendered.

FIX: for each collection in the current workspace, the app now calls the
details endpoint (GET /collections/{id}/details via getCollectionWithDetails)
and renders the full structure. After applying, "hello firsoze" will expand
to show: 📁 first, 📁 et54, GET hi, GET gg — with colored method badges.
No backend change needed.

## Request tabs, like Postman (NEW)
A tab bar now sits above the URL bar:
- Clicking a request in the tree opens it in a TAB (or re-selects its
  existing tab, keeping your unsaved edits).
- Creating a request opens it in a new tab automatically.
- Each tab remembers its own method, URL, params, headers and body —
  switch tabs and the editor swaps state instantly.
- Tabs are closable (x); closing the last one gives you a fresh
  "Untitled Request" tab, like Postman.

## Build & test
1. Copy 3 files → mvn clean package (frontend) → run.
2. Login → expand "hello firsoze": folders and requests are THERE.
3. Click "hi" → opens tab "hi" → click "gg" → second tab → edit gg's URL →
   switch back to "hi" tab → its own URL is intact → back to "gg" → your
   edit survived → close tabs → last close leaves "Untitled Request".
