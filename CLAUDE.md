# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Build Commands

### Docker Building (Cross-Platform, Recommended for WSL)

```bash
# Build Docker image (one-time setup)
docker build -f Dockerfile.android.slim -t android-kotlin-slim .

# Domain module compilation
docker run --rm android-kotlin-slim ./gradlew :domain:compileDebugKotlin --no-daemon

# Full project build
docker run --rm android-kotlin-slim ./gradlew build --no-daemon

# Generate APK
docker run --rm android-kotlin-slim ./gradlew assembleDebug --no-daemon
```

### Native Building (Windows Android Studio)

```bash
# Build the entire project
./gradlew build

# Compile specific modules
./gradlew :domain:compileDebugKotlin
./gradlew :data:compileDebugKotlin
./gradlew :core:core_common:compileDebugKotlin

# Run tests
./gradlew :domain:test
./gradlew test

# Generate APK
./gradlew assembleDebug

# Clean build
./gradlew clean build
```

### Key Development Commands

```bash
# Format code and run static analysis
./gradlew lintDebug

# Database schema validation (Room)
./gradlew :data:kspDebugKotlin

# Generate test reports
./gradlew test jacocoTestReport
```

## Project Architecture

This is a **Clean Architecture multi-module Android project** built with Kotlin, Jetpack Compose, and Firebase.

### Core Architecture Principles

- **Clean Architecture**: Clear separation of concerns with unidirectional dependencies
- **MVVM Pattern**: UI layer uses ViewModels which interact with UseCases, never directly with Repositories
- **Multi-module Structure**: Optimized for build performance and code maintainability
- **Offline-First Strategy**: Primarily uses Firestore caching with optional Room database fallback

### Module Structure

**Core Modules (`:core:`)**
- `core_common`: Shared utilities, error handling, dispatcher providers, custom Result wrapper
- `core_ui`: Reusable UI components, Material 3 theme, common Compose utilities
- `core_navigation`: Modern type-safe navigation system with `NavigationManger` interface
- `core_fcm`: Firebase Cloud Messaging implementation
- `core_logging`: Sentry-based logging abstraction

**Domain Layer (`:domain`)**
- Pure business logic, models, repository interfaces, and use cases
- Framework-independent with no external dependencies
- **UseCase Provider Pattern**: UseCases are organized into semantic groups via Provider classes

**Data Layer (`:data`)**
- Repository implementations, data sources (local/remote), DTOs, and mappers
- Firebase Firestore for primary data storage with offline caching
- Room database for advanced local storage scenarios

**Feature Modules (`:feature:`)**
- Self-contained feature implementations with UI, ViewModels, and feature-specific models
- Each feature depends on domain use cases, never directly on repositories
- Jetpack Compose UI with Material 3 design system

**App Module (`:app`)**
- Application entry point, dependency injection setup, main navigation graph
- Integrates all modules and provides global configuration

### Key Architectural Patterns

**Data Flow**: UI → ViewModel → UseCase → Repository → DataSource → External APIs/Database

**Result Handling**: Uses custom `CustomResult<S, E>` wrapper for explicit success/failure handling throughout all layers

**Navigation**: Modern type-safe navigation system via `NavigationManger` interface with direct navigation methods

**Time Management**: All server timestamps stored as UTC Instant, converted to user timezone for display

**Dependency Injection**: Hilt-based DI with module-specific injection configurations

## Architecture Guidelines

### Key Constraints

- **Repository Usage**: repository를 viewmodel에서 직접 사용하면 안돼 viewmodel에서는 UsecaseProvider와 Usecase만 사용할 수 있어.

[Rest of the existing content remains the same...]