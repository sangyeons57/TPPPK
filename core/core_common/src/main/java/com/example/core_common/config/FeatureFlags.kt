package com.example.core_common.config

/**
 * 앱의 기능 플래그를 관리하는 객체
 * 점진적 롤아웃과 A/B 테스트를 위한 기능 토글을 제공
 */
object FeatureFlags {

    /**
     * 디버그 모드 여부
     * 라이브러리 모듈에서는 BuildConfig가 없으므로 하드코딩
     */
    private const val DEBUG = true // TODO: 실제 배포 시 false로 변경

    /**
     * 로컬 채팅 캐시 시스템 활성화 여부
     *
     * - true: 로컬 Room 캐시를 사용하여 즉시 응답 + 백그라운드 동기화
     * - false: 기존 Firestore 직접 접근 방식 사용
     *
     * 점진적 롤아웃 단계:
     * 1. DEBUG 빌드에서만 활성화 (개발/테스트)
     * 2. 내부 테스터 50% 활성화 (안정성 검증)
     * 3. 일반 사용자 50% 활성화 (성능 비교)
     * 4. 전체 사용자 활성화 (완전 배포)
     */
    val ENABLE_LOCAL_CHAT_CACHE: Boolean = when {
        // 디버그 빌드에서는 항상 활성화
        DEBUG -> true

        // 릴리즈 빌드에서는 점진적 롤아웃
        // TODO: 원격 설정(Firebase Remote Config)으로 대체 예정
        else -> false
    }

    /**
     * 캐시 성능 로깅 활성화 여부
     * 캐시 히트율, 동기화 빈도, 응답 시간 등을 로깅
     */
    val ENABLE_CACHE_PERFORMANCE_LOGGING: Boolean = DEBUG

    /**
     * 캐시 동기화 간격 (분)
     * WebSocket 연결이 안정적일 때는 길게, 불안정할 때만 짧게 설정
     */
    val CACHE_SYNC_INTERVAL_MINUTES: Long = when {
        DEBUG -> 5L // 개발: 5분 (테스트용)
        else -> 30L // 프로덕션: 30분 (안전망 역할)
    }

    /**
     * WebSocket 연결 끊김 후 복구 동기화 지연 시간 (초)
     * 연결이 복구된 후 놓친 메시지를 확인하기 위한 동기화
     */
    val WEBSOCKET_RECOVERY_SYNC_DELAY_SECONDS: Long = 3L

    /**
     * 채널당 최대 캐시 메시지 개수
     * 메모리 사용량과 성능의 균형을 위한 설정
     */
    val MAX_CACHED_MESSAGES_PER_CHANNEL: Int = when {
        DEBUG -> 100 // 개발: 적은 수로 테스트
        else -> 500 // 프로덕션: 충분한 캐시
    }

    /**
     * 백그라운드 동기화 활성화 여부 (현재 사용하지 않음 - WebSocket 기반 실시간 처리)
     * WebSocket을 통한 실시간 동기화를 사용하므로 별도 백그라운드 동기화는 불필요
     */
    @Deprecated("WebSocket 기반 실시간 동기화로 대체됨")
    val ENABLE_BACKGROUND_SYNC: Boolean = false

    /**
     * 캐시 실패 시 폴백 전략 활성화 여부
     * 캐시 오류 시 자동으로 기존 방식으로 폴백
     */
    val ENABLE_CACHE_FALLBACK: Boolean = true
}