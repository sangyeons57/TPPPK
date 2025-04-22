# Android Project Structure Analysis: Projecting Kotlin

## 1. Architecture and Design Patterns

This project utilizes the following primary architectural and design patterns:

* **Clean Architecture:** Employs a layered architecture for separation of concerns.
* **Domain Layer (`domain/`):** Contains core business logic, entities (plain Kotlin data classes), and repository interfaces (contracts). Framework-independent.
* **Data Layer (`data/`):** Implements the Domain Layer's repository interfaces. Handles interactions with data sources (network APIs, local DB) and includes data-specific models (DTOs, Entities).
* **Presentation Layer (Feature Packages `feature_*`/):** Manages the UI and UI state. Consists of Jetpack Compose Screens and ViewModels. Depends on the Domain Layer.
* **MVVM (Model-View-ViewModel):** Each screen (View) interacts with a ViewModel responsible for managing UI state (`UiState`) and handling user events (`Event`). ViewModels fetch data via Repositories and update the `UiState`.
* **Repository Pattern:** Abstracts data source implementations (Data Layer) from ViewModels using interfaces defined in the Domain Layer.
* **Dependency Injection (Hilt):** Manages dependency creation and injection (e.g., Repositories into ViewModels), promoting loose coupling and testability.

## 2. Core Technology Stack

* **Language:** Kotlin
* **UI:** Jetpack Compose
* **Architecture:** Clean Architecture, MVVM
* **Asynchronous Programming:** Kotlin Coroutines & Flow
* **Navigation:** Jetpack Navigation Compose
* **Dependency Injection:** Hilt
* **Networking:** (Estimated) Retrofit or Ktor (Requires checking API service definitions)
* **Local Database:** (Estimated) Room (Based on `FriendEntity` in Data Layer)
* **Backend/Auth:** (Estimated) Firebase Authentication, Firestore (Based on `AuthRepositoryImpl`, `FriendRepositoryImpl`)

## 3. Folder Structure

The project follows a layered architecture and feature-based modularization:

``` Folder
main/
├── AndroidManifest.xml # Core app config, permissions, components
├── java/ or kotlin/
│ └── com/example/teamnovapersonalprojectprojectingkotlin/
│     ├── MainActivity.kt # Main entry point Activity (hosting Compose)
│     ├── MyApp.kt # Custom Application class (Hilt setup)
│     ├── data/ # Data Layer
│     │   ├── di/ # Hilt DI Modules (RepositoryModule, FirebaseModule)
│     │   ├── model/ # Data layer models (DTOs, Entities - e.g., FriendDto, FriendEntity)
│     │   ├── repository/ # Implementations of Domain Repository interfaces (e.g., AuthRepositoryImpl)
│     │   └── source/ # Data sources (Remote API Service, Local DAO)
│     │       └── remote/ # Remote data source (e.g., FriendApiService)
│     ├── domain/ # Domain Layer
│     │   ├── model/ # Core business models (e.g., User, Project, ChatMessage, Role)
│     │   └── repository/ # Data access interfaces (e.g., AuthRepository, ProjectRepository)
│     ├── feature_auth/ # Authentication Feature Module (Presentation Layer)
│     │   ├── ui/ # Auth-related Composable screens (e.g., LoginScreen, SignUpScreen)
│     │   └── viewmodel/ # Auth screen ViewModels (e.g., LoginViewModel, SignUpViewModel)
│     ├── feature_chat/ # Chat Feature Module
│     ├── feature_dev/ # Development Menu Feature Module
│     ├── feature_friends/ # Friends Feature Module
│     ├── feature_main/ # Main Screens Feature Module (incl. Bottom Navigation)
│     ├── feature_profile/ # User Profile Feature Module
│     ├── feature_project/ # Project Creation/Joining Feature Module
│     ├── feature_project_members/ # Project Member Management Feature Module
│     ├── feature_project_roles/ # Project Role Management Feature Module
│     ├── feature_project_setting/ # Project Settings Feature Module
│     ├── feature_project_structure/ # Project Structure (Category/Channel) Management Feature Module
│     ├── feature_schedule/ # Schedule/Calendar Feature Module
│     ├── feature_search/ # Search Feature Module
│     ├── feature_settings/ # User Settings Feature Module
│     ├── navigation/ # Navigation definitions (AppDestination, AppNavigation)
│     └── ui/ # Common UI elements
│         └── theme/ # Compose Theme (Color, Shape, Theme, Type)
└── res/ # Resource folder
    ├── drawable/ # Drawable resources (icons, etc.)
    ├── mipmap/ # Launcher icons
    ├── values/ # Strings, colors, themes, etc.
    └── xml/ # Backup rules, etc.
```

