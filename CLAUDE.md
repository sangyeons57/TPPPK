# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Build Commands

### Docker Building (Recommended for WSL)
```bash
# Build Docker image (one-time setup)
docker build -f Dockerfile.android.slim -t android-kotlin-slim .

# Full project build
docker run --rm android-kotlin-slim ./gradlew build --no-daemon

# Generate APK
docker run --rm android-kotlin-slim ./gradlew assembleDebug --no-daemon
```

### Native Building
```bash
# Build and test
./gradlew build
./gradlew test

# Generate APK
./gradlew assembleDebug

# Lint and validation
./gradlew lintDebug
./gradlew :data:kspDebugKotlin
```

## Build Configuration

### Core Versions
- **Kotlin**: 2.1.0 | **AGP**: 8.10.1 | **JVM**: 17
- **Compile/Target SDK**: 36 | **Min SDK**: 29
- **Compose BOM**: 2025.06.01 | **Hilt**: 2.56.2
- **Firebase BOM**: 33.16.0 | **Room**: 2.7.2

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

**Domain Layer (`:domain`)**
- Pure business logic, models, repository interfaces, and use cases
- Framework-independent with no external dependencies
- **UseCase Provider Pattern**: UseCases are organized into semantic groups via Provider classes

**Data Layer (`:data`)**
- Repository implementations, data sources (local/remote), DTOs, and mappers
- Firebase Firestore for primary data storage with offline caching
- Room database for advanced local storage scenarios

**App API Module (`:app_api`)**
- API interfaces that the app module implements
- Shared contracts between app and feature modules

**Feature Modules (42 total):**
- **Auth**: splash, login, signup, password recovery, profile, settings
- **Project**: main, home, add/join project, project details/settings
- **Structure**: category/channel creation and editing
- **Members & Roles**: member/role management and listing
- **Social**: friends, chat, search
- **Schedule**: calendar views, schedule CRUD
- **Dev/Test**: development tools and model testing

**App Module**: Entry point, DI setup, navigation integration

### Key Patterns

**Data Flow**: UI → ViewModel → UseCase → Repository → DataSource → APIs/Database

**Result Handling**: `CustomResult<S, E>` wrapper for explicit success/failure handling

**Navigation**: Type-safe navigation via `NavigationManger` interface

**DI**: Hilt-based with UseCase Provider pattern

## Architecture Rules

- ViewModels MUST use UseCase Providers, never Repositories directly
- UseCases cannot call other UseCases - delegate to Repositories  
- All operations return `CustomResult<Success, Error>`
- Navigation uses `NavigationManger`, never `NavController` directly
- Feature modules depend only on domain and core modules
- Store timestamps as UTC Instant, display in user timezone

## DDD DefaultRepository Pattern

Unified pattern separating write and read operations:

**Write Operations**: `save(entity)`, `delete(id)` - consistent interface
**Read Operations**: Basic (`findById`, `observe`) + domain-specific queries

**Implementation**: `DefaultRepositoryImpl` handles CRUD + DTO conversion, delegates to `DefaultDatasource` for Firestore operations

**Usage**: 
```kotlin
// Create/Update
val entity = ProjectInvitation.createNew(...)
repository.save(entity)

// Read
repository.findById(id)
repository.getInvitationByCode(code) // domain-specific
```

## UseCase Provider Pattern

Organized UseCases into semantic groups via Provider classes:

**Providers**: Auth (5), Project (6), Other domains (9) - Session, Registration, Password, etc.

**Usage**: ViewModels inject providers, call `create()` or `createForProject(id)` for context-specific operations

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

## Navigation & Development

**Navigation**: Type-safe via `NavigationManger` interface, `TypeSafeRoute` system, `NavigationResultManager` for result passing

**Adding Features**:
1. Define routes in `AppRoutes`
2. Create UseCases in domain, add to Provider
3. Implement repository if needed
4. Create feature module with UI/ViewModel using Providers
5. Add navigation integration
6. Write tests

**Result Handling**:
```kotlin
// Repository/UseCase
suspend fun getData(): CustomResult<Data, Exception>

// ViewModel
useCase().onSuccess { data -> 
    // Handle success 
}.onFailure { error -> 
    // Handle error 
}
```

## Testing

**Test Structure**: Unit tests (ViewModels, UseCases, Repositories), Integration tests (Database, UI), UI tests (Espresso)

**Tools**: MockK, JUnit, Espresso, Fake Repository pattern

**Commands**:
```bash
./gradlew test                    # All unit tests
./gradlew :domain:test           # Module tests
./gradlew connectedAndroidTest   # Integration tests
```

**Requirements**: 100% UseCase coverage, 90%+ ViewModel, 80%+ Repository

## Setup & Troubleshooting

**Prerequisites**: Android Studio (Ladybug+), JDK 17+, Docker (optional), Firebase CLI, Sentry

### JDK 17 Setup (Hybrid Approach)

**Automatic JDK Management**: The project uses a hybrid approach for maximum compatibility:

1. **First Priority**: Detects locally installed JDK 17
2. **Fallback**: Automatically downloads JDK 17 if not found locally

**Option 1 - Local Installation (Recommended):**
```bash
# Via SDKMAN (recommended for cross-platform)
curl -s "https://get.sdkman.io" | bash
sdk install java 17.0.12-tem

# Windows via Chocolatey
choco install openjdk17

# macOS via Homebrew  
brew install openjdk@17

# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# Arch Linux
sudo pacman -S jdk17-openjdk
```

**Option 2 - Automatic Download**: If no local JDK 17 is found, Gradle will automatically download and cache JDK 17 using the foojay-resolver plugin.

