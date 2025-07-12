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

## Build Configuration

### Current Version Information

**Core Versions:**
- **Kotlin**: 2.1.0 (with Compose support)
- **Android Gradle Plugin (AGP)**: 8.10.1
- **Gradle**: Latest compatible version
- **JVM Target**: 17
- **Compile SDK**: 36
- **Target SDK**: 36
- **Min SDK**: 29

**Key Dependencies:**
- **Jetpack Compose BOM**: 2025.06.01 (latest)
- **Hilt**: 2.56.2 (dependency injection)
- **Firebase BOM**: 33.16.0 (comprehensive Firebase integration)
- **Room**: 2.7.2 (local database)
- **Retrofit**: 3.0.0 (HTTP client)
- **OkHttp**: 4.12.0 (networking)
- **Kotlin Coroutines**: 1.10.2 (async programming)
- **Navigation Compose**: 2.9.0 (type-safe navigation)
- **Lifecycle**: 2.9.1 (Android architecture components)

**Development & Debug Tools:**
- **Sentry**: 8.14.0 (error monitoring and performance)
- **LeakCanary**: 2.12 (memory leak detection, debug only)
- **Coil**: 2.7.0 (image loading)
- **WorkManager**: 2.10.2 (background tasks)
- **Accompanist Permissions**: 0.37.3 (runtime permissions)

**Testing Dependencies:**
- **JUnit**: 4.13.2 (unit testing)
- **MockK**: 1.14.4 (Kotlin mocking)
- **Mockito**: 5.18.0 (Java mocking)
- **Espresso**: 3.6.1 (UI testing)
- **Coroutines Test**: 1.10.2 (async testing)
- **Room Testing**: 2.7.2 (database testing)

### Gradle Configuration

**Memory Settings (gradle.properties):**
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

**Build Features Enabled:**
- Jetpack Compose
- View Binding
- KSP (Kotlin Symbol Processing)
- Core Library Desugaring (Java 8+ APIs on older Android)
- Room schema location: `$projectDir/schemas`

**Proguard/R8:**
- Minification disabled in debug builds
- Release builds use default proguard rules

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

**Feature Modules (`:feature:`) - Complete List (42 modules):**

**Authentication & User Management:**
- `feature_splash`: App initialization and splash screen
- `feature_login`: User authentication interface
- `feature_signup`: User registration interface
- `feature_find_password`: Password recovery functionality
- `feature_change_password`: Password change interface
- `feature_profile`: User profile display
- `feature_edit_profile`: User profile editing
- `feature_settings`: App settings and preferences
- `feature_privacy_policy`: Privacy policy display
- `feature_terms_of_service`: Terms of service display

**Project Management:**
- `feature_main`: Main application interface and navigation
- `feature_home`: Home dashboard
- `feature_add_project`: Project creation interface
- `feature_join_project`: Project joining via invite links
- `feature_set_project_name`: Project naming interface
- `feature_project_detail`: Project information display
- `feature_project_setting`: Project configuration interface

**Project Structure Management:**
- `feature_category_edit`: Category creation and editing
- `feature_edit_category`: Category modification interface
- `feature_channel_edit`: Channel creation and editing
- `feature_edit_channel`: Channel modification interface

**Member & Role Management:**
- `feature_member_list`: Project member listing
- `feature_edit_member`: Member profile editing
- `feature_role_list`: Role management listing
- `feature_add_role`: Role creation interface
- `feature_edit_role`: Role modification interface

**Social Features:**
- `feature_friends`: Friend management interface
- `feature_accept_friend`: Friend request handling
- `feature_search`: User and content search
- `feature_chat`: Real-time messaging interface

**Schedule Management:**
- `feature_calendar`: Monthly calendar view
- `feature_calendar_24hour`: 24-hour calendar view
- `feature_add_schedule`: Schedule creation interface
- `feature_edit_schedule`: Schedule modification interface
- `feature_schedule_detail`: Schedule information display

**Development & Testing:**
- `feature_dev`: Development tools and debugging interface
- `feature_model`: Data model testing and validation

**App Module (`:app`)**
- Application entry point, dependency injection setup, main navigation graph
- Integrates all modules and provides global configuration
- Contains navigation graph definitions for all 42 feature modules

