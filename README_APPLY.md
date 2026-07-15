# Round 25 — Params sync, better cell editing, environment menu, full Auth redesign

FRONTEND ONLY: 2 files (main.css untouched this round — nothing to add).

| File | Destination |
|---|---|
| MainController.java | .../ui/controllers/ |
| main.fxml | src/main/resources/views/ |

---

## 1. Params showing empty after import — root cause found and fixed

The Params tab was a **write-only scratchpad**: nothing ever parsed an
existing URL's query string into it. Your imported request's URL already
had `?eid=16465` baked in, but the Params table had no idea that string
existed — it only ever got populated by manually clicking "Add Row".

Fixed properly, matching Postman's actual model: **the URL and the Params
table are now two views of the same data, synced live in both
directions**:
- Opening a request (or importing, or just typing in the URL bar) parses
  the query string straight into the Params table.
- Editing a Params row — or adding/deleting one — rewrites the URL's query
  string immediately.

This also fixed a **real latent bug** I found while doing this: the old
`buildFullUrl()` re-appended every Params row onto the URL again at send
time. Since the URL could already contain the same param (like your
imported `?eid=16465`), sending would have produced `?eid=16465&eid=16465`
— doubled. That re-appending logic is gone now; the URL is always the
single source of truth by send time.

## 2. Table cell editing — commits on losing focus now, not just Enter

This was a genuine JavaFX quirk: the default table cell editor **cancels**
an edit when you click away instead of saving it — Enter was the only way
to commit, exactly matching what felt "not good." Fixed with a custom
cell that also commits when it loses focus (clicking another cell,
another tab, anywhere) — the standard, well-known fix for this.

Separately: **Ctrl+S now force-commits any cell you're still typing in**
before saving. A global shortcut doesn't naturally make a focused table
cell lose focus on its own, so without this, whatever you were mid-typing
when you hit Ctrl+S could have been silently dropped.

## 3. Environments — right-click menu: Rename, Duplicate, Export as JSON, Delete

Added the locally-relevant subset of Postman's environment menu (Share,
fork, pull-request, merge are git/cloud team features with no equivalent
in a self-hosted single-user tool, so those aren't included):
- **Rename** (Ctrl+E) and **Duplicate** (Ctrl+D) work like the collection
  tree's existing rename/duplicate.
- **Export as JSON** writes a clean `{name, description, values: [...]}`
  file you can hand to someone else or re-import later.
- **Delete** with confirmation.

## 4/5/6. Authorization tab — full Postman-style redesign

- **Two-column layout** matching Postman: Auth Type on the left, the
  actual fields on the right — replacing the old single stacked column.
- **Bearer Token field now has the same `{{variable}}` autocomplete and
  coloring as the URL bar and body** (this needed converting it to the
  same CodeArea-based editor under the hood) — type `{{` and your
  environment's variables show up live, exactly like your reference
  screenshot. Same treatment for the Basic Auth username field.
- **OAuth 2.0 got a real, complete implementation** — not just a UI
  mockup:
  - Full Postman-style panel: "Add auth data to" (Headers/Query Params),
    Current Token + Header Prefix, and a "Configure New Token" section
    with Grant Type, Auth URL, Access Token URL, Client ID/Secret, Scope,
    and (for Password Credentials) Username/Password — fields show and
    hide per grant type, same as Postman.
  - **"Get New Access Token" actually works** for Client Credentials and
    Password Credentials grants — a real POST to your Access Token URL,
    parsing `access_token` from the response and filling in Current
    Token.
  - Authorization Code and Implicit need an interactive browser sign-in
    step this app doesn't have yet — rather than fake a result, it tells
    you plainly and suggests pasting a token manually, or using the two
    working grant types.
  - The resulting token is used at send time exactly like Bearer Token —
    as an `Authorization` header, or appended to the URL as
    `access_token=...` if you chose "Query Params."
  - OAuth2 config is saved with the request (reusing the existing
    `authToken` column as a small JSON blob — no database change needed),
    so it survives switching tabs, closing, and reopening.

## Test
1. Rebuild frontend.
2. Open an imported request with a `?param=value` URL — Params tab shows
   it now. Edit a param value, watch the URL bar update live. Type
   directly in the URL bar, watch Params update.
3. Edit a param cell, click elsewhere without pressing Enter — it sticks.
4. Right-click an environment — Rename/Duplicate/Export/Delete all there.
5. Authorization → Bearer Token → type `{{` in the Token field — your
   environment variables suggest, same as the URL bar.
6. Authorization → OAuth 2.0 → Client Credentials → fill in your token
   URL/client id/secret → "Get New Access Token" → a real token comes
   back and gets used on Send.
