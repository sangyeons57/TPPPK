package com.example.data.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Coroutines 테스트를 위한 유틸리티 클래스
 * 
 * 테스트에서 메인 디스패처를 TestCoroutineDispatcher로 대체하여
 * Coroutines 코드를 결정적이고 예측 가능하게 테스트할 수 있도록 합니다.
 */
@ExperimentalCoroutinesApi
class CoroutinesTestRule(
    val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
) : TestRule {
    
    val testCoroutineScope = TestCoroutineScope(testDispatcher)
    
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            Dispatchers.setMain(testDispatcher)
            
            try {
                base.evaluate()
            } finally {
                Dispatchers.resetMain()
                testDispatcher.cleanupTestCoroutines()
            }
        }
    }
    
    fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) = 
        testCoroutineScope.runBlockingTest { block() }
}

/**
 * Flow 테스트를 위한 유틸리티 함수들
 */
@ExperimentalCoroutinesApi
object FlowTestUtil {
    
    /**
     * StateFlow의 모든 방출값을 수집하여 리스트로 반환
     */
    suspend fun <T> collectAllValues(
        dispatcher: TestCoroutineDispatcher,
        block: suspend () -> List<T>
    ): List<T> {
        val values = mutableListOf<T>()
        
        dispatcher.runBlockingTest {
            values.addAll(block())
        }
        
        return values
    }
} 