### Key Architectural Patterns

**Data Flow**: UI → ViewModel → UseCase → Repository → DataSource → External APIs/Database

**Result Handling**: Uses custom `CustomResult<S, E>` wrapper for explicit success/failure handling throughout all layers

**Navigation**: Modern type-safe navigation system via `NavigationManger` interface with direct navigation methods

**Time Management**: All server timestamps stored as UTC Instant, converted to user timezone for display

**Dependency Injection**: Hilt-based DI with module-specific injection configurations

## Architecture Guidelines

### Key Constraints

- **Repository Usage**: repository를 viewmodel에서 직접 사용하면 안돼 viewmodel에서는 UsecaseProvider와 Usecase만 사용할 수 있어.

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
- App module integrates all feature modules for navigation
- Use `AppRoutes` for all navigation route definitions
- Each feature module is self-contained with its own UI, ViewModel, and domain logic

**Time Handling:**
- Store all timestamps as UTC Instant on server
- Convert to user timezone (`ZoneId.systemDefault()`) for display
- Use `DateTimeUtil.kt` for date and time formatting

## DDD DefaultRepository Pattern

This project implements Domain-Driven Design (DDD) principles through a unified DefaultRepository pattern that separates write and read operations for consistency and maintainability.

### Core Pattern Principles

**Unified Write Operations:**
- `save(entity: AggregateRoot)` - Handles both create and update operations based on `entity.isNew` flag
- `delete(id: DocumentId)` - Delete operation
- All write operations go through a single, consistent interface

**Separated Read Operations:**
- **Basic reads**: `findById(id)`, `findAll()`, `observe(id)`, `observeAll()` (from DefaultRepository)
- **Domain-specific reads**: Custom query methods per repository (e.g., `getInvitationByCode()`, `validateInviteCode()`)

### Architecture Layers

**DefaultRepository Interface**
```kotlin
interface DefaultRepository : Repository {
    val factoryContext: DefaultRepositoryFactoryContext
    
    suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception>
    suspend fun delete(id: DocumentId): CustomResult<Unit, Exception>
    suspend fun findById(id: DocumentId, source: Source = Source.DEFAULT): CustomResult<AggregateRoot, Exception>
    suspend fun findAll(source: Source = Source.DEFAULT): CustomResult<List<AggregateRoot>, Exception>
    fun observe(id: DocumentId): Flow<CustomResult<AggregateRoot, Exception>>
    fun observeAll(): Flow<CustomResult<List<AggregateRoot>, Exception>>
}
```

**DefaultRepositoryImpl Base Class**
- Extends `DefaultRepository` with common CRUD implementation
- Handles DTO ↔ Domain conversion automatically
- Manages collection path via `ensureCollection()`
- Delegates to `DefaultDatasource` for actual Firestore operations

**DefaultDatasource Pattern**
```kotlin
interface DefaultDatasource : Datasource {
    fun setCollection(collectionPath: CollectionPath): DefaultDatasource
    suspend fun create(dto: DTO): CustomResult<DocumentId, Exception>
    suspend fun update(id: DocumentId, data: Map<String, Any?>): CustomResult<DocumentId, Exception>
    suspend fun delete(id: DocumentId): CustomResult<Unit, Exception>
    suspend fun findById(id: DocumentId, source: Source): CustomResult<DTO, Exception>
    // ... observe methods
}
```

### Factory Context Pattern

**DefaultRepositoryFactoryContext**
```kotlin
interface DefaultRepositoryFactoryContext: RepositoryFactoryContext {
    val collectionPath: CollectionPath
}

class ProjectInvitationRepositoryFactoryContext(
    override val collectionPath: CollectionPath = CollectionPath.projectInvitations()
) : DefaultRepositoryFactoryContext
```

### Implementation Example

