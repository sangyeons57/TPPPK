package com.example.teamnovapersonalprojectprojectingkotlin

/**
 * 네비게이션 매개변수 전달과 수신 예시
 * 
 * 이 파일은 현재 네비게이션 시스템에서 매개변수가 어떻게 동작하는지 보여주는 예시입니다.
 * feature_project_detail 모듈을 기반으로 실제 사용 패턴을 설명합니다.
 */

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.core.ProjectDetailRoute
import com.example.core_navigation.core.TypeSafeRoute
import com.example.core_navigation.destination.AppRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ===== 1단계: 매개변수 전달 (발신자) =====
 * 
 * HomeScreen에서 프로젝트를 클릭했을 때 ProjectDetailScreen으로 이동하는 예시
 */

// 1-A) ViewModel에서 NavigationManger 주입받아 사용
@HiltViewModel
class ExampleHomeViewModel @Inject constructor(
    private val navigationManger: NavigationManger
) : ViewModel() {
    
    /**
     * 방법 1: TypeSafeRoute 사용 (권장)
     * - 컴파일 타임 타입 안정성 제공
     * - 매개변수 누락 방지
     */
    fun navigateToProjectDetailsTypeSafe(projectId: String) {
        // TypeSafeRoute 생성 - 컴파일러가 타입 체크
        val route = ProjectDetailRoute(projectId = projectId)
        navigationManger.navigateTo(route)
    }
    
    /**
     * 방법 2: 편의 메소드 사용 (권장)
     * - 간단한 API
     * - 내부적으로 TypeSafeRoute 사용
     */
    fun navigateToProjectDetailsConvenience(projectId: String) {
        navigationManger.navigateToProjectDetails(projectId)
    }
    
    /**
     * 방법 3: NavOptions와 함께 사용
     * - 애니메이션, backstack 동작 제어 가능
     */
    fun navigateToProjectDetailsWithOptions(projectId: String) {
        navigationManger.navigateToProjectDetails(
            projectId = projectId,
            navOptions = androidx.navigation.NavOptions.Builder()
                .setEnterAnim(android.R.anim.slide_in_left)
                .setExitAnim(android.R.anim.slide_out_right)
                .build()
        )
    }
}

// 1-B) Composable에서 직접 사용
@Composable
fun ExampleHomeScreen(
    navigationManger: NavigationManger,
    viewModel: ExampleHomeViewModel = hiltViewModel()
) {
    // 프로젝트 리스트 (예시)
    val projects = listOf(
        Project("project_1", "첫 번째 프로젝트"),
        Project("project_2", "두 번째 프로젝트")
    )
    
    LazyColumn {
        items(projects) { project ->
            ProjectItem(
                project = project,
                onClick = {
                    // ViewModel 메소드 호출
                    viewModel.navigateToProjectDetailsConvenience(project.id)
                    
                    // 또는 직접 호출
                    // navigationManger.navigateToProjectDetails(project.id)
                }
            )
        }
    }
}

/**
 * ===== 2단계: 매개변수 수신 (수신자) =====
 * 
 * ProjectDetailScreen에서 전달받은 projectId를 사용하는 예시
 */

// 2-A) ViewModel에서 SavedStateHandle로 매개변수 수신
@HiltViewModel
class ExampleProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle // Hilt가 자동으로 주입
) : ViewModel() {
    
    /**
     * 방법 1: SavedStateHandle에서 직접 매개변수 추출 (현재 사용 중)
     * - AppRoutes에 정의된 KEY 사용
     * - null 체크 필요
     */
    private val projectId: String = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID) 
        ?: error("${AppRoutes.Project.ARG_PROJECT_ID} is required")
    
    /**
     * 방법 2: 안전한 매개변수 추출 (권장)
     * - 기본값 제공으로 더 안전
     */
    private val projectIdSafe: String = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID) 
        ?: "default_project_id"
    
    /**
     * 방법 3: 여러 매개변수 동시 추출
     */
    private val allParams = run {
        val projectId = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID)
        val categoryId = savedStateHandle.get<String>(AppRoutes.Project.ARG_CATEGORY_ID)
        val channelId = savedStateHandle.get<String>(AppRoutes.Project.ARG_CHANNEL_ID)
        
        ProjectParams(projectId, categoryId, channelId)
    }
    
    // 매개변수를 사용한 비즈니스 로직
    fun loadProjectData() {
        viewModelScope.launch {
            // projectId를 사용해서 프로젝트 데이터 로드
            println("Loading project data for: $projectId")
            // useCase.getProject(projectId) 등...
        }
    }
}

