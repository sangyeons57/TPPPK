package com.example.domain.model.ui.search

/**
 * 사용자 검색 결과를 나타내는 UI 모델 클래스
 * 검색 결과 화면에서 표시되는 사용자 정보를 포함합니다.
 * 
 * @property id 사용자의 고유 식별자 
 * @property userId 사용자 ID
 * @property userName 사용자 이름
 * @property displayName 표시 이름
 * @property profileImageUrl 프로필 이미지 URL
 * @property status 사용자 상태 메시지
 * @property isOnline 온라인 상태 여부
 * @property matchReason 검색 일치 이유 (예: 이름, 상태 등)
 */
data class UserResult(
    override val id: String,
    val userId: String,
    val userName: String,
    val displayName: String,
    val profileImageUrl: String?,
    val status: String?,
    val isOnline: Boolean = false,
    val matchReason: String? = null
) : SearchResultItem {
    override val type: SearchResultType = SearchResultType.USER
    
    companion object {
        /**
         * 데이터 레이어의 SearchResult.User 객체를 UI 레이어의 UserResult로 변환
         *
         * @param user 데이터 모델 사용자 검색 결과
         * @return UI 모델 사용자 검색 결과
         */
        fun fromDataModel(user: com.example.domain.model.data.search.SearchResult.User): UserResult {
            return UserResult(
                id = user.id,
                userId = user.userId,
                userName = user.userName,
                displayName = user.displayName,
                profileImageUrl = user.profileUrl,
                status = user.status,
                isOnline = user.online
            )
        }
    }
}
