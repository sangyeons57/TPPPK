# 네비게이션 구조 문제 분석 및 해결 계획

## 1. 현재 문제 분석

- [x] 1.1: AppNavigationGraph 구조 변경으로 인한 문제점 파악
- [x] 1.2: MainScreen 중첩 네비게이션 구조 분석
- [x] 1.3: HomeScreen과 ProjectDetail 간의 연결 문제 분석

### 분석 결과

#### 1.1 AppNavigationGraph 구조 변경으로 인한 문제점

1. **이전 구조**:
   - 원래의 AppNavigationGraph는 **중첩된 네비게이션 그래프**를 사용했습니다.
   - 예: `navigation(route = AppRoutes.Main.Home.GRAPH_ROOT, startDestination = AppRoutes.Main.Home.ROOT_CONTENT)`
   - 이 구조에서는 Home 내부에 Project 관련 화면들이 포함되어 있었습니다.

2. **현재 구조**:
   - 현재 AppNavigationGraph는 **단순화된 플랫 구조**를 사용합니다.
   - 모든 최상위 화면들이 동일한 수준에 위치합니다.
   - `MainScreen`이 독립적인 중첩 네비게이션을 사용합니다.
   - Project 관련 화면들이 Home 내부가 아닌 최상위 수준에 배치되었습니다.

3. **문제점**:
   - HomeScreen에서 ProjectDetail로 이동할 때, 현재 중첩 네비게이션이 이를 처리하지 못합니다.
   - ProjectDetail 화면이 네비게이션 그래프의 다른 부분(MainScreen 외부)에 있어 직접 접근이 어렵습니다.

#### 1.2 MainScreen 중첩 네비게이션 구조 분석

1. **현재 구현**:
   - `MainScreen.kt`는 자체적으로 중첩된 `NavHost`를 가지고 있습니다.
   - 이 내부 NavHost는 Home, Calendar, Profile 세 개의 탭을 처리합니다.
   - 중첩 NavController를 NavigationHandler에 등록하는 DisposableEffect 사용:
     ```kotlin
     DisposableEffect(nestedNavController) {
         navigationHandler.setChildNavController(nestedNavController)
         onDispose {
             navigationHandler.setChildNavController(null)
         }
     }
     ```

2. **문제점**:
   - MainScreen의 중첩 NavController는 Bottom Navigation의 탭 전환만 처리하고, 외부 화면(ProjectDetail 등)으로의 이동은 처리하지 못합니다.
   - NavigationManager에서 `navigateToProjectDetails` 메서드가 구현되어 있지만, 이는 최상위 NavController를 사용하여 별도의 화면으로 이동합니다.

#### 1.3 HomeScreen과 ProjectDetail 간의 연결 문제 분석

1. **현재 동작**:
   - HomeScreen에서 프로젝트 클릭 시 `HomeEvent.NavigateToProjectDetails` 이벤트가 발생합니다.
   - 이 이벤트는 `navigationHandler.navigateToProjectDetails(event.projectId)`를 통해 처리됩니다.
   - 그러나 MainContent 부분에서는 프로젝트 상세 정보를 보여주는 자체 UI 영역이 있습니다:
     ```kotlin
     MainContent(uiState: HomeUiState) {
         when (uiState.selectedTopSection) {
             TopSection.PROJECTS -> {
                 Surface(...) {
                     Column(...) {
                         Text("프로젝트 상세 정보")
                         Text("선택된 프로젝트의 상세 정보가 표시됩니다.")
                     }
                 }
             }
             ...
         }
     }
     ```
   - 즉, **UI에서는 상세 정보를 같은 화면에 표시하는 설계**인데, **코드는 별도 화면으로 이동하는 로직**을 가지고 있습니다.

2. **두 가지 접근법 간의 충돌**:
   - 방법 1: 프로젝트 리스트를 Home 안에 두고, 화면 내 상세 표시 (현재 UI 설계)
   - 방법 2: 프로젝트 상세를 별도 화면으로 분리 (현재 네비게이션 코드)

3. **본질적인 디자인 의사결정 필요**:
   - 프로젝트 목록과 상세 정보를 단일 화면에 분할 표시할지
   - 또는 두 개의 별도 화면으로 구현할지에 대한 결정이 필요합니다.

