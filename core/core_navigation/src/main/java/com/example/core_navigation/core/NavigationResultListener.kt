package com.example.core_navigation.core

import kotlinx.coroutines.flow.Flow

/**
 * 네비게이션 결과를 수신하는 인터페이스
 * 
 * 네비게이션 결과를 Flow로 제공하여 관찰 가능하게 합니다.
 */
interface NavigationResultListener {
    /**
     * 특정 키로 식별되는 결과를 Flow로 가져옵니다.
     * 
     * @param key 결과를 식별하는 키
     * @return 결과를 포함하는 Flow
     */
    fun <T> getResultFlow(key: String): Flow<T>
} 