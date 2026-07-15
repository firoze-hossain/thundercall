# Round 26 — Params table fixed, resizable panels, PDF preview

5 files: MainController.java, main.fxml, main.css, pom.xml, module-info.java.

---

## 1. The doubled Params columns — real root cause found and fixed

Your screenshot showing "Key Value Description Key Value Description" was
a genuine, significant bug: **all five key-value tables** (Params,
Headers, Form Data, Response Headers, Cookies) had their columns defined
**twice** — once statically in the FXML (an old leftover from before
columns were set up dynamically in Java), and again by the Java code,
which never cleared the FXML-defined ones first. Every one of these
tables was silently showing 6 columns instead of 3.

Fixed on both sides so this can't recur:
- Removed the static `<columns>` blocks from all five tables in the FXML.
- The Java setup now explicitly clears any existing columns before adding
  its own, so even if a table got initialized twice for some other reason,
  it can't double up again.

I believe this also explains the odd reddish highlight you saw — with the
table secretly rendering two overlapping copies of itself, a normal
"row selected" highlight would have looked like visual noise layered on
itself. With the duplication gone, editing a param should now look clean,
like your Postman reference — a normal row highlight while selected, gone
once you click away or save. If you still see something odd after this
fix, send a fresh screenshot and I'll dig further.

## 2. Sidebar (Collections/Environments/History) is now resizable

This panel was a fixed 264px region with no divider at all — dragging
never did anything because there was nothing to drag. It's now inside the
same kind of SplitPane as the request/response area, so you can widen it
by mouse to see full request and collection names instead of them getting
cut off (min 200px, max 560px, so it can't be dragged away entirely or
absurdly wide).

## 3. Both dividers are much easier to grab now

The response-panel divider (and the new sidebar divider) had only a 1-2px
hit area, which is exactly what made it fiddly to resize. Both are
significantly wider now, with a highlight color on hover so it's obvious
where to click-drag.

## 4. PDF responses now render an actual preview

The response viewer already had an unused preview pane sitting next to it
(built earlier but never wired to anything — always blank, which is
what you were seeing). When a response is a PDF, it now renders every
page as an image directly into that pane (capped at 20 pages for very
long documents, with a note if there's more — Save still gets you the
complete file). Excel and everything else binary is untouched — same
info-card-and-download behavior as before.

**One honest flag on this one:** PDF rendering needed a new library
(Apache PDFBox), added to `pom.xml` and `module-info.java`. Given the
module-path debugging we already went through together, I want to be
upfront: I can't compile-test this myself in my environment, so there's a
real chance it needs one more small module tweak on first build. If you
see a "module X not found" or "module X not read" error:
- The fast, proven fix you already know works: **delete
  `module-info.java` entirely** — this removes this whole class of error
  for good, with no downside for a desktop app like this.
- Or, the narrower fix: add `requires <exact-name-from-the-error>;` to
  module-info.java, same as we did for reactfx.

## Test
1. Rebuild. If PDFBox causes a module error, see the fallback above.
2. Open any request with params — table shows 3 columns now, editing one
   updates the URL, no visual duplication.
3. Drag the sidebar's right edge — it resizes now.
4. Drag the response panel's top edge — noticeably easier to grab.
5. Send a request that returns a PDF — preview renders on the right;
   send one that returns an Excel file — same download-card behavior as
   before, untouched.