## 2. 해결 방안 설계

- [x] 2.1: 그래프 구조 설계 개선 (Navigation Graph 계층 재구성)
- [x] 2.2: MainScreen 중첩 네비게이션 처리 계획 수립
- [x] 2.3: HomeScreen에서 ProjectDetail로의 네비게이션 설계

### 설계 방안

두 가지 방안을 검토한 결과, **하이브리드 접근 방식**이 가장 적합합니다:

#### 2.1 그래프 구조 개선 방안

1. **중첩 그래프 구조 복원**:
   - 원래 구조처럼 중첩된 네비게이션 그래프를 복원하되, 개선된 형태로 설계합니다.
   - 기본 구조:
     ```
     AppNavGraph (최상위)
     ├── Auth Graph (로그인/가입)
     ├── Main Graph (메인 탭들)
     │   ├── Home
     │   │   └── ProjectDetail (중첩)
     │   ├── Calendar
     │   └── Profile
     └── Other standalone screens
     ```

2. **프로젝트 상세 대안 처리**:
   - 경우에 따라 HomeScreen에 내장된 상세 보기와 별도 화면 상세 보기를 모두 지원합니다.
   - 태블릿/대화면: HomeScreen 내 분할 표시
   - 모바일/소화면: 별도 화면으로 이동

#### 2.2 MainScreen 중첩 네비게이션 처리 계획

1. **MainScreen 네비게이션 관리 개선**:
   - `MainScreen.kt`에서 자체 NavHost에 ProjectDetail 화면을 추가합니다.
   - 현재 Home, Calendar, Profile 탭 외에 ProjectDetail도 MainScreen 내부에서 처리합니다.

2. **HomeScreen 이벤트 처리 분기**:
   - HomeScreen에서 프로젝트 클릭 이벤트를 두 가지로 분기합니다:
     - **분할 보기 모드**: MainContent 영역에 프로젝트 상세 정보 표시 (현재 UI와 일치)
     - **전체 화면 모드**: `navigateToProjectDetails` 통해 별도 화면으로 이동 (옵션)

#### 2.3 HomeScreen에서 ProjectDetail로의 네비게이션 설계

1. **HomeViewModel 수정**:
   - 선택된 프로젝트 ID 저장 및 상태 관리:
     ```kotlin
     data class HomeUiState(
         // ... 기존 필드
         val selectedProjectId: String? = null
     )
     
     fun onProjectClick(projectId: String) {
         viewModelScope.launch {
             // 상태 업데이트 (내부 표시용)
             _uiState.update { it.copy(selectedProjectId = projectId) }
             
             // 이벤트 발행 (필요시 외부 이동용)
             // _eventFlow.emit(HomeEvent.NavigateToProjectDetails(projectId))
         }
     }
     ```

2. **HomeScreen 분할 화면 개선**:
   - MainContent 영역을 선택된 프로젝트 상세 정보로 업데이트:
     ```kotlin
     @Composable
     fun MainContent(
         uiState: HomeUiState,
         onNavigateToFullDetail: (String) -> Unit = {} // 전체 화면 모드 선택 시 콜백
     ) {
         when (uiState.selectedTopSection) {
             TopSection.PROJECTS -> {
                 if (uiState.selectedProjectId != null) {
                     ProjectDetailContent(
                         projectId = uiState.selectedProjectId,
                         onFullScreenClick = { onNavigateToFullDetail(uiState.selectedProjectId) }
                     )
                 } else {
                     EmptyStateMessage("프로젝트를 선택하세요")
                 }
             }
             // ... 기타 섹션
         }
     }
     ```

3. **네비게이션 효율성 개선**:
   - 별도 화면으로 이동할 필요가 있을 때만 `NavigateToProjectDetails` 이벤트 발행
   - 메인 화면에서 이미 보고 있는 프로젝트는 불필요한 네비게이션 없이 바로 표시

## 3. 구현 계획

- [x] 3.1: AppNavigationGraph 업데이트
- [x] 3.2: MainScreen 중첩 네비게이션 구현 개선
- [x] 3.3: HomeScreen과 ProjectDetail 연결 구현

### 구현 상세 계획

#### 3.1 AppNavigationGraph 업데이트

