# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Essential Build Commands

### Building and Testing

```bash
# Build the entire project
.\gradlew build

# Run tests for a specific module (replace with actual module name)
.\gradlew :feature:feature_main:test

# Run tests with detailed output
.\gradlew :feature:feature_main:test --info

# Clean and rebuild
.\gradlew clean build

# Check for compilation issues
.\gradlew compileDebugKotlin

# Generate APK for testing
.\gradlew assembleDebug
```

### Key Development Commands

```bash
# Format code and run static analysis (if configured)
.\gradlew lintDebug

# Database schema validation (Room)
.\gradlew :data:kspDebugKotlin

# Generate test reports
.\gradlew test jacocoTestReport
```

## Project Architecture

This is a **Clean Architecture multi-module Android project** built with Kotlin, Jetpack Compose,
and Firebase.

### Core Architecture Principles

- **Clean Architecture**: Clear separation of concerns with unidirectional dependencies
- **MVVM Pattern**: UI layer uses ViewModels which interact with UseCases, never directly with
  Repositories
- **Multi-module Structure**: Optimized for build performance and code maintainability
- **Offline-First Strategy**: Primarily uses Firestore caching with optional Room database fallback

### Module Structure

**Core Modules (`:core:`)**

- `core_common`: Shared utilities, error handling, dispatcher providers, custom Result wrapper
- `core_ui`: Reusable UI components, Material 3 theme, common Compose utilities
- `core_navigation`: Navigation architecture using custom `AppNavigator` interface and
  `NavigationManager`
- `core_fcm`: Firebase Cloud Messaging implementation
- `core_logging`: Sentry-based logging abstraction

**Domain Layer (`:domain`)**

- Pure business logic, models, repository interfaces, and use cases
- Framework-independent with no external dependencies
- Use cases implement single responsibility principle and cannot call other use cases

**Data Layer (`:data`)**

- Repository implementations, data sources (local/remote), DTOs, and mappers
- Firebase Firestore for primary data storage with offline caching
- Room database for advanced local storage scenarios (currently minimal usage)

**Feature Modules (`:feature:`)**

- Self-contained feature implementations with UI, ViewModels, and feature-specific models
- Each feature depends on domain use cases, never directly on repositories
- Jetpack Compose UI with Material 3 design system

**App Module (`:app`)**

- Application entry point, dependency injection setup, main navigation graph
- Integrates all modules and provides global configuration

### Key Architectural Patterns

**Data Flow**: UI → ViewModel → UseCase → Repository → DataSource → External APIs/Database

**Result Handling**: Uses custom `CustomResult<S, E>` wrapper for explicit success/failure handling
throughout all layers

**Navigation**: Centralized navigation via `AppNavigator` interface with `NavigationManager`
implementation, routes defined in `AppRoutes`

**Time Management**: All server timestamps stored as UTC Instant, converted to user timezone for
display

**Dependency Injection**: Hilt-based DI with module-specific injection configurations

## Firebase Integration

**Primary Services:**

- Firestore: Main NoSQL database with offline caching as primary local storage strategy
- Authentication: User management and session handling
- Storage: File and media storage
- Cloud Messaging: Push notifications via `core_fcm` module

**Hybrid Chat Architecture:**

- Real-time messaging via WebSocket server
- Message persistence and history via Firestore
- Offline chat history through Firestore disk cache

## Testing Strategy

**Test Locations by Module:**

- Unit tests: `src/test/java/`
- Integration tests: `src/androidTest/java/`
- UI tests: Feature modules with Compose testing

**Test Commands:**

```bash
# Run all tests
.\gradlew test

# Run tests for specific feature
.\gradlew :feature:feature_chat:test

# Run with coverage
.\gradlew testDebugUnitTest jacocoTestDebugUnitTestReport
```

**Key Testing Requirements:**

- All new features must include comprehensive unit tests
- ViewModels should be tested with mock use cases
- Use cases should be tested with mock repositories
- Repository implementations should have integration tests

## Code Conventions

**Architecture Rules:**

- ViewModels MUST use Use Cases, never Repositories directly
- Use Cases cannot call other Use Cases - delegate shared logic to Repositories
- All network/database operations return `CustomResult<Success, Error>`
- Navigation uses `AppNavigator` interface, never `NavController` directly

**Module Dependencies:**

- Feature modules depend only on domain and core modules
- Data layer implements domain interfaces
- Domain layer has no dependencies on framework code
- Use `AppRoutes` for all navigation route definitions

**Time Handling:**

- Store all timestamps as UTC Instant on server
- Convert to user timezone (`ZoneId.systemDefault()`) for display
- Use `LocalDateTime`, `ZonedDateTime` appropriately per context

## Common Development Workflows

**Adding a New Feature:**

1. Define routes in `AppRoutes`
2. Create use cases in domain layer
3. Implement repository if needed in data layer
4. Create feature module with UI and ViewModel
5. Add navigation integration in app module
6. Write comprehensive tests

**Navigation Implementation:**

1. Define route in `AppRoutes.kt`
2. Use `AppNavigator.navigateTo()` in ViewModels
3. Handle parameters via `SavedStateHandle` extensions
4. Register routes in appropriate `NavHost`

**Result Handling Pattern:**

```kotlin
// In Repository
suspend fun getData(): CustomResult<DataModel, Exception>

// In UseCase  
suspend operator fun invoke(): CustomResult<DomainModel, Exception>

// In ViewModel
viewModelScope.launch {
    useCase().onSuccess { data ->
        // Handle success
    }.onFailure { error ->
        // Handle error
    }
}
```