**Domain Repository Interface**
```kotlin
interface ProjectInvitationRepository : DefaultRepository {
    override val factoryContext: ProjectInvitationRepositoryFactoryContext
    
    // Domain-specific read operations only
    suspend fun getInvitationByCode(inviteCode: InviteCode): CustomResult<ProjectInvitation, Exception>
    suspend fun validateInviteCode(inviteCode: InviteCode, userId: UserId?): CustomResult<Boolean, Exception>
    suspend fun getInviteLinksByInviter(inviterId: UserId, projectId: DocumentId?, status: InviteStatus?): Flow<CustomResult<List<ProjectInvitation>, Exception>>
}
```

**Repository Implementation**
```kotlin
class ProjectInvitationRepositoryImpl @Inject constructor(
    private val projectInvitationRemoteDataSource: ProjectInvitationRemoteDataSource,
    override val factoryContext: ProjectInvitationRepositoryFactoryContext,
) : DefaultRepositoryImpl(projectInvitationRemoteDataSource, factoryContext), ProjectInvitationRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is ProjectInvitation) 
            return CustomResult.Failure(IllegalArgumentException("Entity must be ProjectInvitation"))
        
        ensureCollection()
        return if (entity.isNew) {
            // Create via Firebase Functions
            projectInvitationRemoteDataSource.generateInviteLinkViaFunction(...)
        } else {
            // Update via Firestore
            projectInvitationRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
    
    // Implement domain-specific read methods...
}
```

### Usage Patterns

**Creating New Entities**
```kotlin
// Before (old approach)
repository.createInviteLink(projectId, inviterId, expiresInHours)

// After (DDD approach)
val invitation = ProjectInvitation.createNew(inviterId, projectId, expiresInHours)
repository.save(invitation)
```

**Updating Entities**
```kotlin
// Before (old approach)
repository.revokeInvitation(inviteCode, inviterId)

// After (DDD approach)
val invitation = repository.findById(inviteId).getOrThrow()
invitation.revoke()
repository.save(invitation)
```

**Reading Entities**
```kotlin
// Basic reads (from DefaultRepository)
repository.findById(invitationId)
repository.findAll()
repository.observe(invitationId)

// Domain-specific reads
repository.getInvitationByCode(inviteCode)
repository.validateInviteCode(inviteCode)
```

### Benefits

1. **Consistency**: All repositories follow the same CRUD patterns
2. **Separation of Concerns**: Write operations unified, read operations domain-specific
3. **DDD Compliance**: Aggregate lifecycle managed through domain entities
4. **Maintainability**: Common logic in base classes, specific logic in implementations
5. **Testability**: Clear interfaces and dependency injection throughout

## UseCase Provider Pattern

This project uses a Provider Pattern for organizing and managing UseCases, promoting better maintainability and consistent dependency injection.

### Provider Organization

**Auth Domain Providers (5 providers):**
- `AuthSessionUseCaseProvider`: Login, logout, session management (4 UseCases)
- `AuthRegistrationUseCaseProvider`: Sign-up, email verification (4 UseCases)
- `AuthPasswordUseCaseProvider`: Password reset, validation (5 UseCases)
- `AuthAccountUseCaseProvider`: Account deletion, reactivation (3 UseCases)
- `AuthValidationUseCaseProvider`: Email/nickname validation, error messages (5 UseCases)

**Project Domain Providers (6 providers):**
- `CoreProjectUseCaseProvider`: Project CRUD operations (11+ UseCases)
- `ProjectStructureUseCaseProvider`: Category management, structure operations (10 UseCases)
- `ProjectChannelUseCaseProvider`: Channel management (7 UseCases)
- `ProjectMemberUseCaseProvider`: Member management (10 UseCases)
- `ProjectRoleUseCaseProvider`: Role and permissions management (8 UseCases)
- `ProjectAssetsUseCaseProvider`: Asset management

**Other Domain Providers (9 providers):**
- `UserUseCaseProvider`: User management and profile operations
- `FriendUseCaseProvider`: Friend relationship management
- `DMUseCaseProvider`: Direct messaging functionality
- `ChatUseCaseProvider`: Chat and messaging operations
- `ScheduleUseCaseProvider`: Schedule and calendar management
- `SearchUseCaseProvider`: Search functionality across entities
- `FileManagementUseCaseProvider`: File upload and management
- `ValidationUseCaseProvider`: General validation operations
- `ContextDependentUseCaseProvider`: Context-specific operations

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

