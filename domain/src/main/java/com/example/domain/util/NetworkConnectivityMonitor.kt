package com.example.domain.util

import kotlinx.coroutines.flow.Flow

/**
 * 네트워크 연결 상태를 모니터링하는 인터페이스
 * 앱의 네트워크 상태 변화를 감지하고 알립니다.
 */
interface NetworkConnectivityMonitor {
    /**
     * 네트워크 연결 가능 여부를 나타내는 Flow
     * true: 네트워크 연결됨, false: 네트워크 연결되지 않음
     */
    val isNetworkAvailable: Flow<Boolean>
} 