**변경 내용**:
1. `AppNavigationGraph.kt` 파일에서 중첩 네비게이션 그래프 구조 복원하되, 개선된 형태로 구현:

```kotlin
@Composable
fun AppNavigationGraph(
    navController: NavHostController,
    navigationHandler: ComposeNavigationHandler,
) {
    // ... 기존 코드 ...
    
    NavHost(
        navController = navController,
        startDestination = AppRoutes.Splash.ROOT,
    ) {
        // ... 인증 화면들 ...
        
        // 메인 화면과 그 내부 중첩 네비게이션
        navigation(
            route = AppRoutes.Main.GRAPH_ROOT,
            startDestination = AppRoutes.Main.ROOT
        ) {
            // 메인 화면 (이 안에 자체 NavHost로 Home/Calendar/Profile 포함)
            composable(AppRoutes.Main.ROOT) {
                MainScreen(
                    navigationHandler = navigationHandler
                )
            }
            
            // 메인 화면 내에서 직접 넘어갈 수 있는 화면들 (일반적으로는 탭별로 중첩 네비게이션 사용)
            composable(
                route = AppRoutes.Project.DETAIL_WITH_ARGS,
                arguments = AppRoutes.Project.detailArguments()
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString(AppRoutes.Project.ARG_PROJECT_ID) ?: ""
                // ... 프로젝트 상세 화면 ...
            }
            
            // 기타 메인 화면에서 접근 가능한 화면들...
        }
        
        // 별도의 독립 화면들 (메인 탭바 외부로 이동하는 화면)
        composable(
            route = AppRoutes.Project.JOIN_WITH_ARGS,
            arguments = AppRoutes.Project.joinArguments()
        ) {
            // ...
        }
        
        // 추가 화면들 여기에 등록
    }
}
```

2. `AppRoutes.kt` 클래스의 경로 상수 업데이트:
```kotlin
object AppRoutes {
    // ... 기존 코드 ...
    
    object Main {
        const val GRAPH_ROOT = "main_graph"
        const val ROOT = "main"
        
        object Home {
            const val GRAPH_ROOT = "home_graph"
            const val ROOT = "home"
            const val PROJECT_DETAIL = "project_detail/{projectId}"
        }
        
        // ... Calendar, Profile 등 다른 탭
    }
    
    // ... 기타 경로들
}
```

#### 3.2 MainScreen 중첩 네비게이션 구현 개선

**변경 내용**:
1. `MainScreen.kt` 파일에서 NavHost를 업데이트하여 ProjectDetail도 처리하도록 변경:

```kotlin
@Composable
fun MainScreen(
    navigationHandler: ComposeNavigationHandler,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    // ... 기존 코드 ...
    
    NavHost(
        navController = nestedNavController,
        startDestination = AppRoutes.Main.Home.GRAPH_ROOT,
        modifier = Modifier.padding(innerPadding)
    ) {
        // Home 탭 그래프
        navigation(
            route = AppRoutes.Main.Home.GRAPH_ROOT,
            startDestination = AppRoutes.Main.Home.ROOT
        ) {
            // 홈 화면
            composable(AppRoutes.Main.Home.ROOT) {
                HomeScreen(
                    navigationHandler = navigationHandler
                )
            }
            
            // 프로젝트 상세 화면 (전체 화면 모드)
            composable(
                route = AppRoutes.Main.Home.PROJECT_DETAIL,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                ProjectDetailScreen(
                    navigationHandler = navigationHandler,
                    projectId = projectId,
                    viewModel = hiltViewModel()
                )
            }
        }
        
        // ... Calendar, Profile 등 다른 탭
    }
}
```

2. `ComposeNavigationHandler` 인터페이스 수정:
```kotlin
interface ComposeNavigationHandler : NavigationHandler {
    // ... 기존 메서드들 ...
    
    /**
     * 중첩 그래프 내에서 프로젝트 상세 화면으로 이동
     * Main > Home > ProjectDetail 경로로 이동합니다.
     * 
     * @param projectId 이동할 프로젝트의 ID
     */
    fun navigateToProjectDetailsNested(projectId: String)
}
```