**Provider Implementation with Repository Factory Pattern:**
```kotlin
@Singleton
class AuthSessionUseCaseProvider @Inject constructor(
    private val authRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>,
    private val userRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<UserRepositoryFactoryContext, UserRepository>
) {
    
    fun create(): AuthSessionUseCases {
        val authRepository = authRepositoryFactory.create(AuthRepositoryFactoryContext())
        val userRepository = userRepositoryFactory.create(UserRepositoryFactoryContext(CollectionPath.users))

        return AuthSessionUseCases(
            loginUseCase = LoginUseCase(authRepository, userRepository),
            logoutUseCase = LogoutUseCase(authRepository),
            checkAuthenticationStatusUseCase = CheckAuthenticationStatusUseCaseImpl(userRepository, authRepository),
            checkSessionUseCase = CheckSessionUseCase(authRepository),
            
            // Direct repository access included
            authRepository = authRepository,
            userRepository = userRepository
        )
    }
    
    // Context-aware factory methods for project-specific operations
    fun createForProject(projectId: DocumentId): AuthSessionUseCases {
        // Project-specific repository contexts
        return create() // Simplified example
    }
    
    fun createForCurrentUser(): AuthSessionUseCases {
        // Current user context
        return create() // Simplified example
    }
}

data class AuthSessionUseCases(
    val loginUseCase: LoginUseCase,
    val logoutUseCase: LogoutUseCase,
    val checkAuthenticationStatusUseCase: CheckAuthenticationStatusUseCaseImpl,
    val checkSessionUseCase: CheckSessionUseCase,
    
    // Repositories for direct access when needed
    val authRepository: AuthRepository,
    val userRepository: UserRepository
)
```

### Key Provider Implementation Features

**1. Repository Factory Integration:**
- Uses `RepositoryFactory<Context, Repository>` pattern instead of direct injection
- Repository contexts contain `CollectionPath` for Firestore document organization
- Multiple factory types with `@JvmSuppressWildcards` annotations for Hilt compatibility

**2. Context-Aware Creation Methods:**
- `create()` - General context (for non-project entities)
- `createForProject(projectId: DocumentId)` - Project-specific repositories
- `createForCurrentUser()` - Current user context
- `createForMember(projectId, memberId)` - Member-specific context

**3. UseCase Groups Include Repositories:**
The data classes include both UseCases and their underlying repositories for direct access when needed:
```kotlin
data class CoreProjectUseCases(
    // UseCases
    val createProjectUseCase: CreateProjectUseCase,
    val deleteProjectUseCase: DeleteProjectUseCase,
    // ...
    
    // Direct Repository access
    val authRepository: AuthRepository,
    val projectRepository: ProjectRepository
)
```

**4. Project-Specific Collection Paths:**
```kotlin
val memberRepository = memberRepositoryFactory.create(
    MemberRepositoryFactoryContext(
        collectionPath = CollectionPath.projectMembers(projectId.value)
    )
)
```

### Real-World Usage Patterns

**Context-Dependent Creation (Most Common):**
```kotlin
@HiltViewModel 
class ProjectDetailViewModel @Inject constructor(
    private val projectMemberUseCaseProvider: ProjectMemberUseCaseProvider,
    private val projectRoleUseCaseProvider: ProjectRoleUseCaseProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val projectId = savedStateHandle.get<String>("projectId")?.let { DocumentId(it) }
        ?: throw IllegalArgumentException("Project ID is required")
    
    // Context-aware UseCase creation for project-specific operations
    private val memberUseCases = projectMemberUseCaseProvider.createForProject(projectId)
    private val roleUseCases = projectRoleUseCaseProvider.createForProject(projectId)
    
    fun loadMembers() {
        viewModelScope.launch {
            memberUseCases.getProjectMembersUseCase().collect { result ->
                // Handle member list
            }
        }
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

### Test Architecture

**Comprehensive Test Pyramid Implementation:**
```
                    ┌─────────────────┐
                    │   UI Tests      │
                    │   (Espresso)    │
                    └─────────────────┘
              ┌─────────────────────────────┐
              │    Integration Tests        │
              │  (androidTest directory)    │
              └─────────────────────────────┘
        ┌─────────────────────────────────────────┐
        │           Unit Tests                    │
        │       (test directory)                  │
        └─────────────────────────────────────────┘
