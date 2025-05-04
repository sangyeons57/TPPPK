package com.example.feature_settings.utils

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
    val testCoroutineDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
) : TestRule {
    
    val testCoroutineScope = TestCoroutineScope(testCoroutineDispatcher)
    
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            Dispatchers.setMain(testCoroutineDispatcher)
            
            try {
                base.evaluate()
            } finally {
                Dispatchers.resetMain()
                testCoroutineDispatcher.cleanupTestCoroutines()
            }
        }
    }
    
    fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) = 
        testCoroutineScope.runBlockingTest { block() }
} 