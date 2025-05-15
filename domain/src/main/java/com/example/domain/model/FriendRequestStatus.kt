package com.example.domain.model

// import com.example.core_common.constants.FirestoreConstants // 더 이상 직접 참조하지 않음

/**
 * 친구 요청 및 관계 상태를 나타내는 열거형.
 */
enum class FriendRequestStatus(val value: String) {
    PENDING_SENT("pending_sent"),     // 내가 친구 요청을 보낸 상태 (Firestore: "pending_sent")
    PENDING_RECEIVED("pending_received"), // 내가 친구 요청을 받은 상태 (Firestore: "pending_received")
    ACCEPTED("accepted"),         // 친구 관계가 수락된 상태 (Firestore: "accepted")
    DECLINED("declined"),         // 친구 요청이 거절된 상태 (선택적)
    BLOCKED("blocked"),           // 차단된 상태 (선택적)
    UNKNOWN("unknown");           // 알 수 없는 상태

    companion object {
        /**
         * 문자열로부터 FriendRequestStatus를 파싱합니다.
         * 일치하는 타입이 없으면 UNKNOWN을 반환합니다.
         */
        fun fromString(statusStr: String?): FriendRequestStatus {
            return entries.find { it.value.equals(statusStr, ignoreCase = true) } ?: UNKNOWN
        }
    }
} 