// 경로: domain/model/UserStatus.kt (신규 생성 또는 이동)
package com.example.domain.model

// TODO: 실제 앱 기획에 맞는 상태들로 수정/추가/삭제
enum class UserStatus(val displayName: String) {
    ONLINE("온라인"),
    AWAY("자리 비움"),
    DO_NOT_DISTURB("다른 용무 중"),
    OFFLINE("오프라인")
    // INVISIBLE("오프라인으로 표시") 등 추가 가능
}