3. `NavigationManager` 구현체 수정:
```kotlin
override fun navigateToProjectDetailsNested(projectId: String) {
    // MainScreen 내부의 중첩된 NavController가 있는 경우 그것을 사용
    activeChildNavController?.let { childNav ->
        // /main/home/project_detail/{projectId} 형태의 경로
        val route = AppRoutes.Main.Home.PROJECT_DETAIL.replace("{projectId}", projectId)
        childNav.navigate(route)
        return
    }
    
    // 중첩 컨트롤러가 없는 경우 기존 방식으로 최상위 네비게이션 사용
    navigateToProjectDetails(projectId)
}
```

#### 3.3 HomeScreen과 ProjectDetail 연결 구현

**변경 내용**:
1. `HomeViewModel.kt` 파일 수정:
```kotlin
data class HomeUiState(
    // ... 기존 필드
    val selectedProjectId: String? = null,
    val isDetailFullScreen: Boolean = false // 전체 화면 모드 플래그
)

fun onProjectClick(projectId: String) {
    viewModelScope.launch {
        // 내부 상태 업데이트
        _uiState.update { it.copy(selectedProjectId = projectId) }
        
        // 이벤트는 필요한 경우에만 발행
        if (_uiState.value.isDetailFullScreen) {
            _eventFlow.emit(HomeEvent.NavigateToProjectDetails(projectId))
        }
    }
}

// 전체 화면 표시 모드 토글
fun toggleDetailDisplayMode() {
    _uiState.update { it.copy(isDetailFullScreen = !it.isDetailFullScreen) }
}
```

2. `HomeScreen.kt` 파일 수정:
```kotlin
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navigationHandler: ComposeNavigationHandler,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // ... 기존 코드 ...
    
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                // ... 기존 이벤트 처리 ...
                is HomeEvent.NavigateToProjectDetails -> {
                    if (uiState.isDetailFullScreen) {
                        // 전체 화면 모드
                        navigationHandler.navigateToProjectDetailsNested(event.projectId)
                    }
                    // 분할 화면 모드에서는 UI 상태 업데이트만으로 처리됨
                }
            }
        }
    }
    
    // ... 기존 코드 ...
}

@Composable
fun MainContent(uiState: HomeUiState, viewModel: HomeViewModel) {
    when (uiState.selectedTopSection) {
        TopSection.PROJECTS -> {
            if (uiState.selectedProjectId != null) {
                // 선택된 프로젝트가 있는 경우
                Column(modifier = Modifier.fillMaxSize()) {
                    // 간단한 헤더/상단 툴바
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "프로젝트 상세",
                            style = MaterialTheme.typography.titleLarge
                        )
                        
                        // 표시 모드 토글 버튼
                        IconButton(onClick = { viewModel.toggleDetailDisplayMode() }) {
                            Icon(
                                if (uiState.isDetailFullScreen) Icons.Default.Fullscreen
                                else Icons.Default.FullscreenExit,
                                contentDescription = "표시 모드 변경"
                            )
                        }
                    }
                    
                    // 프로젝트 상세 정보 표시 영역
                    ProjectDetailContent(
                        projectId = uiState.selectedProjectId,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )
                }
            } else {
                // 선택된 프로젝트가 없는 경우
                EmptyStateMessage("프로젝트를 선택하세요")
            }
        }
        // ... 기타 섹션
    }
}

@Composable
fun ProjectDetailContent(
    projectId: String,
    modifier: Modifier = Modifier
) {
    // 여기에 실제 프로젝트 상세 정보 표시 (feature_project의 컴포넌트 재사용 가능)
    // viewModel을 통해 프로젝트 정보 로드 또는 props로 전달받은 데이터 표시
    
    // 임시 표시 UI
    Surface(
        modifier = modifier,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "프로젝트 ID: $projectId",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "선택된 프로젝트의 상세 정보가 표시됩니다.",
                style = MaterialTheme.typography.bodyLarge
            )
            // 프로젝트 정보 리스트 (테스트용)
            for (i in 1..5) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("항목 $i: 프로젝트 세부 정보")
            }
        }
    }
}
```

## 4. 테스트 및 검증

- [ ] 4.1: 네비게이션 흐름 테스트
- [ ] 4.2: 각 화면 이동 검증
- [ ] 4.3: 에지 케이스 처리 확인 