```

### Test Structure by Module

**Unit Tests (`src/test/java/`):**
- **ViewModels**: Mock use case providers, test state changes
- **UseCases**: Mock repositories, test business logic
- **Repositories**: Mock data sources, test data transformation
- **Utils**: Test utility functions and extensions
- **Domain Models**: Test entity behavior and validation

**Integration Tests (`src/androidTest/java/`):**
- **Database**: Room database operations and migrations
- **Repository**: Real Firestore integration tests
- **UI Components**: Compose component testing
- **Navigation**: End-to-end navigation flows

**UI Tests (Feature modules):**
- **Screen Tests**: Complete user interaction flows
- **Component Tests**: Individual Compose component behavior
- **Permission Tests**: Runtime permission handling

### Testing Tools & Frameworks

**Unit Testing:**
```kotlin
// MockK for Kotlin-first mocking
@Test
fun `login should emit success when credentials are valid`() {
    // Given
    val mockAuthRepository = mockk<AuthRepository>()
    every { mockAuthRepository.login(any(), any()) } returns 
        flow { emit(CustomResult.Success(mockUser)) }
    
    // When & Then
    runTest {
        loginUseCase(email, password).test {
            assertEquals(CustomResult.Success(mockUser), awaitItem())
            awaitComplete()
        }
    }
}
```

**Fake Repository Pattern:**
```kotlin
class FakeAuthRepository : AuthRepository {
    private val users = mutableMapOf<String, User>()
    var shouldFail = false
    
    override suspend fun login(email: String, password: String): CustomResult<User, Exception> {
        return if (shouldFail) {
            CustomResult.Failure(Exception("Login failed"))
        } else {
            users[email]?.let { CustomResult.Success(it) }
                ?: CustomResult.Failure(Exception("User not found"))
        }
    }
}
```

**ViewModel Testing Pattern:**
```kotlin
@Test
fun `viewModel should handle login correctly`() = runTest {
    // Given
    val mockUseCaseProvider = mockk<AuthSessionUseCaseProvider>()
    val mockUseCases = mockk<AuthSessionUseCases>()
    every { mockUseCaseProvider.create() } returns mockUseCases
    
    val viewModel = LoginViewModel(mockUseCaseProvider, navigationManager)
    
    // When
    viewModel.onLoginClick("test@example.com", "password")
    
    // Then
    verify { mockUseCases.loginUseCase.invoke("test@example.com", "password") }
}
```

### Test Commands

**Basic Test Execution:**
```bash
# Run all unit tests
./gradlew test

# Run specific module tests
./gradlew :feature:feature_chat:test
./gradlew :domain:test
./gradlew :data:test

# Run integration tests
./gradlew connectedAndroidTest

# Run specific feature integration tests
./gradlew :feature:feature_chat:connectedAndroidTest
```

**Advanced Test Commands:**
```bash
# Run with coverage reports
./gradlew testDebugUnitTest jacocoTestDebugUnitTestReport

# Continuous testing (watch mode)
./gradlew test --continuous

# Parallel test execution
./gradlew test --parallel --max-workers=4

# Run specific test class
./gradlew test --tests="*AuthRepositoryTest"

# Run tests with system property
./gradlew test -Dtest.profile=integration
```

### Testing Patterns & Best Practices

**1. UseCase Testing:**
```kotlin
class GetProjectDetailsUseCaseTest {
    @MockK
    private lateinit var projectRepository: ProjectRepository
    
    private lateinit var useCase: GetProjectDetailsUseCase
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        useCase = GetProjectDetailsUseCase(projectRepository)
    }
    
    @Test
    fun `should return project when repository returns success`() = runTest {
        // Test implementation
    }
}
```

**2. Repository Testing with Fake Data Sources:**
```kotlin
class ProjectRepositoryImplTest {
    private lateinit var fakeRemoteDataSource: FakeProjectRemoteDataSource
    private lateinit var fakeLocalDataSource: FakeProjectLocalDataSource
    private lateinit var repository: ProjectRepositoryImpl
    
