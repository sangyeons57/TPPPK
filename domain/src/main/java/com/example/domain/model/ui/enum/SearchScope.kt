package com.example.domain.model.ui.search

/**
 * 검색 범위를 정의하는 UI 열거형
 * 사용자 인터페이스에서 검색할 범위를 지정하는 데 사용됩니다.
 */
enum class SearchScope {
    /**
     * 모든 범위에서 검색 (메시지, 사용자, 프로젝트 등)
     */
    ALL,

    /**
     * 메시지만 검색
     */
    MESSAGES,

    /**
     * 사용자만 검색
     */
    USERS,

    /**
     * 프로젝트만 검색
     */
    PROJECTS;

    /**
     * 현재 검색 범위의 이름을 반환합니다.
     * 
     * @return 검색 범위 이름 (한국어)
     */
    fun getDisplayName(): String {
        return when (this) {
            ALL -> "전체"
            MESSAGES -> "메시지"
            USERS -> "사용자"
            PROJECTS -> "프로젝트"
        }
    }
}
