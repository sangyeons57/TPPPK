package com.example.core_common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DispatcherProvider 인터페이스의 기본 구현체입니다.
 * 실제 코루틴 디스패처를 제공합니다.
 */
@Singleton // 앱 전역에서 하나의 인스턴스만 사용하도록 Singleton으로 지정
class DispatcherProviderImpl @Inject constructor() : DispatcherProvider {
    override val io: CoroutineDispatcher
        get() = Dispatchers.IO

    override val default: CoroutineDispatcher
        get() = Dispatchers.Default

    override val main: CoroutineDispatcher
        get() = Dispatchers.Main

    override val mainImmediate: CoroutineDispatcher
        get() = Dispatchers.Main.immediate

    override val unconfined: CoroutineDispatcher
        get() = Dispatchers.Unconfined
} 