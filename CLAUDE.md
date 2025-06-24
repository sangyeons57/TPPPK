# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Development Environment Setup

### Docker-Based Development (Recommended for WSL/Cross-Platform)

**Prerequisites Checklist:**
1. ✅ Docker Desktop installed and running
2. ✅ WSL2 enabled (for Windows users)  
3. ✅ Docker Desktop WSL2 integration enabled
4. ✅ At least 8GB RAM allocated to Docker
5. ✅ At least 20GB free disk space

**Quick Environment Check:**
```bash
# Verify Docker is running
docker --version
docker info

# If Docker is not running, start Docker Desktop first
# Windows: Start Docker Desktop application
# Linux: sudo systemctl start docker
```

### Essential Build Commands

#### Docker-Based Building (Cross-Platform Compatible)

```bash
# Quick build test using optimized Dockerfile
docker build -f Dockerfile.android.slim -t android-kotlin-slim .

# Test domain module compilation
docker run --rm android-kotlin-slim ./gradlew :domain:compileDebugKotlin --no-daemon

# Full project build in Docker
docker run --rm android-kotlin-slim ./gradlew build --no-daemon

# Generate APK in Docker environment
docker run --rm android-kotlin-slim ./gradlew assembleDebug --no-daemon
```

#### Native Building (Windows/Local Android SDK)

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

#### Module-Specific Build Commands

