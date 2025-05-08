package com.example.core_navigation.testing

import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.core_navigation.core.NavigationCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever

/**
 * 테스트에서 사용할 수 있는 네비게이션 관련 유틸리티 함수들
 */
object NavigationTestUtils {
    // 테스트 타임아웃을 위한 상수 (밀리초)
    private const val TEST_TIMEOUT_MS = 5000L
    // 네비게이션 버퍼 사이즈
    private const val NAVIGATION_BUFFER_SIZE = 10
    
    /**
     * 테스트용 TestNavigationHandler를 생성합니다.
     * 
     * @return 구성된 TestNavigationHandler 인스턴스
     */
    fun createTestNavigationHandler(): TestNavigationHandler {
        return TestNavigationHandler(NAVIGATION_BUFFER_SIZE)
    }
    
    /**
     * 모의 NavHostController를 생성합니다.
     * 
     * @return 구성된 모의 NavHostController 인스턴스
     */
    fun createMockNavController(): NavHostController {
        val navController = Mockito.mock(NavHostController::class.java)
        
        // navigate 함수 모의
        doAnswer<Unit> { Unit }.whenever(navController).navigate(any<String>())
        doAnswer<Unit> { Unit }.whenever(navController).navigate(any<String>(), any())
        
        // popBackStack 함수 모의
        whenever(navController.popBackStack()).thenReturn(true)
        
        return navController
    }
    
    /**
     * 주어진 TestNavigationHandler로부터 네비게이션 이벤트를 수집하는 테스트 스코프를 제공합니다.
     * 
     * @param testNavigationHandler 사용할 테스트 네비게이션 핸들러
     * @param block 테스트 로직을 포함한 람다
     */
    suspend fun collectNavigationCommands(
        testNavigationHandler: TestNavigationHandler,
        block: suspend (List<NavigationCommand>) -> Unit
    ) {
        val collectedCommands = mutableListOf<NavigationCommand>()
        
        val job = CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined).launch {
            testNavigationHandler.navigationCommands.collectLatest { command ->
                collectedCommands.add(command)
            }
        }
        
        try {
            block(collectedCommands)
        } finally {
            job.cancel()
        }
    }
    
    /**
     * 네비게이션 테스트를 실행하는 도우미 함수
     * 
     * @param testBlock 테스트 로직을 포함한 람다
     */
    fun runNavigationTest(
        testBlock: suspend TestScope.(TestNavigationHandler) -> Unit
    ) = runTest {
        val testNavigationHandler = createTestNavigationHandler()
        val mockNavController = createMockNavController()
        
        // 테스트 네비게이션 핸들러에 모의 NavController 설정
        testNavigationHandler.setNavController(mockNavController)
        
        // 테스트 실행
        testBlock(testNavigationHandler)
    }
    
    /**
     * 문자열 경로에서 경로 인자를 추출합니다.
     * 
     * @param route 검사할 경로 문자열
     * @param paramName 추출할 파라미터 이름
     * @return 추출된 파라미터 값 또는 null
     */
    fun extractPathParam(route: String, paramName: String): String? {
        // 경로 패턴: path/{param}/path 또는 path/{param}
        val regex = ".*\\{$paramName\\}([^/]*).*".toRegex()
        val matchResult = regex.find(route)
        
        return matchResult?.groupValues?.getOrNull(1)
    }
    
    /**
     * 문자열 경로에서 쿼리 파라미터를 추출합니다.
     * 
     * @param route 검사할 경로 문자열
     * @param paramName 추출할 파라미터 이름
     * @return 추출된 파라미터 값 또는 null
     */
    fun extractQueryParam(route: String, paramName: String): String? {
        // 쿼리 파라미터 패턴: path?param=value 또는 path?other=value&param=value
        val regex = ".*[?&]$paramName=([^&]*).*".toRegex()
        val matchResult = regex.find(route)
        
        return matchResult?.groupValues?.getOrNull(1)
    }
} 