package com.example.domain.vo.message

/**
 * 메시지 타입을 나타내는 열거형
 *
 * 각 타입별 payload 구조:
 * - TEXT: {"content": "실제 메시지 내용"}
 * - SYSTEM_PROJECT_JOIN: {"projectId": "...", "projectName": "...", "actionText": "참여하기"}
 * - SYSTEM_DATE: {"date": "2024-01-01", "displayText": "2024년 1월 1일"}
 * - SYSTEM_CHAT_START: {"channelName": "...", "welcomeText": "채팅이 시작되었습니다"}
 */
enum class MessageType {
    /**
     * 일반 텍스트 메시지
     * Payload: {"content": "메시지 내용"}
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
     * 이미지 메시지 (향후 확장용)
     * Payload: {"imageUrl": "...", "caption": "..."}
     */
    IMAGE,

    /**
     * 파일 메시지 (향후 확장용)
     * Payload: {"fileUrl": "...", "fileName": "...", "fileSize": 1024}
     */
    FILE
}