```bash
# Domain module (core business logic)
# Docker: 
docker run --rm android-kotlin-slim ./gradlew :domain:compileDebugKotlin --no-daemon
# Native: 
.\gradlew :domain:compileDebugKotlin

# Data module (repository implementations)
# Docker:
docker run --rm android-kotlin-slim ./gradlew :data:compileDebugKotlin --no-daemon
# Native:
.\gradlew :data:compileDebugKotlin

# Core modules (shared utilities)
# Docker:
docker run --rm android-kotlin-slim ./gradlew :core:core_common:compileDebugKotlin --no-daemon
# Native:
.\gradlew :core:core_common:compileDebugKotlin

# Feature modules (UI components)
# Docker:
docker run --rm android-kotlin-slim ./gradlew :feature:feature_main:compileDebugKotlin --no-daemon
# Native:
.\gradlew :feature:feature_main:compileDebugKotlin
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
- `core_navigation`: Modern type-safe navigation system with `NavigationManger` interface and
  `TypeSafeRoute` architecture
- `core_fcm`: Firebase Cloud Messaging implementation
- `core_logging`: Sentry-based logging abstraction

**Domain Layer (`:domain`)**

- Pure business logic, models, repository interfaces, and use cases
- Framework-independent with no external dependencies
- Use cases implement single responsibility principle and cannot call other use cases
- **UseCase Provider Pattern**: UseCases are organized into semantic groups via Provider classes for better maintainability and consistent DI integration

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

**UseCase Provider Pattern**: UseCases are grouped semantically into Provider classes that handle repository creation and UseCase instantiation. ViewModels inject Providers instead of individual UseCases.

**Result Handling**: Uses custom `CustomResult<S, E>` wrapper for explicit success/failure handling
throughout all layers

**Navigation**: Modern type-safe navigation system via `NavigationManger` interface with direct
navigation methods, `TypeSafeRoute` system, and dedicated `NavigationResultManager`

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

- ViewModels MUST use UseCase Providers, never individual UseCases or Repositories directly
- Use Cases cannot call other Use Cases - delegate shared logic to Repositories
- All network/database operations return `CustomResult<Success, Error>`
- Navigation uses `NavigationManger` interface, never `NavController` directly
- UseCases are organized into semantic groups (5-8 UseCases per Provider) for maintainability

**Module Dependencies:**

- Feature modules depend only on domain and core modules
- Data layer implements domain interfaces
- Domain layer has no dependencies on framework code
- Use `AppRoutes` for all navigation route definitions

**Time Handling:**

- Store all timestamps as UTC Instant on server
- Convert to user timezone (`ZoneId.systemDefault()`) for display
- Use `LocalDateTime`, `ZonedDateTime` appropriately per context
- Use `DateTimeUtil.kt` for date and time formatting

## Common Development Workflows

**Adding a New Feature:**

1. Define routes in `AppRoutes`
2. Create use cases in appropriate semantic domain directory
3. Add UseCases to relevant Provider or create new Provider if needed
4. Implement repository if needed in data layer
5. Create feature module with UI and ViewModel using Providers
6. Add navigation integration in app module
7. Write comprehensive tests for UseCases and ViewModels

**Navigation Implementation:**

1. Define route in `AppRoutes.kt`
2. Use `NavigationManger.navigateTo()` in ViewModels
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
- `ProjectAssetsUseCaseProvider`: File uploads, media management (1 UseCase)

### UseCase Directory Structure

```
/domain/usecase/
├── auth/
│   ├── session/        # Login, logout, session checks
│   ├── registration/   # Sign-up, email verification
│   ├── password/       # Password reset, validation
│   ├── account/        # Account deletion, reactivation
│   └── validation/     # Email/nickname validation
├── project/
│   ├── core/           # Project CRUD operations
│   ├── structure/      # Category and structure management
│   ├── channel/        # Channel operations
│   ├── member/         # Member management
│   ├── role/           # Role and permissions
│   ├── assets/         # File uploads and media
│   └── category/       # Category domain operations
├── user/               # User profile operations
├── dm/                 # Direct messaging
├── friend/             # Friend relationships
├── schedule/           # Calendar and scheduling
└── search/             # Search functionality
```

### Provider Usage Pattern

**In ViewModels:**
```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider
) : ViewModel() {
    
    // Provider creates UseCase group with repositories
    private val authUseCases = authSessionUseCaseProvider.create()
    
    fun login(email: String, password: String) {
        authUseCases.loginUseCase(email, password)
    }
}
```

**Provider Implementation Pattern:**
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

### Adding New UseCases

1. Create UseCase in appropriate semantic directory
2. Add to relevant Provider class
3. Update Provider's UseCases data class
4. Register Provider in `UseCaseProviderModule` if new
5. Update ViewModel to use Provider

## Docker Development Environment (Detailed)

### Docker Configuration Files

This project includes optimized Docker configurations for consistent cross-platform development:

- **`Dockerfile.android.slim`**: Lightweight Android build environment (recommended)
- **`quick-docker-test.bat`**: Windows batch script for quick Docker testing
- **`docker-compose.android.yml`**: Service orchestration for complex builds

### Docker Environment Setup Steps

**1. Initial Docker Setup:**
```bash
# Check Docker installation
docker --version

# If Docker is not installed:
# Windows: Download Docker Desktop from docker.com
# Linux: sudo apt-get install docker.io docker-compose
# macOS: Download Docker Desktop from docker.com

# Start Docker Desktop (Windows/macOS)
# Linux users: sudo systemctl start docker
```

**2. WSL2 Integration (Windows Users):**
```bash
# Enable WSL2 integration in Docker Desktop:
# Settings > Resources > WSL Integration > Enable integration

# Verify WSL2 Docker access:
wsl --list --verbose
docker run hello-world
```

**3. Build Environment Setup:**
```bash
# Navigate to project root
cd /mnt/d/repository/repository_java/Android/TeamnovaPersonalProjectProjectingKotlin

# Build optimized Android development image
docker build -f Dockerfile.android.slim -t android-kotlin-slim .

# Verify build environment
docker run --rm android-kotlin-slim ./gradlew --version
```

### Module-Specific Docker Strategies

**Domain Module Testing:**
```bash
# Quick domain compilation test
docker run --rm android-kotlin-slim ./gradlew :domain:compileDebugKotlin --no-daemon

# Domain tests
docker run --rm android-kotlin-slim ./gradlew :domain:test --no-daemon

