package com.example.domain.vo.message

/**
 * 메시지 타입을 나타내는 열거형
 *
 * 각 타입별 payload 구조:
 * - TEXT: {"content": "실제 메시지 내용"} (일반 텍스트)
 * - SYSTEM: {"content": "시스템 메시지", "systemType": "..."} (일반 시스템 메시지)
 * - SYSTEM_PROJECT_JOIN: {"projectId": "...", "projectName": "...", "actionText": "참여하기"}
 * - SYSTEM_PROJECT_LEAVE: {"projectId": "...", "projectName": "...", "actionText": "떠나기"}
 * - SYSTEM_DATE: {"date": "2024-01-01", "displayText": "2024년 1월 1일"}
 * - SYSTEM_CHAT_START: {"channelName": "...", "welcomeText": "채팅이 시작되었습니다"}
 * - SYSTEM_MEMBER_INVITATION: {"projectId": "...", "projectName": "...", "inviterName": "...", "targetUserId": "...", "actionText": "멤버로 추가"}
 * - SYSTEM_USER_INVITE: {"inviterName": "...", "projectName": "...", "actionText": "초대됨"}
 * - PROJECT_INVITE: {"projectId": "...", "projectName": "...", "inviterName": "...", "invitationId": "...", "actionText": "참여하기"}
 */
enum class MessageType {
    /**
     * 일반 텍스트 메시지
     * 이미지도 attachment에 추가해서 사용가능
     * Payload: {"content": "메시지 내용", "attachments": [...]}
     */
    TEXT,

    /**
     * 일반 시스템 메시지
     * Payload: {"content": "시스템 메시지", "systemType": "..."}
     */
    SYSTEM,

    /**
     * 프로젝트 참여 시스템 메시지
     * Payload: {"projectId": "...", "projectName": "...", "actionText": "참여하기"}
     */
    SYSTEM_PROJECT_JOIN,

    /**
     * 프로젝트 떠나기 시스템 메시지
     * Payload: {"projectId": "...", "projectName": "...", "actionText": "떠나기"}
     */
    SYSTEM_PROJECT_LEAVE,

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
    SYSTEM_MEMBER_INVITATION,

    /**
     * 사용자 초대 시스템 메시지
     * Payload: {"inviterName": "...", "projectName": "...", "actionText": "초대됨"}
     */
    SYSTEM_USER_INVITE,

    /**
     * 프로젝트 초대 메시지
     * Payload: {"projectId": "...", "projectName": "...", "inviterName": "...", "invitationId": "...", "actionText": "참여하기"}
     */
    PROJECT_INVITE
}