// 2-B) Composable에서 ViewModel 사용
@Composable
fun ExampleProjectDetailScreen(
    navigationManger: NavigationManger,
    viewModel: ExampleProjectDetailViewModel = hiltViewModel()
) {
    // ViewModel이 이미 projectId를 가지고 있음
    LaunchedEffect(Unit) {
        viewModel.loadProjectData()
    }
    
    // UI 구성...
    Column {
        // TopAppBar with back button
        TopAppBar(
            title = { Text("프로젝트 상세") },
            navigationIcon = {
                IconButton(onClick = { navigationManger.navigateBack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                }
            }
        )
        
        // Content...
    }
}

/**
 * ===== 3단계: AppNavigationGraph.kt 등록 =====
 * 
 * 매개변수를 받는 라우트를 네비게이션 그래프에 등록하는 방법
 */

/*
// AppNavigationGraph.kt에서의 등록 예시

fun NavGraphBuilder.projectGraph(navigationManger: NavigationManger) {
    navigation(
        route = AppRoutes.Project.ROOT,
        startDestination = AppRoutes.Project.ADD
    ) {
        // 기존 방식: 수동 매개변수 처리
        composable(
            route = AppRoutes.Project.detailRoute(), // "project/{projectId}"
            arguments = AppRoutes.Project.detailArguments // projectId 매개변수 정의
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(AppRoutes.Project.ARG_PROJECT_ID) ?: ""
            
            ExampleProjectDetailScreen(navigationManger = navigationManger)
        }
        
        // 개선된 방식: safeComposable 사용 (권장)
        safeComposable(
            route = AppRoutes.Project.detailRoute(),
            arguments = projectArguments()
        ) { backStackEntry ->
            val args = backStackEntry.extractProjectArguments()
            
            ExampleProjectDetailScreen(
                navigationManger = navigationManger
                // args.projectId 는 이미 ViewModel에서 SavedStateHandle로 받음
            )
        }
    }
}
*/

/**
 * ===== 4단계: 복잡한 매개변수 예시 =====
 * 
 * 여러 매개변수를 동시에 전달하는 경우
 */

// 복잡한 라우트 예시: 채널 편집 화면
@Composable
fun ExampleComplexNavigation(navigationManger: NavigationManger) {
    // 여러 매개변수를 한 번에 전달
    Button(
        onClick = {
            // TypeSafeRoute로 복잡한 매개변수 전달
            val route = com.example.core_navigation.core.EditChannelRoute(
                projectId = "project_123",
                categoryId = "category_456", 
                channelId = "channel_789"
            )
            navigationManger.navigateTo(route)
        }
    ) {
        Text("채널 편집")
    }
}

// 복잡한 매개변수 수신
@HiltViewModel
class ExampleComplexViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // 여러 매개변수 동시 추출
    private val projectId = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID) ?: ""
    private val categoryId = savedStateHandle.get<String>(AppRoutes.Project.ARG_CATEGORY_ID) ?: ""
    private val channelId = savedStateHandle.get<String>(AppRoutes.Project.ARG_CHANNEL_ID) ?: ""
    
    init {
        println("Received parameters:")
        println("Project ID: $projectId")
        println("Category ID: $categoryId") 
        println("Channel ID: $channelId")
    }
}

/**
 * ===== 5단계: 결과 반환 예시 =====
 * 
 * 화면 간 결과를 주고받는 방법
 */

// 결과를 설정하고 뒤로가기
@HiltViewModel
class ExampleResultViewModel @Inject constructor(
    private val navigationManger: NavigationManger
) : ViewModel() {
    
    fun saveAndReturn(projectName: String) {
        // 결과와 함께 뒤로가기
        navigationManger.navigateBackWithResult(
            key = "project_name_result",
            result = projectName
        )
    }
}

// 결과 관찰
@HiltViewModel  
class ExampleReceiveResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // 결과 관찰
    val projectNameResult = savedStateHandle.getStateFlow<String?>("project_name_result", null)
    
    init {
        viewModelScope.launch {
            projectNameResult.collect { result ->
                result?.let {
                    println("Received result: $it")
                    // 결과 처리 로직
                }
            }
        }
    }
}

/**
 * ===== 데이터 클래스 정의 =====
 */

data class Project(
    val id: String,
    val name: String
)

data class ProjectParams(
    val projectId: String?,
    val categoryId: String?,
    val channelId: String?
)

@Composable
fun ProjectItem(
    project: Project,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Text(
            text = project.name,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * ===== 요약: 매개변수 전달 플로우 =====
 * 
 * 1. 발신자 (HomeScreen):
 *    navigationManger.navigateToProjectDetails("project_123")
 *    
 * 2. 네비게이션 시스템:
 *    - TypeSafeRoute → AppRoutes path 변환
 *    - NavController.navigate("project/project_123") 호출
 *    
 * 3. 수신자 (ProjectDetailScreen):
 *    - ViewModel에서 SavedStateHandle.get("projectId") 호출
 *    - "project_123" 값 수신
 *    
 * 4. 사용:
 *    - ViewModel에서 projectId로 비즈니스 로직 실행
 *    - UI에서 해당 프로젝트 데이터 표시
 */