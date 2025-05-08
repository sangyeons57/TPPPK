package com.example.core_navigation.testing

import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.core.NavigationHandler
import com.example.core_navigation.destination.AppRoutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 테스트 환경에서 사용할 수 있는 NavigationHandler 구현체
 * 
 * 실제 네비게이션을 수행하지 않고 명령을 기록하여 테스트 검증을 용이하게 합니다.
 */
class TestNavigationHandler(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) : ComposeNavigationHandler {
    companion object {
        // 기본 버퍼 크기
        private const val DEFAULT_BUFFER_SIZE = 10
        // 테스트 결과 Flow의 재생 크기
        private const val RESULT_REPLAY_SIZE = 1
    }
    
    // 네비게이션 경로 상수들
    private object Routes {
        const val PROJECT_DETAIL_PREFIX = "project_detail/"
        const val PROJECT_DETAIL_NESTED_PREFIX = "home/project_detail/"
        const val CHAT_BASE = "chat/"
        const val MESSAGE_ID_PARAM = "messageId"
    }
    
    // 명령 기록 저장소
    public val navigationHistory = mutableListOf<NavigationCommand>()
    
    // 결과 데이터 저장소
    private val resultStore = mutableMapOf<String, Any?>()
    
    // 결과 Flow 저장소
    private val resultFlows = mutableMapOf<String, MutableSharedFlow<Any?>>()
    
    // 네비게이션 명령 Flow
    private val _navigationCommands = MutableSharedFlow<NavigationCommand>(
        extraBufferCapacity = bufferSize
    )
    override val navigationCommands: Flow<NavigationCommand> = _navigationCommands.asSharedFlow()
    
    // 마지막으로 발행된 네비게이션 명령
    var lastNavigationCommand: NavigationCommand? = null
        private set
    
    // 기록된 모든 네비게이션 명령
    val allNavigationCommands: List<NavigationCommand>
        get() = navigationHistory.toList()
    
    // 중첩 NavController 레퍼런스
    private var childNavController: NavHostController? = null
    
    override fun setNavController(navController: NavHostController) {
        // 테스트 환경에서는 실제 처리 없음
    }
    
    override fun setChildNavController(navController: NavHostController?) {
        this.childNavController = navController
    }
    
    override fun getChildNavController(): NavHostController? {
        return childNavController
    }
    
    override fun navigate(command: NavigationCommand) {
        // 명령 기록
        navigationHistory.add(command)
        lastNavigationCommand = command
        
        // Flow에 발행
        _navigationCommands.tryEmit(command)
    }
    
    override fun navigateToTab(route: String, saveState: Boolean, restoreState: Boolean) {
        navigate(NavigationCommand.NavigateToTab(route, saveState, restoreState))
    }
    
    override fun navigateClearingBackStack(route: String) {
        navigate(NavigationCommand.NavigateClearingBackStack(route))
    }
    
    override fun navigateToNestedGraph(parentRoute: String, childRoute: String) {
        navigate(NavigationCommand.NavigateToNestedGraph(parentRoute, childRoute))
    }
    
    override fun navigateTo(routePath: String, navOptions: NavOptions?) {
        navigate(NavigationCommand.NavigateToRoute(routePath, navOptions))
    }
    
    override fun navigateBack(): Boolean {
        navigate(NavigationCommand.NavigateBack)
        return true // 항상 성공으로 처리
    }
    
    override fun navigateBackWithResult(key: String, result: Any?) {
        setResult(key, result)
        navigateBack()
    }
    
    override fun <T> setResult(key: String, result: T) {
        resultStore[key] = result
        
        // Flow 발행
        val resultFlow = resultFlows.getOrPut(key) { 
            MutableSharedFlow(replay = RESULT_REPLAY_SIZE) 
        } as MutableSharedFlow<Any?>
        resultFlow.tryEmit(result)
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T> getResult(key: String): T? {
        return resultStore[key] as? T
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T> getResultFlow(key: String): Flow<T> {
        return resultFlows.getOrPut(key) { 
            MutableSharedFlow(replay = RESULT_REPLAY_SIZE) 
        }.asSharedFlow() as Flow<T>
    }
    
    override fun navigateToProjectDetails(projectId: String) {
        // 프로젝트 상세 화면 이동 기록
        navigate(NavigationCommand.NavigateToRoute("${Routes.PROJECT_DETAIL_PREFIX}$projectId"))
    }
    
    override fun navigateToProjectDetailsNested(projectId: String) {
        // 중첩된 프로젝트 상세 화면 이동 기록
        navigate(NavigationCommand.NavigateToRoute("${Routes.PROJECT_DETAIL_NESTED_PREFIX}$projectId"))
    }
    
    override fun navigateToChat(channelId: String, messageId: String?) {
        // 채팅 화면 이동 기록
        val route = if (messageId != null) {
            "${Routes.CHAT_BASE}$channelId?${Routes.MESSAGE_ID_PARAM}=$messageId"
        } else {
            "${Routes.CHAT_BASE}$channelId"
        }
        navigate(NavigationCommand.NavigateToRoute(route))
    }
    
    override fun isDestinationActive(routePath: String): Boolean {
        // 테스트 환경에서는 항상 false 반환
        return false
    }
    
    /**
     * 기록된 모든 네비게이션 명령을 초기화합니다.
     */
    fun reset() {
        navigationHistory.clear()
        lastNavigationCommand = null
        resultStore.clear()
        resultFlows.clear()
    }
    
    /**
     * 특정 타입의 명령이 발행되었는지 확인합니다.
     * 
     * @return 해당 타입의 명령이 발행되었는지 여부
     */
    inline fun <reified T : NavigationCommand> hasCommandOfType(): Boolean {
        return navigationHistory.any { it is T }
    }
    
    /**
     * 특정 타입의 명령들을 필터링하여 반환합니다.
     * 
     * @return 해당 타입의 명령 목록
     */
    inline fun <reified T : NavigationCommand> getCommandsOfType(): List<T> {
        return navigationHistory.filterIsInstance<T>()
    }
} 