# Domain with dependency verification
docker run --rm android-kotlin-slim ./gradlew :domain:dependencies --no-daemon
```

**Data Module with Room Database:**
```bash
# Data module with annotation processing
docker run --rm android-kotlin-slim ./gradlew :data:kspDebugKotlin --no-daemon

# Data module compilation
docker run --rm android-kotlin-slim ./gradlew :data:compileDebugKotlin --no-daemon
```

**Core Modules (Parallel Build):**
```bash
# Build all core modules simultaneously
docker run --rm android-kotlin-slim ./gradlew \
  :core:core_common:compileDebugKotlin \
  :core:core_ui:compileDebugKotlin \
  :core:core_navigation:compileDebugKotlin \
  --parallel --no-daemon
```

**Feature Modules (Individual):**
```bash
# Specific feature module
docker run --rm android-kotlin-slim ./gradlew :feature:feature_main:compileDebugKotlin --no-daemon

# All feature modules
docker run --rm android-kotlin-slim ./gradlew compileDebugKotlin -x :app:compileDebugKotlin --no-daemon
```

### Performance Optimization

**Docker Build Caching:**
```bash
# Use BuildKit for faster builds
export DOCKER_BUILDKIT=1

# Build with caching
docker build -f Dockerfile.android.slim -t android-kotlin-slim . --progress=plain

# Pre-download dependencies for faster subsequent builds
docker run --rm android-kotlin-slim ./gradlew dependencies --no-daemon
```

**Memory and CPU Allocation:**
- Recommended: 8GB RAM, 4 CPU cores for Docker Desktop
- Minimum: 6GB RAM, 2 CPU cores
- Storage: 20GB+ free space for Android SDK and build cache

### Troubleshooting Guide

#### Common Docker Issues

**🚨 "Docker daemon not running"**
```bash
# Solution:
# Windows/macOS: Start Docker Desktop application
# Linux: sudo systemctl start docker

# Verify Docker is running:
docker info
```

**🚨 "Permission denied" in WSL**
```bash
# Add user to docker group (Linux/WSL):
sudo usermod -aG docker $USER
newgrp docker

# Test permission:
docker run hello-world
```

**🚨 "Out of disk space" during build**
```bash
# Clean Docker resources:
docker system prune -a

# Remove unused volumes:
docker volume prune

# Check disk usage:
docker system df
```

**🚨 "Android SDK not found" in Docker**
```bash
# This should be handled by Dockerfile.android.slim
# If error persists, rebuild image:
docker build -f Dockerfile.android.slim -t android-kotlin-slim . --no-cache

# Verify SDK installation:
docker run --rm android-kotlin-slim ls -la /opt/android-sdk
```

#### Build-Specific Issues

**🚨 "Could not resolve dependencies"**
```bash
# Solution 1: Clean and retry
docker run --rm android-kotlin-slim ./gradlew clean dependencies --no-daemon

# Solution 2: Force refresh dependencies
docker run --rm android-kotlin-slim ./gradlew build --refresh-dependencies --no-daemon
```

**🚨 "Compilation errors in Providers"**
```bash
# Known issue: Provider import path errors
# Check these files for incorrect package imports:
# - domain/src/main/java/com/example/domain/provider/auth/*.kt
# - Look for import mismatches like:
#   Wrong: com.example.domain.usecase.auth.account.DeleteAuthUserUseCase
#   Correct: com.example.domain.usecase.auth.DeleteAuthUserUseCase

# Quick compilation check:
docker run --rm android-kotlin-slim ./gradlew :domain:compileDebugKotlin --no-daemon
```

**🚨 "Gradle daemon issues"**
```bash
# Always use --no-daemon in Docker to avoid memory issues
# If needed, stop all Gradle daemons:
docker run --rm android-kotlin-slim ./gradlew --stop
```

#### Environment-Specific Troubleshooting

**WSL2 Environment:**
```bash
# Check WSL version:
wsl --status

# Verify WSL Docker integration:
docker run --rm hello-world

