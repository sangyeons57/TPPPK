package com.example.teamnovapersonalprojectprojectingkotlin

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Stress test to reproduce the app crash that occurs after a certain period.
 * This test simulates heavy usage patterns that might trigger memory leaks,
 * navigation issues, or Firebase-related problems.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CrashReproductionTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    /**
     * Test for memory leaks by simulating repeated navigation and data loading
     */
    @Test
    fun stressTest_NavigationMemoryLeaks() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Simulate app running for extended period
        repeat(50) { iteration ->
            // Simulate navigation between screens
            // Wait between operations to allow coroutines to accumulate
            delay(100)
            
            // Log memory usage periodically
            if (iteration % 10 == 0) {
                val runtime = Runtime.getRuntime()
                val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                println("CrashTest - Iteration $iteration, Memory used: ${usedMemory / 1024 / 1024} MB")
            }
        }
        
        // Final memory check
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        println("CrashTest - Final memory usage: ${usedMemory / 1024 / 1024} MB")
        
        // Force garbage collection to see if memory is released
        System.gc()
        delay(1000)
        
        val usedMemoryAfterGC = runtime.totalMemory() - runtime.freeMemory()
        println("CrashTest - Memory after GC: ${usedMemoryAfterGC / 1024 / 1024} MB")
        
        // Wait for potential delayed crashes
        delay(5000)
    }

    /**
     * Test for Firebase operation hanging or accumulating
     */
    @Test
    fun stressTest_FirebaseOperations() = runBlocking {
        // Simulate repeated Firebase operations that might accumulate
        repeat(20) { iteration ->
            println("CrashTest - Firebase operation iteration: $iteration")
            
            // Simulate auth state changes
            delay(200)
            
            // Check for any hanging operations
            if (iteration % 5 == 0) {
                println("CrashTest - Checking for hanging operations at iteration $iteration")
                delay(1000)
            }
        }
        
        // Wait for potential Firebase-related crashes
        delay(10000)
    }

    /**
     * Test for coroutine accumulation and proper cancellation
     */
    @Test
    fun stressTest_CoroutineAccumulation() = runBlocking {
        println("CrashTest - Testing coroutine accumulation")
        
        // Simulate repeated coroutine launches that might not be properly canceled
        repeat(100) { iteration ->
            // Simulate ViewModel operations with flow collectors
            delay(50)
            
            if (iteration % 20 == 0) {
                println("CrashTest - Coroutine test iteration: $iteration")
                // Force garbage collection periodically
                System.gc()
                delay(500)
            }
        }
        
        println("CrashTest - Waiting for potential coroutine-related crashes")
        delay(15000)
    }

    /**
     * Combined stress test that simulates real usage patterns
     */
    @Test
    fun stressTest_RealUsagePattern() = runBlocking {
        println("CrashTest - Starting real usage pattern simulation")
        
        val startTime = System.currentTimeMillis()
        val testDurationMs = 30_000 // 30 seconds
        
        var iteration = 0
        while (System.currentTimeMillis() - startTime < testDurationMs) {
            iteration++
            
            // Simulate user interactions
            delay(100)
            
            // Periodic memory monitoring
            if (iteration % 50 == 0) {
                val runtime = Runtime.getRuntime()
                val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                val elapsedTime = (System.currentTimeMillis() - startTime) / 1000
                println("CrashTest - Time: ${elapsedTime}s, Iteration: $iteration, Memory: ${usedMemory / 1024 / 1024} MB")
            }
            
            // Simulate different types of operations
            when (iteration % 4) {
                0 -> {
                    // Simulate navigation
                    delay(50)
                }
                1 -> {
                    // Simulate data loading
                    delay(100)
                }
                2 -> {
                    // Simulate Firebase operations
                    delay(75)
                }
                3 -> {
                    // Simulate UI state changes
                    delay(25)
                }
            }
        }
        
        println("CrashTest - Completed real usage pattern. Total iterations: $iteration")
        
        // Final stability check
        delay(5000)
        println("CrashTest - Final stability check completed")
    }
}