package com.example.core_common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 코루틴 디스패처를 제공하는 인터페이스입니다.
 * 테스트 용이성을 위해 실제 디스패처를 추상화합니다.
 */
interface DispatcherProvider {
    /** IO 작업을 위한 디스패처 */
    val io: CoroutineDispatcher
    /** CPU 집약적인 작업을 위한 디스패처 */
    val default: CoroutineDispatcher
    /** UI 작업을 위한 메인 스레드 디스패처 */
    val main: CoroutineDispatcher
    /** 즉시 실행되지만 메인 스레드 경계를 넘지 않는 디스패처 */
    val mainImmediate: CoroutineDispatcher
    /** 제한되지 않은, 새 스레드를 생성할 수 있는 디스패처 */
    val unconfined: CoroutineDispatcher
} 