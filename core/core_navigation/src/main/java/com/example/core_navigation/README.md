# Navigation System Documentation

## Overview

이 문서는 Projecting Kotlin 앱의 네비게이션 시스템 사용법을 설명합니다. 이 시스템은 타입 안전하고 모듈화된 네비게이션을 제공하며, 다음과 같은 기능을 포함합니다:

- 타입 안전한 화면 간 이동
- 인자 전달 및 결과 수신
- 딥링크 처리
- 네비게이션 이벤트 추적 (Sentry 통합)
- 중첩 네비게이션 지원

## 주요 컴포넌트

### 1. NavigationCommand

네비게이션 작업을 캡슐화하는 인터페이스입니다.

```kotlin
sealed interface NavigationCommand {
    object NavigateBack : NavigationCommand
    data class Navigate(val route: String) : NavigationCommand
    data class NavigateWithClearBackStack(val route: String) : NavigationCommand
    // 기타 명령들...
}
```

### 2. NavigationHandler

`NavigationCommand`를 실행하는 인터페이스입니다.

```kotlin
interface NavigationHandler {
    fun execute(command: NavigationCommand)
}
```

### 3. NavigationManager

상위 수준 네비게이션 기능을 제공하는 클래스입니다.

```kotlin
class NavigationManager(private val handler: NavigationHandler) {
    fun navigateTo(route: String) { ... }
    fun navigateBack() { ... }
    fun navigateAndClearBackStack(route: String) { ... }
    // 기타 메서드들...
}
```

## 사용 방법

### 1. 화면 간 이동

ViewModel에서:

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val navigationManager: NavigationManager
) : ViewModel() {
    fun navigateToDetail(itemId: String) {
        navigationManager.navigateToItemDetail(itemId)
    }
}
```

Composable에서:

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    Button(onClick = { viewModel.navigateToDetail("123") }) {
        Text("상세 보기")
    }
}
```

### 2. 결과 수신

ViewModel에서:

```kotlin
@HiltViewModel
class ParentViewModel @Inject constructor(
    private val navigationManager: NavigationManager
) : ViewModel() {
    init {
        // 결과 관찰
        navigationManager.observeResult<Boolean>("refresh_needed") { shouldRefresh ->
            if (shouldRefresh) {
                refreshData()
            }
        }
    }
    
    fun navigateToEdit() {
        navigationManager.navigateWithResult("edit_screen", "refresh_needed")
    }
}
```

결과 설정:

```kotlin
@HiltViewModel
class EditViewModel @Inject constructor(
    private val navigationManager: NavigationManager
) : ViewModel() {
    fun saveAndReturn() {
        // 저장 로직...
        navigationManager.setResult("refresh_needed", true)
        navigationManager.navigateBack()
    }
}
```

### 3. 중첩 네비게이션

MainScreen에서:

```kotlin
@Composable
fun MainScreen(
    parentManager: NavigationManager,
    nestedFactory: NestedNavigationManagerFactory
) {
    val nestedNavController = rememberNavController()
    val nestedManager = remember(nestedNavController) { 
        nestedFactory.create(nestedNavController) 
    }
    
    // 나머지 UI 구현...
}
```

## 확장성

새로운 목적지를 추가하려면:

1. `AppDestinations.kt`에 새 목적지 클래스 추가
2. `NavigationManager`에 해당 목적지로 이동하는 메서드 추가
3. `AppNavigation.kt`에 새 경로 추가

## Sentry 통합

네비게이션 이벤트는 자동으로 Sentry에 추적됩니다:

```kotlin
// MainActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val navController = rememberNavController()
    SentryNavigationTracker.registerNavigationListener(navController)
}
```

사용자 액션 추적:

```kotlin
fun onButtonClick() {
    SentryNavigationTracker.trackUserAction("button_click", "로그인 버튼 클릭")
}
```

## 모범 사례

1. 경로 문자열 대신 `AppDestinations`의 함수 사용하기
2. ViewModel에서 네비게이션 로직 처리하기
3. `NavigationManager`를 통해 화면 간 이동하기
4. 결과 전달에는 `navigateWithResult`와 `setResult` 사용하기
5. 중첩 네비게이션이 필요한 경우 `NestedNavigationManagerFactory` 활용하기 