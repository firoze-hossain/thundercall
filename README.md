<div align="center">

# ⚡ Thundercall

**A free, self-hosted API client and team collaboration platform — built to do everything Postman does, without the paywall.**

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)](https://spring.io/projects/spring-boot)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)](https://openjfx.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-336791)](https://www.postgresql.org/)

</div>

---

## What is Thundercall?

Thundercall is a desktop API client with a real backend behind it —
not a toy, not a demo. Build and send HTTP requests, organize them
into collections, manage environments across dev/staging/production,
mock APIs that don't exist yet, schedule automated health checks, and
share your work with a team, all without paying for a single seat.

It's made of two parts:
- **A Spring Boot backend** that owns your data, runs your monitors on
  a schedule, sends your mock server responses, and enforces who can
  see and touch what.
- **A JavaFX desktop client** that talks to that backend — the actual
  workspace you live in day to day.

If you know Postman, Thundercall will feel immediately familiar. If
you don't, think of it as: a place to build and test API requests,
organized like folders on a hard drive, shareable with the people you
work with, and able to run itself on a schedule even when you're not
looking at it.

---

## ✨ Features

### Core request building
- Full HTTP method support (GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS)
- Headers, query params, and request body editors with syntax
  highlighting (JSON-aware, via RichTextFX)
- Bearer, Basic, and OAuth2 authentication
- Pre-request and post-response scripting (Postman-style `pm.*` patterns)
- Form-data with real file uploads, and `x-www-form-urlencoded` support
- Binary response handling (images, PDFs, zips — byte-for-byte, not text-mangled)
- Full request history, searchable

### Organize your work
- Collections → nested folders → requests, exactly like a filesystem
- Environments with variable sets, switchable per request, with
  `{{variable}}` resolution throughout the app
- Multiple workspaces, switchable from one menu
- Postman collection/environment **import and export**, JSON and CSV
- Global search across every collection, folder, and request (⌘/Ctrl+K)

### Beyond plain HTTP
- **GraphQL** — dedicated query/variables editor with autocomplete
- **WebSocket** — connect, send, and watch a live message log, right
  in a request tab
- **Socket.IO** — real client-library-backed connections (not
  hand-rolled protocol parsing), with automatic event logging and emit
  support
- **Mock Servers** — configure method + path + canned response, get a
  real URL anyone can hit, complete with configurable status codes,
  headers, and artificial latency
- **Monitors** — schedule a whole collection to run automatically
  (every 5 minutes to daily), with pass/fail history and email alerts
  on failure

### Team collaboration — free, not gated behind a paid tier
- **Teams**: Owner/Admin/Member roles, invite by email
- **Workspace sharing, two ways**:
  - *Direct invite* — type an email, pick Editor or Viewer, done. No
    team required, just like sharing a document.
  - *Bulk team invite* — invite an entire team to a workspace in one click.
- **Per-environment access control** — environments are opt-in only.
  Being granted access to a workspace shows you *zero* environments
  until the owner explicitly picks which ones you can see — because
  environments often hold real secrets (API keys, tokens, production
  URLs), and collections don't need the same caution.
- **Real workspace switching** — pick a shared workspace from the same
  menu you use for your own, and the *entire app* — Collections,
  Environments, creating/editing/deleting things — operates on it,
  respecting your role. An Editor can do everything an owner can; a
  Viewer can look but not touch.

---

## 🛠️ Technology

### Backend — `thundercall-backend`

| Layer | Technology |
|---|---|
| Language / runtime | Java 21, Spring Boot 3.5 |
| Persistence | Spring Data JPA + Hibernate, PostgreSQL |
| Security | Spring Security, JWT (jjwt) |
| Scheduling | Spring `ThreadPoolTaskScheduler` (dynamic, per-monitor schedules) |
| Email | Spring Mail, dynamic per-account SMTP configuration |
| HTTP execution | Apache HttpClient 5 (the engine behind every request you send) |
| Real-time protocols | `socket.io-client` for genuine Socket.IO support |
| Object mapping | MapStruct, Jackson |
| Boilerplate reduction | Lombok |

### Frontend — `thundercall-frontend`

| Layer | Technology |
|---|---|
| UI framework | JavaFX 21 |
| Code/text editing | RichTextFX + ReactFX (syntax highlighting, autocomplete) |
| Icons | Ikonli (FontAwesome5) |
| PDF rendering | Apache PDFBox |
| HTTP client (to the backend) | Java 11+ built-in `java.net.http` |
| JSON | Jackson, `org.json` |
| Build | Maven, `javafx-maven-plugin` |

Both projects build independently with Maven and talk to each other
over a REST API on `localhost:9084/api/v1` by default.

---

## 🚀 Getting Started

### Prerequisites
- JDK 21+
- Maven 3.9+
- PostgreSQL 15+ (a database named `thunder_db`, or update
  `application.yml` to match yours)

### Backend
```bash
cd thundercall-backend
# set DB_USERNAME and DB_PASSWORD as environment variables, or edit
# src/main/resources/application.yml directly
mvn spring-boot:run
```
The API comes up on `http://localhost:9084/api/v1`. Schema is managed
by Hibernate (`ddl-auto: update`) — no manual migrations needed for a
fresh database.

### Frontend
```bash
cd thundercall-frontend
mvn clean javafx:run
```
On first launch, create an account and your first workspace — sample
collections and environments are set up for you automatically.

---

## 🗺️ Project structure

```
thundercall/
├── thundercall-backend/
│   └── src/main/java/com/roze/thundercall/api/
│       ├── controller/    REST endpoints
│       ├── service/       business logic
│       ├── entity/        JPA entities
│       ├── repository/    Spring Data repositories
│       ├── dto/           request/response records
│       ├── security/      auth + access-control guards
│       └── mapper/        entity ↔ DTO mapping
└── thundercall-frontend/
    └── src/main/java/com/roze/thundercall/ui/
        ├── controllers/   JavaFX controllers (MainController owns most UI logic)
        ├── services/      backend API clients
        ├── models/        DTOs mirrored from the backend
        └── dialogs/       reusable dialog windows
```

---

## 📍 Roadmap — what's next

Thundercall is under active development. Here's what's deliberately
scoped for later rather than half-built now:

- **In-place editing of shared collections from the browse view** —
  Editors can already create, edit, and delete everything in a shared
  workspace through the main UI; a lighter-weight quick-edit view
  directly from the Team Spaces browse dialog is a natural refinement.
- **Real-time collaborative editing** — multiple people watching the
  same request update live, the way Postman's paid tiers do it. This
  needs a genuinely different piece of infrastructure (a live
  WebSocket broadcast layer with presence indicators) rather than an
  extension of what exists today.
- **Path-parameter and wildcard matching in Mock Servers** — routes
  currently match exact paths only (`/users/1`, not `/users/:id`).
- **Per-collection access overrides** within a shared workspace
  (finer-grained than the current whole-workspace Editor/Viewer model).
- **Collection runner UI** — Monitors already run a whole collection
  on a schedule; a manual "run this collection now and show me a
  report" view is a natural sibling feature.
- **cURL import/export**, drag-and-drop request reordering, and
  installer packaging (jpackage) for distributing the desktop client
  without requiring a local Maven build.

---

## 🤝 Contributing

This project is being built iteratively, feature by feature. If you're
picking this up: the codebase favors clear, explicit service-layer
logic over cleverness, and every access-control decision (who can see
or edit what) flows through `WorkspaceAccessGuard` on the backend —
that's the one place to look if you're adding a new kind of shared
resource.

---

## 📄 License

Not yet decided — add a `LICENSE` file with your preferred terms
before treating this as open for external use.
