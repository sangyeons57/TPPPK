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
     * WebSocket 연결 끊김 후 복구 동기화 지연 시간 (초)
     * 연결이 복구된 후 놓친 메시지를 확인하기 위한 동기화
     */
    val WEBSOCKET_RECOVERY_SYNC_DELAY_SECONDS: Long = 3L
}