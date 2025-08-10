package com.example.core_common.constant

/**
 * 페이징 관련 상수들을 중앙화한 객체
 * MessageService와 MessageRepositoryImpl 간의 값 불일치 방지
 */
object PagingConstants {
    const val PAGE_SIZE = 30 // 15개씩 로딩
    const val INITIAL_LOAD_SIZE = PAGE_SIZE * 2 // 초기 2페이지
    // 윈도우 크기: 90개(메모리/안정성 균형)
    const val MAX_SIZE = PAGE_SIZE * 6 // 90
    const val PREFETCH_DISTANCE = 2
    const val MAX_SIZE_THRESHOLD = PAGE_SIZE * 5 // 75
}