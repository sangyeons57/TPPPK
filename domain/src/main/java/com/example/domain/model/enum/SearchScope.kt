// 경로: domain/model/SearchScope.kt (신규 생성 또는 이동)
package com.example.domain.model

enum class SearchScope(val displayName: String) {
    ALL("전체"),
    MESSAGES("메시지"),
    USERS("사용자")
    // 필요시 채널, 파일 등 추가
}