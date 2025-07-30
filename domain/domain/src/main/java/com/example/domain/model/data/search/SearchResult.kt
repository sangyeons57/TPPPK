package com.example.domain.model.data.search

import java.time.Instant

/**
 * 검색 결과를 나타내는 데이터 도메인 모델
 * Repository가 반환하는 검색 결과 데이터 타입
 */
sealed class SearchResult {
    
    /**
     * 사용자 검색 결과
     * 
     * @property id 사용자의 고유 식별자
     * @property userId 사용자 ID
     * @property displayName 표시 이름
     * @property userName 사용자 이름 
     * @property profileUrl 프로필 이미지 URL
     * @property status 상태 메시지
     * @property online 온라인 상태 여부
     */
    data class User(
        val id: String,
        val userId: String,
        val displayName: String,
        val userName: String,
        val profileUrl: String?,
        val status: String?,
        val online: Boolean = false
    ) : SearchResult()
    
    /**
     * 메시지 검색 결과
     * 
     * @property id 메시지의 고유 식별자
     * @property messageId 메시지 ID
     * @property content 메시지 내용
     * @property timestamp 메시지 작성 시간
     * @property channelId 채널 ID
     * @property channelName 채널 이름
     * @property senderId 발신자 ID
     * @property senderName 발신자 이름
     */
    data class Message(
        val id: String,
        val messageId: String,
        val content: String,
        val timestamp: Instant,
        val channelId: String,
        val channelName: String,
        val senderId: String,
        val senderName: String
    ) : SearchResult()
    
    /**
     * 프로젝트 검색 결과
     * 
     * @property id 프로젝트 고유 식별자
     * @property projectId 프로젝트 ID
     * @property name 프로젝트 이름
     * @property description 프로젝트 설명
     * @property createdAt 생성 시간
     * @property thumbnailUrl 썸네일 이미지 URL
     */
    data class Project(
        val id: String,
        val projectId: String,
        val name: String,
        val description: String,
        val createdAt: Instant,
        val thumbnailUrl: String?
    ) : SearchResult()
}
