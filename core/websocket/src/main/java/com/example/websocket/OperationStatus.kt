package com.example.websocket

/**
 * 작업 상태 상수 관리
 * 로깅, 메타데이터 등에서 사용하는 작업 결과 상태
 */
object OperationStatus {

    /** 작업 성공 */
    const val SUCCESS = "SUCCESS"

    /** 작업 실패 */
    const val FAILED = "FAILED"

    /** 수신 액션 */
    const val ACTION_RECEIVE = "RECEIVE"

    /** 전송 액션 */
    const val ACTION_SEND = "SEND"

    /** 편집 액션 */
    const val ACTION_EDIT = "EDIT"

    /** 삭제 액션 */
    const val ACTION_DELETE = "DELETE"
}