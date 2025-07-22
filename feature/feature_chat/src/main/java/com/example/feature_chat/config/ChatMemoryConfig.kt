package com.example.feature_chat.config

/**
 * Chat Memory Management Configuration
 * 채팅 메모리 관리를 위한 상수 정의
 */
object ChatMemoryConfig {
    
    /**
     * 메모리에 보관할 수 있는 최대 메시지 개수
     * 이 값을 초과하면 자동으로 오래된 메시지들을 메모리에서 제거합니다.
     */
    const val MAX_MESSAGES_IN_MEMORY = 200
    
    /**
     * 한번에 로딩하는 메시지 개수 (페이지네이션 크기)
     * 과거/최신 메시지를 로딩할 때 사용되는 배치 크기입니다.
     */
    const val PAGINATION_SIZE = 50
    
    /**
     * 메모리 정리를 시작할 임계값
     * 메시지 개수가 이 값에 도달하면 메모리 정리를 시작합니다.
     * MAX_MESSAGES_IN_MEMORY보다 작아야 합니다.
     */
    const val MEMORY_CLEANUP_THRESHOLD = 150
    
    /**
     * 메모리 정리 시 제거할 메시지 개수
     * 임계값에 도달했을 때 한번에 제거할 메시지의 개수입니다.
     */
    const val MESSAGES_TO_REMOVE_ON_CLEANUP = 50
    
    /**
     * 최소 메모리 보관 메시지 개수
     * 메모리 정리 후에도 최소한 이 개수만큼은 메시지를 보관합니다.
     */
    const val MIN_MESSAGES_IN_MEMORY = 100
}