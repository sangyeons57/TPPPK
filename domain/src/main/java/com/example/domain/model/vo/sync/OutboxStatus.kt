package com.example.domain.model.vo.sync

/**
 * Outbox 작업의 처리 상태를 나타내는 열거형
 * 개별 Outbox 항목의 처리 진행상황을 추적하기 위해 사용
 *
 * 📋 상태 전이:
 * PENDING → PROCESSING (SyncManager가 작업 시작)
 * PROCESSING → COMPLETED (서버 전송 성공)
 * PROCESSING → FAILED (서버 전송 실패)
 * FAILED → PENDING (재스케줄링)
 *
 * 🎯 사용처:
 * - OutboxEntity의 status 필드
 * - OutboxRepository에서 작업 필터링
 * - SyncManager에서 처리 상태 관리
 */
enum class OutboxStatus {
    /**
     * 처리 대기 상태
     * - 새로 생성된 Outbox 작업
     * - 재시도 대기 중인 작업
     * - SyncManager 처리 대상
     */
    PENDING,

    /**
     * 처리 진행 중
     * - SyncManager가 현재 처리 중
     * - 서버로 데이터 전송 중
     * - 중복 처리 방지를 위한 상태
     */
    PROCESSING,

    /**
     * 처리 완료 상태
     * - 서버 전송 성공
     * - 정리(cleanup) 대상
     */
    COMPLETED,

    /**
     * 처리 실패 상태
     * - 서버 전송 중 오류 발생
     * - 재시도 대상 또는 수동 처리 필요
     */
    FAILED;

    companion object {
        /**
         * 기본 Outbox 상태 (새로 생성된 작업)
         */
        val DEFAULT = PENDING

        /**
         * 처리가 필요한 상태들
         * SyncManager에서 lease해야 할 대상
         */
        val PROCESSABLE = setOf(PENDING, FAILED)

        /**
         * 진행 중인 상태들
         * 중복 처리를 방지해야 할 대상
         */
        val IN_PROGRESS = setOf(PROCESSING)

        /**
         * 완료된 상태들
         * 정리(cleanup) 대상
         */
        val FINISHED = setOf(COMPLETED)

        /**
         * String 값으로부터 OutboxStatus 생성
         * Room DB 저장시 문자열로 변환되므로 역변환 필요
         */
        fun fromString(value: String): OutboxStatus {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                DEFAULT
            }
        }
    }

    /**
     * 처리 가능한 상태인지 확인
     */
    fun isProcessable(): Boolean = this in PROCESSABLE

    /**
     * 현재 처리 진행 중인지 확인
     */
    fun isInProgress(): Boolean = this in IN_PROGRESS

    /**
     * 처리가 완료되었는지 확인
     */
    fun isCompleted(): Boolean = this == COMPLETED

    /**
     * 처리가 실패했는지 확인
     */
    fun isFailed(): Boolean = this == FAILED

    /**
     * 정리(cleanup) 대상인지 확인
     */
    fun canBeCleanedUp(): Boolean = this in FINISHED
}