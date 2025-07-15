# Project Overview

This is a large, multi-module Android application built with Kotlin and Jetpack Compose. It follows a Clean Architecture approach with a strict module hierarchy and uses Firebase for its backend services. The application appears to be a team collaboration tool, similar in concept to Discord or Slack, with features for projects, channels, direct messaging, and scheduling.

A TypeScript Firebase Functions project is also included in the `functions/` directory.

## Core Technologies & Versions

*   **Kotlin**: 2.1.0
*   **Android Gradle Plugin (AGP)**: 8.10.1
*   **JVM**: 17
*   **UI**: Jetpack Compose (BOM 2025.06.01)
*   **Architecture**: MVVM with Clean Architecture, UseCase Provider Pattern
*   **Dependency Injection**: Hilt (2.56.2)
*   **Asynchronous Programming**: Kotlin Coroutines & Flow
*   **Backend**: Firebase (BOM 33.16.0) - Firestore, Authentication, Storage, Functions
*   **Database**: Room (2.7.2) for local storage, primarily uses Firestore caching.
*   **Navigation**: Jetpack Navigation for Compose, managed by a custom `NavigationManager`.
*   **Image Loading**: Coil

## Project Structure & Modules

The project is divided into several module types:

*   `app`: The main application module. It's the entry point and handles DI setup and the main navigation graph.
*   `app_api`: Defines interfaces that the `app` module exposes to feature modules, decoupling them.
*   `core`: Contains shared functionality used across multiple feature modules.
    *   `core_common`: Common utilities, constants, coroutine dispatchers, and a `CustomResult` wrapper for error handling.
    *   `core_navigation`: Handles navigation logic, including a type-safe `NavigationManager` and routes.
    *   `core_ui`: Reusable Jetpack Compose components, Material 3 theme, and UI-related utilities.
    *   `core_fcm`: Firebase Cloud Messaging setup and handling.
*   `data`: Implements repositories and data sources (Firestore, Firebase Auth, Storage, Room). It handles data mapping (DTOs) and interacts directly with the backend.
*   `domain`: The pure Kotlin business logic layer. It contains domain models, value objects, repository interfaces, and UseCases. It is framework-independent.
*   `feature`: Each feature of the application is encapsulated in its own module (42 total). This promotes separation of concerns. Examples include:
    *   **Auth**: `feature_login`, `feature_signup`, `feature_profile`, `feature_settings`
    *   **Project**: `feature_home`, `feature_add_project`, `feature_project_detail`
    *   **Social**: `feature_friends`, `feature_chat`, `feature_search`
    *   **Scheduling**: `feature_calendar`, `feature_add_schedule`
*   `functions`: Contains the backend logic for the application, written in TypeScript and deployed as Firebase Functions. It follows a simplified architecture.

## Architecture & Key Patterns

### Android App Architecture

The Android app follows Clean Architecture principles with a unidirectional data flow:

**UI → ViewModel → UseCase → Repository → DataSource → Firebase/Room**

*   **MVVM**: The UI layer uses ViewModels.
*   **UseCase Provider Pattern**: UseCases are grouped into semantic providers (e.g., `AuthSessionUseCaseProvider`). ViewModels inject these providers to get access to specific use cases. This avoids injecting individual UseCases.
*   **Repository Pattern**: Repositories hide data access details behind interfaces defined in the `domain` layer. Implementations in the `data` layer delegate to `DataSource` classes.
*   **Result Handling**: All asynchronous operations (in UseCases and Repositories) must return a `CustomResult<Success, Error>` object for explicit success and failure handling.
*   **Navigation**: All navigation is handled through the `NavigationManager` and type-safe routes. Direct use of `NavController` is discouraged.
*   **Domain Model**: The domain layer includes entities (`AggregateRoot`), immutable value objects (`UserId`, `UserEmail`), and enums to ensure type safety.

### Firebase Functions Architecture

The Firebase Functions (`functions/` directory) use a simplified, pragmatic architecture:

*   **Simplicity over Abstraction**: Avoids complex patterns like Domain Events and the ValueObject pattern.
*   **Validation**: Uses direct validation functions and primitive types rather than wrapper classes.
*   **Entity Design**: Simple entity classes with validation in the constructor.
*   **Repository Pattern**: Simple repositories with direct CRUD operations.

## How to Build and Run

### Prerequisites

*   Android Studio (Ladybug or newer)
*   JDK 17
*   Firebase CLI (for functions)
*   A `google-services.json` file in the `app` module from a Firebase project.

### Build & Test Commands

*   **Full Build & Test**: `./gradlew build && ./gradlew test`
*   **Generate Debug APK**: `./gradlew assembleDebug`
*   **Run Linter**: `./gradlew lintDebug`
*   **Run Module-specific tests**: `./gradlew :domain:test`
*   **Firebase Functions tests**: `cd functions && npm test`

## High-Level Flow

1.  The app starts with `MainActivity`, which sets up the main navigation graph.
2.  The `feature_splash` module checks the user's authentication status.
3.  If not authenticated, the user is directed to the `feature_login` or `feature_signup` screens.
4.  Once authenticated, the user is taken to the `feature_main` module, which hosts the `feature_home` screen.
5.  The `feature_home` screen acts as the central hub for navigating to projects, DMs, and other features.
6.  The `NavigationManager` in `core_navigation` handles all navigation between feature modules.
7.  ViewModels in each feature module use UseCases (via UseCaseProviders) from the `domain` module to interact with data.
8.  UseCases get data from Repositories, which are implemented in the `data` module and communicate with Firebase.