    @BeforeEach
    fun setup() {
        fakeRemoteDataSource = FakeProjectRemoteDataSource()
        fakeLocalDataSource = FakeProjectLocalDataSource()
        repository = ProjectRepositoryImpl(fakeRemoteDataSource, fakeLocalDataSource)
    }
}
```

**3. Compose UI Testing:**
```kotlin
@Test
fun loginScreen_displaysErrorWhenLoginFails() {
    composeTestRule.setContent {
        LoginScreen(
            uiState = LoginUiState.Error("Invalid credentials"),
            onLoginClick = { _, _ -> }
        )
    }
    
    composeTestRule
        .onNodeWithText("Invalid credentials")
        .assertIsDisplayed()
}
```

### Test Configuration Files

**Key Test Utilities:**
- `CoroutinesTestRule`: Coroutine testing setup
- `FlowTestExtensions`: Flow testing helpers
- `TestUtilities`: Common test data builders
- `MockProviders`: Centralized mock creation

**Test Requirements:**
- **100% UseCase coverage** (business logic critical)
- **90%+ ViewModel coverage** (UI state management)
- **80%+ Repository coverage** (data layer reliability)
- **Critical UI flows** must have integration tests
- **Error scenarios** must be thoroughly tested
- **Performance tests** for data-heavy operations

## Development Environment Setup

### Prerequisites

**Required Software:**
- **Android Studio**: Latest stable version (Ladybug or newer)
- **JDK**: OpenJDK 17 or newer (required for AGP 8.10.1)
- **Git**: For version control
- **Node.js**: 18+ (for Firebase Functions development)

**Optional but Recommended:**
- **Docker Desktop**: For cross-platform builds
- **Firebase CLI**: For cloud functions deployment
- **Sentry CLI**: For error monitoring setup

### Android Studio Setup

**1. SDK Configuration:**
```bash
# Ensure Android SDK path is set correctly in local.properties
# For Windows:
sdk.dir=C\\:\\\\Users\\\\[username]\\\\AppData\\\\Local\\\\Android\\\\Sdk

# Required SDK components:
# - Android API 36 (compileSdk)
# - Android API 29+ (minSdk) 
# - Build Tools 34.0.0+
# - Android SDK Platform-Tools
# - Android SDK Command-line Tools
```

**2. IDE Configuration:**
```gradle
# File -> Settings -> Build, Execution, Deployment -> Gradle
# - Gradle JVM: Use Project JDK (17+)
# - Use Gradle from: gradle-wrapper.properties file
# - Build and run using: Gradle
```

**3. Memory Settings:**
```
# Android Studio VM Options (Help -> Edit Custom VM Options)
-Xmx8g
-XX:MaxMetaspaceSize=512m
-XX:+UseG1GC
```

### Firebase Project Setup

**1. Create Firebase Project:**
- Go to [Firebase Console](https://console.firebase.google.com/)
- Create new project with name matching your app
- Enable Authentication, Firestore, Storage, Functions, App Check

**2. Download Configuration:**
```bash
# Download google-services.json from Firebase Console
# Place in: app/google-services.json
```

**3. Configure Firebase Services:**
```json
// Required Firebase services configuration:
{
  "authentication": {
    "providers": ["email/password"],
    "email_verification": true
  },
  "firestore": {
    "rules": "firestore.rules",
    "indexes": "firestore.indexes.json",
    "region": "asia-northeast3"
  },
  "storage": {
    "rules": "storage.rules",
    "region": "asia-northeast3"  
  },
  "functions": {
    "region": "asia-northeast3",
    "runtime": "nodejs18"
  }
}
```

### Sentry Configuration

**1. Create Sentry Account:**
- Register at [Sentry.io](https://sentry.io)
- Create new project for Android
- Get your DSN and auth token

**2. Configure Sentry:**
```properties
# sentry.properties
auth.token=your_sentry_auth_token_here
org=your_organization_name
```

**3. Add to build.gradle.kts:**
```kotlin
sentry {
    org.set("your_org")
    projectName.set("android")
    includeSourceContext.set(true)
}
```

### Environment Variables

**Required Environment Variables:**
```bash
# Windows (set in System Environment Variables)
ANDROID_SDK_ROOT=C:\Users\[username]\AppData\Local\Android\Sdk
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.x.x-hotspot