* **`feature_*` Packages:** Organize code by application feature. Each typically contains `ui` (Composable UI) and `viewmodel` (UI logic and state) sub-packages.

## 4. Key Components

* **Navigation:**
  * `navigation/AppDestination.kt`: Defines all navigation routes and arguments type-safely using a Sealed Interface. Includes bottom navigation destinations.
  * `navigation/AppNavigation.kt`: Uses `NavHost` to map routes from `AppDestination` to Composable functions, managing the overall navigation flow. Likely uses nested navigation within `MainScreen` for bottom tabs.
* **Dependency Injection (Hilt):**
  * `MyApp.kt` (`@HiltAndroidApp`): Application class for Hilt setup.
  * `MainActivity.kt` (`@AndroidEntryPoint`): Enables DI in the Activity.
  * `feature_*/viewmodel/*ViewModel.kt` (`@HiltViewModel`, `@Inject`): ViewModel creation and dependency injection (e.g., Repositories).
  * `data/di/*.kt` (`@Module`, `@Provides`, `@Binds`): Hilt modules providing dependencies like Repository implementations and Firebase services.
* **UI State Management (MVVM):**
  * **ViewModel:** Manages UI state and handles business logic for each screen/feature (`feature_*/viewmodel/*ViewModel.kt`).
  * **UiState:** A `data class` holding the data needed by the UI, exposed via `StateFlow` from the ViewModel. The UI observes this state using `collectAsStateWithLifecycle`.
  * **Event:** A `sealed class` representing one-time UI events (navigation, snackbars), exposed via `SharedFlow` from the ViewModel.

## 5. Common Pattern Examples

### 5.1. MVVM & UI State Management

ViewModel exposes UI state via `StateFlow` and one-time events via `SharedFlow`. The UI (Screen) collects the state and triggers ViewModel functions on user interaction.

```kotlin
// --- ViewModel ---
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val repository: ExampleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExampleUiState())
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ExampleEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.getData()
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, data = result.getOrThrow()) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Error") }
                _eventFlow.emit(ExampleEvent.ShowSnackbar("Failed to load data"))
            }
        }
    }

    fun onButtonClick() {
        viewModelScope.launch {
            // ... logic ...
            _eventFlow.emit(ExampleEvent.NavigateToDetail)
        }
    }
}

// --- UI State & Event ---
data class ExampleUiState(
    val isLoading: Boolean = false,
    val data: String? = null,
    val error: String? = null
)

sealed class ExampleEvent {
    object NavigateToDetail : ExampleEvent()
    data class ShowSnackbar(val message: String) : ExampleEvent()
}

// --- Screen (Composable) ---
@Composable
fun ExampleScreen(viewModel: ExampleViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ExampleEvent.NavigateToDetail -> { /* Handle navigation */ }
                is ExampleEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    // ... UI rendering based on uiState ...
}
```

### 5.2. Repository Pattern

Define interfaces in the Domain Layer and implement them in the Data Layer. ViewModels depend on the interfaces.

```kotlin
// --- domain/repository/UserRepository.kt ---
interface UserRepository {
    suspend fun getUserProfile(): Result<UserProfile>
    suspend fun updateUserName(newName: String): Result<Unit>
}

// --- data/repository/UserRepositoryImpl.kt ---
class UserRepositoryImpl @Inject constructor(
    private val userApiService: UserApiService, // e.g., Retrofit
    private val userDao: UserDao // e.g., Room
) : UserRepository {
    // ... implementation using apiService and dao ...
}

// --- data/di/RepositoryModule.kt ---
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}

// --- ViewModel ---
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository // Depends on interface
) : ViewModel() {
    // ... use userRepository ...
}
```

