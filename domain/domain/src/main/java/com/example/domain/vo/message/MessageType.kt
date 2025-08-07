package com.example.domain.vo.message

/**
 * 메시지 타입을 나타내는 열거형
 *
 * 각 타입별 payload 구조:
 * - TEXT: {"content": "실제 메시지 내용"} (일반 텍스트, 이미지, 파일 모두 포함)
 * - SYSTEM_PROJECT_JOIN: {"projectId": "...", "projectName": "...", "actionText": "참여하기"}
 * - SYSTEM_DATE: {"date": "2024-01-01", "displayText": "2024년 1월 1일"}
 * - SYSTEM_CHAT_START: {"channelName": "...", "welcomeText": "채팅이 시작되었습니다"}
 * - SYSTEM_MEMBER_INVITATION: {"projectId": "...", "projectName": "...", "inviterName": "...", "targetUserId": "...", "actionText": "멤버로 추가"}
 */
enum class MessageType {
    /**
     * 일반 메시지 (텍스트, 이미지, 파일 모두 포함)
     * Payload: {"content": "메시지 내용", "attachments": [...]}
     */
    TEXT,

    /**
     * 프로젝트 참여 시스템 메시지
     * Payload: {"projectId": "...", "projectName": "...", "actionText": "참여하기"}
     */
    SYSTEM_PROJECT_JOIN,

    /**
     * 날짜 표시 시스템 메시지
     * Payload: {"date": "2024-01-01", "displayText": "2024년 1월 1일"}
     */
    SYSTEM_DATE,

    /**
     * 채팅 시작 시스템 메시지
     * Payload: {"channelName": "...", "welcomeText": "채팅이 시작되었습니다"}
     */
    SYSTEM_CHAT_START,

    /**
     * 프로젝트 멤버 초대 시스템 메시지
     * Payload: {"projectId": "...", "projectName": "...", "inviterName": "...", "targetUserId": "...", "actionText": "멤버로 추가"}
     */
    SYSTEM_MEMBER_INVITATION
}