# Optional for CI/CD
FIREBASE_TOKEN=your_firebase_ci_token
SENTRY_AUTH_TOKEN=your_sentry_auth_token
```

### Firebase Functions Setup (Optional)

**1. Install Firebase CLI:**
```bash
npm install -g firebase-tools
firebase login
```

**2. Initialize Functions:**
```bash
cd functions
npm install
npm run build
```

**3. Deploy Functions:**
```bash
firebase deploy --only functions
```

### First Build Verification

**1. Sync and Build:**
```bash
# Open project in Android Studio
# File -> Sync Project with Gradle Files
# Build -> Make Project

# Or via command line:
./gradlew clean build
```

**2. Run Tests:**
```bash
./gradlew test
./gradlew :domain:test
./gradlew :data:test
```

**3. Generate APK:**
```bash
./gradlew assembleDebug
```

### Common Setup Issues & Solutions

**OutOfMemoryError during build:**
```gradle
# Increase Gradle heap size in gradle.properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8

# Or use Gradle daemon
org.gradle.daemon=true
org.gradle.parallel=true
```

**KSP compilation errors:**
```bash
# Clean and rebuild with KSP
./gradlew clean
./gradlew :data:kspDebugKotlin
./gradlew build
```

**Firebase connection issues:**
```bash
# Verify google-services.json placement
# Check Firebase project settings match applicationId
# Ensure Firebase services are enabled in console
```

**Hilt dependency injection errors:**
```kotlin
// Ensure @HiltAndroidApp annotation on Application class
// Verify @AndroidEntryPoint on Activities/Fragments
// Check Hilt annotation processor is working
```

### Development Workflow

**1. Branch Management:**
```bash
# Create feature branch
git checkout -b feature/your-feature-name

# Regular commits with meaningful messages
git commit -m "feat: add user profile editing functionality"
```

**2. Pre-commit Checklist:**
```bash
# Run linting
./gradlew lintDebug

# Run tests
./gradlew test

# Build verification
./gradlew assembleDebug

# Format code (Android Studio: Ctrl+Alt+L)
```

**3. Code Review Guidelines:**
- UseCase Provider pattern compliance
- Proper error handling with CustomResult
- Test coverage for new features
- Navigation via NavigationManger interface
- Repository Factory usage consistency

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

### Firebase-Specific Considerations

**Firestore Integration**
- Design entities to work seamlessly with Firestore's document model
- Minimize data transformation between entities and Firestore documents
- Use Firestore Timestamps appropriately for date fields
- Leverage Firestore's offline caching instead of implementing separate local storage

**Cloud Function Environment**
- Remember functions are stateless - no persistent event handling needed
- Optimize for cold start performance (avoid heavy initialization)
- Use Firebase Admin SDK efficiently
- Handle authentication via Firebase Auth context

### Validation Guidelines

**Input Validation**
- Validate at entity construction time
- Use shared validation utilities from `core/validation.ts`
- Throw appropriate error types (`ValidationError`, `ConflictError`, etc.)
- Keep validation logic simple and focused

**Error Handling**
- Use `CustomResult<T, E>` pattern for operation results
- Provide clear, actionable error messages
- Handle Firebase-specific errors appropriately
- Don't over-abstract error handling

### Testing Strategy

**Focus Areas**
- Test business logic, not architectural abstractions
- Validate Firestore integration correctness
- Test error handling and edge cases
- Mock Firebase services for unit tests

**Avoid Testing**
- ValueObject wrapper functionality (eliminated)
- Domain event generation/handling (eliminated)
- Over-complex factory patterns

### Migration Notes

When working with existing code that violates these principles:
1. Remove domain event systems completely
2. Replace ValueObjects with validation functions and primitive types
3. Simplify repository factories to direct injection
4. Update tests to focus on actual business logic

This approach ensures Firebase Functions remain maintainable, performant, and focused on their primary responsibility: providing efficient server-side functionality for the mobile application.