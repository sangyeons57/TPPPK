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

## Code Conventions

**Architecture Rules:**
- ViewModels MUST use UseCase Providers, never individual UseCases or Repositories directly
- Use Cases cannot call other Use Cases - delegate shared logic to Repositories
- All network/database operations return `CustomResult<Success, Error>`
- Navigation uses `NavigationManger` interface, never `NavController` directly

**Module Dependencies:**
- Feature modules depend only on domain and core modules
- Data layer implements domain interfaces
- Domain layer has no dependencies on framework code
- Use `AppRoutes` for all navigation route definitions

**Time Handling:**
- Store all timestamps as UTC Instant on server
- Convert to user timezone (`ZoneId.systemDefault()`) for display
- Use `DateTimeUtil.kt` for date and time formatting

## UseCase Provider Pattern

This project uses a Provider Pattern for organizing and managing UseCases, promoting better maintainability and consistent dependency injection.

### Provider Organization

**Auth Domain Providers:**
- `AuthSessionUseCaseProvider`: Login, logout, session management (4 UseCases)
- `AuthRegistrationUseCaseProvider`: Sign-up, email verification (4 UseCases)
- `AuthPasswordUseCaseProvider`: Password reset, validation (5 UseCases)
- `AuthAccountUseCaseProvider`: Account deletion, reactivation (3 UseCases)
- `AuthValidationUseCaseProvider`: Email/nickname validation, error messages (5 UseCases)

**Project Domain Providers:**
- `CoreProjectUseCaseProvider`: Project CRUD operations (7 UseCases)
- `ProjectStructureUseCaseProvider`: Category management, structure operations (10 UseCases)
- `ProjectChannelUseCaseProvider`: Channel management (7 UseCases)
- `ProjectMemberUseCaseProvider`: Member management (7 UseCases)
- `ProjectRoleUseCaseProvider`: Role and permissions management (8 UseCases)

### Provider Usage Pattern

**In ViewModels:**
```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider
) : ViewModel() {
    
    private val authUseCases = authSessionUseCaseProvider.create()
    
    fun login(email: String, password: String) {
        authUseCases.loginUseCase(email, password)
    }
}
```

**Provider Implementation:**
```kotlin
@Singleton
class AuthSessionUseCaseProvider @Inject constructor(
    private val authRepositoryFactory: RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {
    fun create(): AuthSessionUseCases {
        val authRepository = authRepositoryFactory.create(AuthRepositoryFactoryContext())
        
        return AuthSessionUseCases(
            loginUseCase = LoginUseCase(authRepository),
            logoutUseCase = LogoutUseCase(authRepository),
            // ... other UseCases
            authRepository = authRepository
        )
    }
}
```

## Navigation System

This project uses a modern, type-safe navigation architecture built around three core components:

### Navigation Components

**NavigationManger Interface**
- Primary navigation API for feature modules
- Provides direct navigation methods instead of command patterns
- Injected via Hilt into ViewModels and Composables

**TypeSafeRoute System**
- Compile-time type safety for navigation arguments
- Uses Kotlinx Serialization for route generation
- Eliminates string-based navigation errors

**NavigationResultManager**
- Dedicated lifecycle-aware result handling using SavedStateHandle
- Type-safe result passing between screens
- Automatic cleanup of consumed results

### Navigation Usage Patterns

**In ViewModels:**
```kotlin
@HiltViewModel
class AddProjectViewModel @Inject constructor(
    private val navigationManger: NavigationManger
) : ViewModel() {
    
    fun onProjectCreated() {
        navigationManger.navigateBack()
    }
    
    fun navigateToProjectDetails(projectId: String) {
        navigationManger.navigateToProjectDetails(projectId)
    }
}
```

**In Composables:**
```kotlin
@Composable
fun AddProjectScreen(
    navigationManger: NavigationManger,
    viewModel: AddProjectViewModel = hiltViewModel()
) {
    DebouncedBackButton(onClick = {
        navigationManger.navigateBack()
    })
}
```

## Common Development Workflows

**Adding a New Feature:**
1. Define routes in `AppRoutes`
2. Create use cases in appropriate semantic domain directory
3. Add UseCases to relevant Provider or create new Provider if needed
4. Implement repository if needed in data layer
5. Create feature module with UI and ViewModel using Providers
6. Add navigation integration in app module
7. Write comprehensive tests for UseCases and ViewModels

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

## Testing Strategy

**Test Locations by Module:**
- Unit tests: `src/test/java/`
- Integration tests: `src/androidTest/java/`
- UI tests: Feature modules with Compose testing

**Test Commands:**
```bash
# Run all tests
./gradlew test

# Run tests for specific feature
./gradlew :feature:feature_chat:test

# Run with coverage
./gradlew testDebugUnitTest jacocoTestDebugUnitTestReport
```

**Key Testing Requirements:**
- All new features must include comprehensive unit tests
- ViewModels should be tested with mock use cases
- Use cases should be tested with mock repositories
- Repository implementations should have integration tests

## Troubleshooting

**Common Issues:**

**Docker not available:**
```bash
# Check Docker Desktop is running (Windows)
docker --version
docker info

# If Docker Desktop is not running, start it from Windows
# Then retry Docker build commands
```

**SDK Path Error (Native builds):**
```bash
# For Windows Android Studio: 
# local.properties should have Windows path:
sdk.dir=C\:\\Users\\[username]\\AppData\\Local\\Android\\Sdk

# For WSL/Docker builds: 
# Docker creates its own local.properties with internal SDK path
```

**Build Permission Issues:**
```bash
# Clean and rebuild
./gradlew clean
./gradlew build

# Or use Docker (no permission issues)
docker run --rm android-kotlin-slim ./gradlew clean build --no-daemon
```

**Compilation Errors:**
```bash
# Check specific module (Native)
./gradlew :domain:compileDebugKotlin

# Or use Docker
docker run --rm android-kotlin-slim ./gradlew :domain:compileDebugKotlin --no-daemon
```