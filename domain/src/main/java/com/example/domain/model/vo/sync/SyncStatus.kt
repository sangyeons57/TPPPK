package com.example.domain.model.vo.sync

/**
 * 엔티티별 동기화 상태를 나타내는 열거형
 * 개별 엔티티의 동기화 진행상황과 상태를 추적하기 위해 사용
 *
 * 📋 상태 전이:
 * SYNCED → PENDING (로컬 변경 발생)
 * PENDING → SYNCING (서버 전송 시작)
 * SYNCING → SYNCED (성공적 동기화)
 * SYNCING → FAILED (동기화 실패)
 * FAILED → PENDING (재시도 대기)
 *
 * 🎯 사용처:
 * - 각 도메인 엔티티의 syncStatus 필드
 * - UI에서 동기화 상태 표시
 * - 동기화 재시도 로직에서 대상 엔티티 식별
 */
enum class SyncStatus {
    /**
     * 동기화 완료 상태
     * - 서버와 로컬 데이터가 일치
     * - 추가 동기화 작업 불필요
     */
    SYNCED,

    /**
     * 동기화 대기 상태
     * - 로컬에서 변경이 발생했으나 아직 서버로 전송되지 않음
     * - OutboxRepository에서 처리 대상
     */
    PENDING,

    /**
     * 동기화 진행 중
     * - 현재 서버로 데이터 전송 중
     * - 중복 처리 방지를 위한 상태
     */
    SYNCING,

    /**
     * 동기화 실패 상태
     * - 서버 전송 중 오류 발생
     * - 재시도 대상 또는 사용자 개입 필요
     */
    FAILED;

    companion object {
        /**
         * 기본 동기화 상태 (새로 생성된 엔티티)
         */
        val DEFAULT = SYNCED

        /**
         * 동기화가 필요한 상태들
         * OutboxRepository에서 처리해야 할 대상
         */
        val NEEDS_SYNC = setOf(PENDING, FAILED)

        /**
         * 동기화 진행 상태들
         * 중복 처리를 방지해야 할 대상
         */
        val IN_PROGRESS = setOf(SYNCING)

        /**
         * String 값으로부터 SyncStatus 생성
         * Room DB 저장시 문자열로 변환되므로 역변환 필요
         */
        fun fromString(value: String): SyncStatus {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                DEFAULT
            }
        }
    }

    /**
     * 동기화가 필요한지 확인
     */
    fun needsSync(): Boolean = this in NEEDS_SYNC

    /**
     * 현재 동기화 진행 중인지 확인
     */
    fun isInProgress(): Boolean = this in IN_PROGRESS

    /**
     * 동기화가 완료된 상태인지 확인
     */
    fun isSynced(): Boolean = this == SYNCED

    /**
     * 동기화가 실패한 상태인지 확인
     */
    fun isFailed(): Boolean = this == FAILED
}