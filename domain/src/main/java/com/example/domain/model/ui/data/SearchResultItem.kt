package com.example.domain.model.ui.search

/**
 * 검색 결과 아이템의 기본 인터페이스
 * 여러 유형의 검색 결과(메시지, 사용자 등)가 공통적으로 구현해야 하는 UI 모델 인터페이스입니다.
 * 이 인터페이스는 UI 계층에서 사용될 검색 결과 아이템을 표현합니다.
 */
interface SearchResultItem {
    /**
     * 검색 결과 아이템의 고유 식별자
     */
    val id: String
    
    /**
     * 검색 결과 유형
     */
    val type: SearchResultType
}

/**
 * 검색 결과 유형을 정의하는 열거형
 */
enum class SearchResultType {
    MESSAGE,
    USER,
    PROJECT
}