**Docker Environment**: JDK 17 is pre-installed, uses local detection.

**First Build**:
1. Download `google-services.json` to `app/`
2. `./gradlew clean build`
3. `./gradlew test`

**Common Issues**:
- **Memory**: Increase heap size in `gradle.properties`
- **KSP**: `./gradlew clean && ./gradlew :data:kspDebugKotlin`
- **Firebase**: Verify `google-services.json` placement and enabled services
- **Hilt**: Check `@HiltAndroidApp` and `@AndroidEntryPoint` annotations

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

**Hilt Compilation Issues:**
```bash
# Clean Hilt generated code
./gradlew clean
./gradlew :app:kspDebugKotlin
./gradlew build
```

**Firebase Connection Problems:**
```bash
# Verify google-services.json is correct
# Check package name matches Firebase project
# Ensure Firebase services are enabled
```


# Using Gemini CLI for Large Codebase Analysis

When analyzing large codebases or multiple files that might exceed context limits, use the Gemini CLI with its massive
context window. Use `gemini -p` to leverage Google Gemini's large context capacity.

## File and Directory Inclusion Syntax

Use the `@` syntax to include files and directories in your Gemini prompts. The paths should be relative to WHERE you run the
  gemini command:

### Examples:

**Single file analysis:**
gemini -p "@src/main.py Explain this file's purpose and structure"

Multiple files:
gemini -p "@package.json @src/index.js Analyze the dependencies used in the code"

Entire directory:
gemini -p "@src/ Summarize the architecture of this codebase"

Multiple directories:
gemini -p "@src/ @tests/ Analyze test coverage for the source code"

Current directory and subdirectories:
gemini -p "@./ Give me an overview of this entire project"

# Or use --all_files flag:
gemini --all_files -p "Analyze the project structure and dependencies"

Implementation Verification Examples

Check if a feature is implemented:
gemini -p "@src/ @lib/ Has dark mode been implemented in this codebase? Show me the relevant files and functions"

Verify authentication implementation:
gemini -p "@src/ @middleware/ Is JWT authentication implemented? List all auth-related endpoints and middleware"

Check for specific patterns:
gemini -p "@src/ Are there any React hooks that handle WebSocket connections? List them with file paths"

Verify error handling:
gemini -p "@src/ @api/ Is proper error handling implemented for all API endpoints? Show examples of try-catch blocks"

Check for rate limiting:
gemini -p "@backend/ @middleware/ Is rate limiting implemented for the API? Show the implementation details"

Verify caching strategy:
gemini -p "@src/ @lib/ @services/ Is Redis caching implemented? List all cache-related functions and their usage"

Check for specific security measures:
gemini -p "@src/ @api/ Are SQL injection protections implemented? Show how user inputs are sanitized"

Verify test coverage for features:
gemini -p "@src/payment/ @tests/ Is the payment processing module fully tested? List all test cases"

When to Use Gemini CLI

Use gemini -p when:
- Analyzing entire codebases or large directories
- Comparing multiple large files
- Need to understand project-wide patterns or architecture
- Current context window is insufficient for the task
- Working with files totaling more than 100KB
- Verifying if specific features, patterns, or security measures are implemented
- Checking for the presence of certain coding patterns across the entire codebase

Important Notes

- Paths in @ syntax are relative to your current working directory when invoking gemini
- The CLI will include file contents directly in the context
- No need for --yolo flag for read-only analysis
- Gemini's context window can handle entire codebases that would overflow Claude's context
- When checking implementations, be specific about what you're looking for to get accurate results

## Firebase Functions Architecture Guidelines

This project includes Firebase Functions (located in `/functions/`) that follow a simplified, pragmatic architecture approach distinct from the Android client architecture.

### Core Principles

**Simplicity Over Abstraction**
- Prioritize direct, readable code over complex architectural patterns
- Avoid over-engineering for TypeScript/JavaScript server-side environment
- Focus on Firestore integration efficiency and Firebase ecosystem compatibility

**Forbidden Patterns**
- **Domain Events**: Do NOT implement domain event systems in Firebase Functions
  - Events add complexity without business value in stateless cloud functions
  - No event handlers or processing infrastructure exists
  - Use direct method calls for side effects instead
- **ValueObject Pattern**: Do NOT use ValueObject wrapper classes
  - Creates unnecessary complexity for simple validation
  - Poor integration with Firestore's JSON-based storage model
  - Use validation functions and primitive types instead

### Recommended Patterns

**Validation Strategy**
```typescript
// ❌ DON'T: ValueObject wrappers
export class Email {
  constructor(public readonly value: string) {
    if (!isValidEmail(value)) throw new Error('Invalid email');
  }
}

// ✅ DO: Direct validation functions
export function validateEmail(email: string): void {
  if (!isValidEmail(email)) {
    throw new ValidationError('email', 'Invalid email format');
  }
}

// ✅ DO: Type aliases for better type safety
export type UserId = string;
export type ProjectId = string;
```

**Entity Design**
```typescript
// ✅ DO: Simple entities with primitive types
export class UserEntity {
  constructor(
    public readonly id: string,
    public readonly email: string,    // Direct primitive
    public readonly name: string,     // Direct primitive
    // ... other fields
  ) {
    // Validate in constructor
    validateEmail(email);
    validateUsername(name);
  }

  toData(): UserData {
    return {
      id: this.id,
      email: this.email,  // Direct access, no .value unwrapping
      name: this.name,    // Direct access, no .value unwrapping
      // ...
    };
  }
}
```

**Repository Pattern**
- Keep repositories simple with direct CRUD operations
- Avoid complex factory patterns for basic operations
- Use constructor injection instead of factory contexts where possible

