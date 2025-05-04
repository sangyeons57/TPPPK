package com.example.feature_settings.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import java.util.concurrent.atomic.AtomicReference

/**
 * Flow 테스트를 위한 확장 함수
 * 
 * 테스트에서 Flow를 쉽게 테스트하기 위한 유틸리티 함수들을 제공합니다.
 */
@ExperimentalCoroutinesApi
object FlowTestExtensions {

    /**
     * StateFlow의 현재 값 가져오기
     */
    fun <T> StateFlow<T>.getValue() = this.value

    /**
     * Flow에서 첫 번째 값 가져오기
     */
    @ExperimentalCoroutinesApi
    suspend fun <T> Flow<T>.getFirstValue(scope: TestCoroutineScope): T {
        val result = AtomicReference<T>()
        val job = scope.launch {
            result.set(first())
        }
        job.join()
        return result.get()
    }

    /**
     * Flow에서 모든 값 수집하기
     */
    @ExperimentalCoroutinesApi
    suspend fun <T> Flow<T>.getAllValues(scope: TestCoroutineScope): List<T> {
        val result = mutableListOf<T>()
        val job = scope.launch {
            toList(result)
        }
        job.join()
        return result
    }

    /**
     * SharedFlow 테스트용 이벤트 수집기
     */
    @ExperimentalCoroutinesApi
    class EventCollector<T> {
        private val _events = mutableListOf<T>()
        val events: List<T> get() = _events.toList()

        fun collectFrom(scope: TestCoroutineScope, flow: SharedFlow<T>) {
            scope.launch {
                flow.collect {
                    _events.add(it)
                }
            }
        }

        fun clear() {
            _events.clear()
        }
    }

    /**
     * MutableStateFlow에 값 설정 (테스트 헬퍼)
     */
    fun <T> MutableStateFlow<T>.setValue(value: T) {
        this.value = value
    }
} 