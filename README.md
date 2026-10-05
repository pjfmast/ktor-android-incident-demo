# Ktor & Android Incident App

[![Kotlin]][kotlin-url]
[![Ktor]][ktor-url]
[![Kotlin Toolchain]][toolchain-url]

This project is a modern full-stack demo application (`ktor-android-incident-demo`) structured as a multi-module Kotlin project (Amper / Kotlin Toolchain) combining a Ktor backend server and an Android client.

### Project Modules

- **`android`**: Android application module (`android/app`) depending on `shared`. Implements the mobile client app using Jetpack Compose (*coming soon*). Package: `incident.android.*`
- **`shared`**: Kotlin Multiplatform library (`[jvm, android]`) containing shared domain models, Enums (`Priority`, `Status`, `Category`, `Role`), DTOs, and serialization contracts. Package: `incident.shared.*`
- **`server`**: Ktor backend service depending on `shared`. Implements JWT authentication, role-based authorization, Exposed ORM persistence, and REST endpoints. Package: `incident.server.*`

---

### Highlights & Modern Tech Stack

- **Build System**: Built with the modern **Kotlin Toolchain** (powered by Amper) in a multi-module layout (`android`, `shared`, `server`).
- **Architecture**: `embeddedServer(Netty)` pattern with explicit, type-safe `Dependencies` passing and isolated test doubles.
- **Ktor 3.6**: Modern typed authentication API (`jwt<User>`) with direct `User` principals and declarative roles (`withRoles`, `authenticateWith`).
- **Security & Authorization**: Hierarchical Role-Based Access Control (RBAC), fine-grained Resource Ownership checks (ABAC), and endpoint rate limiting (`RateLimit`).
- **HTTP Standards & Observability**: Structured request logging (`CallLogging`), standard `HEAD` support (`AutoHeadResponse`), and typed exception mapping (`StatusPages`).
- **Persistence**: **JetBrains Exposed** ORM with automated demo data seeding on H2 (in-memory or file-based).
- **Serialization**: Native JSON handling via `kotlinx.serialization` with shared DTOs in `shared`.
- **Interactive API Testing**: Ready-to-use HTTP requests in `server/test/http-requests/` for direct execution via IntelliJ IDEA's HTTP Client.
- **Mobile Client**: Native Android app (`android` module) coming soon.

---

### Quick Start

This project uses the official Kotlin Toolchain wrapper (no Gradle installation required):

```bash
# Run automated tests across all modules
./kotlin test          # Linux / macOS
.\kotlin.bat test      # Windows

# Run the backend server (starts on http://localhost:8080)
./kotlin run -m server       # Linux / macOS
.\kotlin.bat run -m server   # Windows
```

---

### Feature Modules (`server`)

| Module / Feature | Description |
|:---|:---|
| `incident.server.users.UsersModule` | Registration, user profile management, role inspection |
| `incident.server.auth.AuthModule` | Login and JWT token issuance |
| `incident.server.incidents.IncidentsModule` | Incident reporting, updates, status lifecycle, image upload management |

[kotlin-url]: https://kotlinlang.org
[ktor-url]: https://ktor.io
[toolchain-url]: https://kotlin-toolchain.org/dev/

[Kotlin]: https://img.shields.io/badge/Kotlin-2.4.20-blue.svg?logo=data:image/svg+xml;base64,PHN2ZyB2aWV3Qm94PSIwIDAgMjAuNTU0IDIwLjU0MyIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmFkaWFsR3JhZGllbnQgaWQ9ImEiIGN4PSIyMi40MzIiIGN5PSIzLjQ5MyIgcj0iMjEuNjc5IiBncmFkaWVudFRyYW5zZm9ybT0ibWF0cml4KDEuMDg1NiAwIDAgMS4wODU2IC00LjQ4NDIgLTIuOTUxMSkiIGdyYWRpZW50VW5pdHM9InVzZXJTcGFjZU9uVXNlIj48c3RvcCBzdG9wLWNvbG9yPSIjZTQ0ODU3IiBvZmZzZXQ9Ii4wMDMiLz48c3RvcCBzdG9wLWNvbG9yPSIjYzcxMWUxIiBvZmZzZXQ9Ii40NjkiLz48c3RvcCBzdG9wLWNvbG9yPSIjN2Y1MmZmIiBvZmZzZXQ9IjEiLz48L3JhZGlhbEdyYWRpZW50PjxwYXRoIGQ9Im0yMC41NTQgMjAuNTQzaC0yMC41NTR2LTIwLjU0M2gyMC41NTRsLTEwLjQ4OSAxMC4xMTl6IiBmaWxsPSJ1cmwoI2EpIi8+PC9zdmc+
[Ktor]: https://img.shields.io/badge/Ktor-3.6.0-blue.svg?logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA0OCA0OCI+PGxpbmVhckdyYWRpZW50IGlkPSJnIiB4MT0iMSIgeTE9IjE3IiB4Mj0iMzEiIHkyPSI0NyIgZ3JhZGllbnRVbml0cz0idXNlclNwYWNlT25Vc2UiPjxzdG9wIG9mZnNldD0iMCIgc3RvcC1jb2xvcj0iIzZiNTdmZiIvPjxzdG9wIG9mZnNldD0iLjUiIHN0b3AtY29sb3I9IiNmZjQ1ZWQiLz48c3RvcCBvZmZzZXQ9IjEiIHN0b3AtY29sb3I9IiNkZDEyNjUiLz48L2xpbmVhckdyYWRpZW50PjxwYXRoIGQ9Im00OCAzMi0xNiAxNkwwIDE2IDE2IDAgNDggMzJ6IiBmaWxsPSJ1cmwoI2cpIi8+PHBhdGggZD0iTTMyIDE2SDE2djE2aDE2eiIgZmlsbD0iIzAwMCIvPjwvc3ZnPg==
[Kotlin Toolchain]: https://img.shields.io/badge/Kotlin_Toolchain-0.13-blue.svg?logo=data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTM2IDM2SDRMMjYuMjExNyA0SDM1Ljk3NzhMMzYgMzZaTTI2LjA2NjYgMTkuNjA4M0wyMS40NDM5IDI2LjI2ODFIMjYuMDY2NlYxOS42MDgzWiIgZmlsbD0idXJsKCNwYWludDBfbGluZWFyXzM3NzJfMTU0KSIvPgo8ZGVmcz4KPGxpbmVhckdyYWRpZW50IGlkPSJwYWludDBfbGluZWFyXzM3NzJfMTU0IiB4MT0iMTEuODcwMyIgeTE9IjExLjg3NzgiIHgyPSIzNi4yOTAzIiB5Mj0iMzYuMjg3MSIgZ3JhZGllbnRVbml0cz0idXNlclNwYWNlT25Vc2UiPgo8c3RvcCBvZmZzZXQ9IjAuMjEyMTciIHN0b3AtY29sb3I9IiMzQkVBNjIiLz4KPHN0b3Agb2Zmc2V0PSIwLjc1MjU0IiBzdG9wLWNvbG9yPSIjMDg3Q0ZBIi8+CjwvbGluZWFyR3JhZGllbnQ+CjwvZGVmcz4KPC9zdmc+Cg==
