# Codex-1 Agent Guide

This repository is a large multi-module Android project written in Kotlin and Jetpack Compose with a Firebase backend.  A TypeScript Firebase Functions project lives under `functions/`.  The code follows a Clean Architecture approach with a strict module hierarchy and the UseCase Provider pattern.

## Modules
- **app** – Android application entry point, DI setup and navigation graph.
- **app_api** – Interfaces that the `app` module exposes to feature modules.
- **core** – Shared code split into:
  - `core_common` – utilities, coroutine dispatchers, `CustomResult` wrapper.
  - `core_ui` – Compose UI components and theme.
  - `core_navigation` – `NavigationManger` interface and type‑safe routes.
  - `core_fcm` – Firebase Cloud Messaging helpers.
- **domain** – Pure Kotlin business layer: domain models, value objects, events, repository interfaces, use cases and *UseCaseProvider* classes.
- **data** – Repository implementations and data sources (Firestore, Auth, Storage, etc.).
- **feature** – Each feature (login, chat, calendar, project management…) is its own module containing UI Composables and ViewModels.
- **functions** – Firebase Functions (TypeScript) using a similar Clean Architecture style.

Supporting files such as `codex_setup.sh`, `CLAUDE.md`, `GEMINI.md` and the `.tasks/` directory contain environment setup, architectural docs and pending work.  Leave AI helper directories (`.cursor`, `.claude`, `.gemini`, `.windsurf`) untouched.

## Project Flow
The typical flow for user actions is:
```
UI → ViewModel → UseCaseProvider → UseCase → Repository (Impl) → DataSource (Impl) → Firebase/Network
```
- ViewModels must depend on UseCaseProviders, **not** directly on repositories.
- UseCases handle business logic and return `CustomResult<Success, Error>`.
- Repositories hide data access behind interfaces; implementations live in the `data` module and delegate to `datasource` classes.

## Firebase Functions
`functions/src/index.ts` exports callable functions for:
- User profile updates and image management.
- Project profile image handling and sync triggers.
- System utilities (`helloWorld`, temp file cleanup).
- Friend request management.
- Direct message channel management.
- Member and project invitation management.
These functions use a layered structure (`business`, `domain`, `infrastructure`) mirroring the Android modules.

## Domain Model Overview
Entities such as `User`, `Project`, `DMChannel`, `Category`, `Message` etc. reside in `domain/model/base`.  They extend `AggregateRoot`, track state and produce domain events.  Immutable value objects like `UserId`, `UserEmail`, `DocumentId` ensure type‑safety.  Enums and data/value classes live under `model/enum`, `model/data` and `model/vo`.

## Programming Style & Rules
- Kotlin 2.1, AGP 8.1, JDK 17.
- MVVM with Jetpack Compose UI.
- Navigation handled only through `NavigationManger` and type‑safe routes.
- UseCaseProvider pattern groups related use cases for injection.
- Repositories and UseCases must return `CustomResult` for explicit success/failure.
- Timestamps are stored as UTC `Instant` values.
- Follow the patterns in `PROVIDER_PATTERN_MIGRATION_GUIDE.md` when creating new features.

## Testing & Linting
- For Kotlin modules run:
  - `./gradlew lintDebug`
  - `./gradlew test`
  - Firebase Functions use Jest: `npm test`.
  - Because full builds are slow, target specific modules when possible, e.g.
    `./gradlew :core_common:lintDebug :core_common:test`.
- Documentation or comment‑only changes do **not** require running tests.

