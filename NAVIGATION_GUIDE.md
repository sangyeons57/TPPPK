# 네비게이션 매개변수 사용 가이드

이 가이드는 현재 프로젝트의 네비게이션 시스템에서 매개변수를 전달하고 수신하는 방법을 설명합니다.

## 📋 목차

1. [기본 개념](#기본-개념)
2. [매개변수 전달 방법](#매개변수-전달-방법)
3. [매개변수 수신 방법](#매개변수-수신-방법)
4. [실제 사용 예시](#실제-사용-예시)
5. [베스트 프랙티스](#베스트-프랙티스)

## 🎯 기본 개념

### 네비게이션 아키텍처 구성요소

- **NavigationManger**: 네비게이션 API 인터페이스
- **TypeSafeRoute**: 타입 안전한 라우트 정의 (Kotlinx Serialization 사용)
- **SavedStateHandle**: 매개변수 수신용 (Android Jetpack)
- **AppRoutes**: 기존 문자열 기반 라우트 (하위 호환성)

## 🚀 매개변수 전달 방법

### 방법 1: 편의 메소드 사용 (권장)

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigationManger: NavigationManger
) : ViewModel() {
    
    fun onProjectClick(projectId: String) {
        // 간단하고 직관적인 API
        navigationManger.navigateToProjectDetails(projectId)
    }
}
```

### 방법 2: TypeSafeRoute 직접 사용

```kotlin
fun navigateToProjectWithTypeSafety(projectId: String) {
    val route = ProjectDetailRoute(projectId = projectId)
    navigationManger.navigateTo(route)
}
```

### 방법 3: NavOptions와 함께 사용

```kotlin
fun navigateWithAnimation(projectId: String) {
    navigationManger.navigateToProjectDetails(
        projectId = projectId,
        navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .build()
    )
}
```

## 📥 매개변수 수신 방법

### ViewModel에서 SavedStateHandle 사용

```kotlin
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    // 다른 의존성들...
) : ViewModel() {
    
    // 방법 1: 필수 매개변수 (오류 발생시 앱 크래시)
    private val projectId: String = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID) 
        ?: error("프로젝트 ID가 필요합니다")
    
    // 방법 2: 기본값 제공 (더 안전)
    private val projectIdSafe: String = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID) 
        ?: "default_project_id"
    
    // 방법 3: 선택적 매개변수
    private val categoryId: String? = savedStateHandle.get<String>(AppRoutes.Project.ARG_CATEGORY_ID)
    
    // 매개변수를 사용한 초기화
    init {
        loadProjectData(projectId)
    }
}
```

## 🔍 실제 사용 예시

### 완전한 플로우: HomeScreen → ProjectDetailScreen

#### 1단계: HomeScreen에서 프로젝트 클릭

```kotlin
@Composable
fun HomeScreen(
    navigationManger: NavigationManger,
    viewModel: HomeViewModel = hiltViewModel()
) {
    LazyColumn {
        items(projects) { project ->
            Card(
                modifier = Modifier.clickable {
                    // ✅ 매개변수 전달
                    viewModel.onProjectClick(project.id)
                }
            ) {
                Text(project.name)
            }
        }
    }
}
```

#### 2단계: HomeViewModel에서 네비게이션 처리

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigationManger: NavigationManger
) : ViewModel() {
    
    fun onProjectClick(projectId: String) {
        // 프로젝트 선택 상태 업데이트
        updateSelectedProject(projectId)
        
        // ✅ 매개변수와 함께 네비게이션
        navigationManger.navigateToProjectDetails(projectId)
    }
}
```

#### 3단계: ProjectDetailScreen에서 매개변수 수신

```kotlin
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProjectDetailsUseCase: GetProjectDetailsUseCase
) : ViewModel() {
    
    // ✅ 매개변수 수신
    private val projectId: String = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID) 
        ?: error("프로젝트 ID가 전달되지 않았습니다")
    
    private val _uiState = MutableStateFlow(ProjectDetailUiState(projectId = projectId))
    val uiState = _uiState.asStateFlow()
    
    init {
        // ✅ 매개변수 사용
        loadProjectDetails(projectId)
    }
    
    private fun loadProjectDetails(projectId: String) {
        viewModelScope.launch {
            getProjectDetailsUseCase(projectId).collect { result ->
                // 프로젝트 데이터 처리...
            }
        }
    }
}
```

#### 4단계: ProjectDetailScreen UI

```kotlin
@Composable
fun ProjectDetailScreen(
    navigationManger: NavigationManger,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // ViewModel이 이미 projectId를 받아서 데이터를 로드했음
    Column {
        TopAppBar(
            title = { Text("프로젝트: ${uiState.projectName}") },
            navigationIcon = {
                IconButton(onClick = { navigationManger.navigateBack() }) {
                    Icon(Icons.Default.ArrowBack, "뒤로가기")
                }
            }
        )
        
        // 프로젝트 상세 내용...
        ProjectContent(
            project = uiState.project,
            onChannelClick = { channelId ->
                // ✅ 체이닝된 네비게이션
                navigationManger.navigateToChat(channelId)
            }
        )
    }
}
```

### 복잡한 매개변수 예시: 채널 편집

```kotlin
// 복잡한 매개변수 전달
fun navigateToEditChannel(projectId: String, categoryId: String, channelId: String) {
    val route = EditChannelRoute(
        projectId = projectId,
        categoryId = categoryId, 
        channelId = channelId
    )
    navigationManger.navigateTo(route)
}

// 복잡한 매개변수 수신
@HiltViewModel
class EditChannelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val projectId = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID) ?: ""
    private val categoryId = savedStateHandle.get<String>(AppRoutes.Project.ARG_CATEGORY_ID) ?: ""
    private val channelId = savedStateHandle.get<String>(AppRoutes.Project.ARG_CHANNEL_ID) ?: ""
    
    // 모든 매개변수를 사용한 초기화
    init {
        loadChannelData(projectId, categoryId, channelId)
    }
}
```

## 📊 결과 반환 패턴

### 결과와 함께 뒤로가기

```kotlin
@HiltViewModel
class AddProjectViewModel @Inject constructor(
    private val navigationManger: NavigationManger,
    private val createProjectUseCase: CreateProjectUseCase
) : ViewModel() {
    
    fun createProject(projectName: String) {
        viewModelScope.launch {
            createProjectUseCase(projectName).onSuccess { project ->
                // ✅ 결과와 함께 뒤로가기
                navigationManger.navigateBackWithResult(
                    key = "created_project",
                    result = project.id
                )
            }
        }
    }
}
```

### 결과 받기

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // ✅ 결과 관찰
    private val createdProjectResult = savedStateHandle.getStateFlow<String?>("created_project", null)
    
    init {
        viewModelScope.launch {
            createdProjectResult.collect { projectId ->
                projectId?.let {
                    // 새로 생성된 프로젝트 처리
                    refreshProjects()
                    selectProject(it)
                }
            }
        }
    }
}
```

## 🎯 베스트 프랙티스

### ✅ 권장사항

1. **편의 메소드 사용**: `navigationManger.navigateToProjectDetails(id)` 형태가 가장 간단
2. **안전한 매개변수 수신**: 기본값 제공으로 앱 크래시 방지
3. **명확한 에러 메시지**: 매개변수 누락시 디버깅 용이한 메시지 제공
4. **TypeSafeRoute 활용**: 복잡한 매개변수의 경우 컴파일 타임 안정성 확보

### ❌ 피해야 할 것들

1. **문자열 직접 조작**: `"project/$projectId"` 같은 수동 URL 생성
2. **매개변수 검증 누락**: null 체크 없이 매개변수 사용
3. **하드코딩된 키**: AppRoutes 상수 대신 문자열 직접 사용

### 📝 매개변수 키 정의 위치

모든 매개변수 키는 `AppRoutes`에 정의되어 있습니다:

```kotlin
object AppRoutes {
    object Project {
        const val ARG_PROJECT_ID = "projectId"
        const val ARG_CATEGORY_ID = "categoryId"
        const val ARG_CHANNEL_ID = "channelId"
        const val ARG_USER_ID = "userId"
        // ...
    }
}
```

## 🔧 디버깅 팁

### 매개변수가 전달되지 않는 경우

1. **로그 확인**: NavigationManagerImpl에서 로그 출력
2. **라우트 등록 확인**: AppNavigationGraph.kt에서 매개변수 정의 확인
3. **타입 체크**: SavedStateHandle에서 올바른 타입으로 받고 있는지 확인

```kotlin
// 디버깅용 로그 추가
private val projectId: String = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID).also { id ->
    Log.d("ProjectDetailViewModel", "Received projectId: $id")
} ?: error("프로젝트 ID가 필요합니다")
```

### 일반적인 문제와 해결방법

| 문제 | 원인 | 해결방법 |
|------|------|----------|
| 매개변수가 null | 잘못된 키 사용 | AppRoutes 상수 사용 확인 |
| 앱 크래시 | 필수 매개변수 누락 | 기본값 제공 또는 안전한 처리 |
| 네비게이션 실패 | 라우트 미등록 | AppNavigationGraph.kt 확인 |
| 타입 에러 | 잘못된 타입 캐스팅 | SavedStateHandle 타입 확인 |

---

이 가이드를 통해 네비게이션 매개변수를 안전하고 효율적으로 사용할 수 있습니다. 추가 질문이 있으시면 언제든지 문의해주세요!