### 5.3. Hilt Dependency Injection

Uses annotations like @HiltAndroidApp, @AndroidEntryPoint, @HiltViewModel, @Inject, @Module, @Provides, @Binds (See data/di/ and component classes).

## 6. Programming Conventions (Estimated)

These conventions are estimated based on the provided code files. A formal style guide might exist for the project.

* **Naming Conventions:**
  * **Packages:** lower_case_with_underscores (e.g., feature_auth, viewmodel, data.repository).
  * **Files (.kt):** PascalCase.kt (e.g., LoginScreen.kt, UserRepository.kt, AppDestination.kt).
  * **Classes, Interfaces, Objects, Enums:** PascalCase (e.g., MainActivity, UserRepository, FirebaseModule, UserStatus, LoginUiState, LoginEvent).
    * UI State classes often end with UiState.
    * Event sealed classes often end with Event.
    * ViewModel classes often end with ViewModel.
    * Repository implementations often end with Impl.
  * **Functions (Methods):** camelCase (e.g., onCreate, loadUserProfile, onEmailChange).
  * **Composable Functions:** PascalCase (e.g., LoginScreen, ProfileContent, DayCell). Follows standard Jetpack Compose conventions.
  * **Variables & Properties:** camelCase (e.g., projectId, uiState, snackbarHostState, _eventFlow). Private mutable state flows often use a leading underscore (_uiState).
  * **Constants (Top-level or Companion Object):** (Assumed) SCREAMING_SNAKE_CASE (e.g., const val DEFAULT_TIMEOUT = 5000L).
* **Formatting:**
  * **Indentation:** Standard 4 spaces (inferred, typical for Kotlin/Android Studio).
  * **Line Length:** (Cannot verify precisely) Likely follows standard Kotlin style guide limits (around 100-120 characters).
* **Commenting:**
  * Uses // for single-line comments.
  * Uses /** ... */ for KDoc documentation comments (observed on some classes/functions).
  * Comments are used to explain purpose, TODOs, and sometimes temporarily disable code.
  * Comments are written in Korean

## \!\!\! 7. LLM Constraints and Guidelines \!\!\!

**This section defines the core principles the LLM must follow when interacting with this project. It is critically important.**

* **Purpose-First Thinking:** Understand the user's actual goal first, then initiate thinking in a way best suited to achieve that goal.
* **'Why' Analysis:** Before generating code or explanations, analyze and understand the fundamental reason 'why it's needed', then provide the optimal solution.
* **Prioritize Simplicity and Clarity:** Prefer understandable and clear code structures over complex optimization techniques.
* **Long-Term Perspective:** Value long-term maintainability (considering architectural structure) and scalability more than immediate implementation feasibility.
* **Value-Based Suggestions:** While aiming to fulfill user requests is the primary goal, proactively suggest better architectures, patterns, or implementation methods if available.
* **Meaning-Centric Generation:** Go beyond simply creating the requested feature; consider the meaning and impact of that feature within the overall project context during generation.
* **Maintain Consistency:** Remember and consider the context and flow of the previous conversation to provide consistent responses and suggestions.
* **Plan-Then-Code:** When generating code, always create a plan first, then write the code according to that plan.
* **API Usage:** Always refer to official documentation for API usage by searching the internet.

**Document Update Guide:**

* **On Project Structure Changes:** If significant changes occur to the overall project structure – such as adding new feature modules (feature_*), altering the core architecture (Clean Architecture, MVVM), or replacing major libraries (Hilt, Navigation Compose) – **this document (project_structure_md) must be updated.**

* **How to Update:**
    1. Modify the relevant sections (e.g., Folder Structure, Architecture, Tech Stack, Key Components) to reflect the changes.
    2. Add new sections or reorganize existing ones if necessary.
    3. Clearly describe the changes, as they might affect the LLM's understanding.