# If WSL path issues occur:
# Use Docker bind mounts consistently
# Avoid mixing Windows and WSL paths
```

**Windows Native Environment:**
```bash
# If Android Studio works but Docker fails:
# 1. Check Docker Desktop is using WSL2 backend
# 2. Verify WSL integration is enabled
# 3. Use quick-docker-test.bat for validation

# Run quick test:
quick-docker-test.bat
```

### Pre-Execution Checklist

Before starting development, ensure:

1. **✅ Docker Status**
   ```bash
   docker info | grep "Server Version"
   ```

2. **✅ Available Resources**
   ```bash
   docker system df
   # Ensure sufficient space available
   ```

3. **✅ Network Connectivity**
   ```bash
   docker run --rm alpine ping -c 1 google.com
   ```

4. **✅ Android SDK Installation**
   ```bash
   docker run --rm android-kotlin-slim ls /opt/android-sdk/platform-tools
   ```

5. **✅ Gradle Wrapper Permissions**
   ```bash
   docker run --rm android-kotlin-slim ls -la ./gradlew
   ```

### Quick Start Commands Summary

```bash
# 1. Build Docker image (one-time setup)
docker build -f Dockerfile.android.slim -t android-kotlin-slim .

# 2. Test domain module (most common)
docker run --rm android-kotlin-slim ./gradlew :domain:compileDebugKotlin --no-daemon

# 3. Full project build
docker run --rm android-kotlin-slim ./gradlew build --no-daemon

# 4. Quick validation script (Windows)
quick-docker-test.bat
```

## Navigation System

This project uses a modern, type-safe navigation architecture built around three core components:

### Navigation Components

**NavigationManger Interface**
- Primary navigation API for feature modules
- Provides direct navigation methods instead of command patterns
- Injected via Hilt into ViewModels and Composables

```kotlin
interface NavigationManger {
    fun navigateBack(): Boolean
    fun navigateTo(route: TypeSafeRoute, navOptions: NavOptions? = null)
    fun navigateToProjectDetails(projectId: String, navOptions: NavOptions? = null)
    fun navigateToChat(channelId: String, messageId: String? = null, navOptions: NavOptions? = null)
    // Additional direct navigation methods...
}
```

**TypeSafeRoute System**
- Compile-time type safety for navigation arguments
- Uses Kotlinx Serialization for route generation
- Eliminates string-based navigation errors

```kotlin
@Serializable
sealed interface TypeSafeRoute

@Serializable
data class ProjectDetailRoute(val projectId: String) : TypeSafeRoute

@Serializable
data class ChatRoute(val channelId: String, val messageId: String? = null) : TypeSafeRoute
```

**NavigationResultManager**
- Dedicated lifecycle-aware result handling using SavedStateHandle
- Type-safe result passing between screens
- Automatic cleanup of consumed results

```kotlin
// Setting results
navigationResultManager.setResult(navController, "project_created", project)

// Observing results in Composables
ObserveNavigationResult(navController, navigationResultManager, "project_created") { project ->
    // Handle result
}
```

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
    // Direct usage in UI events
    DebouncedBackButton(onClick = {
        navigationManger.navigateBack()
    })
}
```

**Route Registration in AppNavigationGraph.kt:**
```kotlin
// Modern pattern with type-safe argument extraction
safeComposable(
    route = AppRoutes.Project.settingsRoute(),
    arguments = projectArguments()
) { backStackEntry ->
    val args = backStackEntry.extractProjectArguments()
    ProjectSettingScreen(
        projectId = args.projectId,
        navigationManger = navigationManger
    )
}
```

### Migration from Legacy Navigation

The navigation system has been modernized from a command-based architecture. Key changes:

- **Removed**: NavigationCommand pattern (was over-engineered)
- **Added**: Direct navigation methods in NavigationManger interface
- **Improved**: Type-safe routes with Kotlinx Serialization
- **Enhanced**: Dedicated result handling with NavigationResultManager