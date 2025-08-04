package com.example.websocket.constants;

/**
 * WebSocket 이벤트 타입 상수 관리 (Java 서버 측)
 * 클라이언트의 WebSocketEventTypes.kt와 동일한 상수값들을 정의
 * 
 * 이벤트 타입 변경 시 클라이언트와 서버 양쪽에서 동시에 수정 필요
 */
public final class WebSocketEventConstants {
    
    // ================================
    // 기본 연결 및 인증 이벤트
    // ================================
    
    /** 인증 요청 */
    public static final String AUTH = "AUTH";
    
    /** 인증 성공 알림 */
    public static final String AUTH_SUCCESS = "AUTH_SUCCESS";
    
    /** 방 입장 요청 */
    public static final String JOIN_ROOM = "JOIN_ROOM";
    
    /** 방 나가기 요청 */
    public static final String LEAVE_ROOM = "LEAVE_ROOM";
    
    /** 방 입장 성공 알림 */
    public static final String JOINED_ROOM = "JOINED_ROOM";
    
    /** 방 나가기 성공 알림 */
    public static final String LEFT_ROOM = "LEFT_ROOM";
    
    /** 방 입장 성공 알림 (별칭) */
    public static final String ROOM_JOINED = "ROOM_JOINED";
    
    /** 방 나가기 성공 알림 (별칭) */
    public static final String ROOM_LEFT = "ROOM_LEFT";
    
    // ================================
    // 메시지 관련 이벤트
    // ================================
    
    /** 일반 메시지 전송 */
    public static final String MESSAGE = "MESSAGE";
    
    /** 메시지 편집 */
    public static final String EDIT_MESSAGE = "EDIT_MESSAGE";
    
    /** 메시지 삭제 */
    public static final String DELETE_MESSAGE = "DELETE_MESSAGE";
    
    // ================================
    // ACK (성공 응답) 이벤트
    // ================================
    
    /** 메시지 전송 성공 확인 */
    public static final String MESSAGE_ACK = "MESSAGE_ACK";
    
    /** 메시지 편집 성공 확인 */
    public static final String EDIT_MESSAGE_ACK = "EDIT_MESSAGE_ACK";
    
    /** 메시지 삭제 성공 확인 */
    public static final String DELETE_MESSAGE_ACK = "DELETE_MESSAGE_ACK";
    
    // ================================
    // FAILED (실패 응답) 이벤트
    // ================================
    
    /** 메시지 전송 실패 알림 */
    public static final String MESSAGE_FAILED = "MESSAGE_FAILED";
    
    /** 메시지 편집 실패 알림 */
    public static final String EDIT_MESSAGE_FAILED = "EDIT_MESSAGE_FAILED";
    
    /** 메시지 삭제 실패 알림 */
    public static final String DELETE_MESSAGE_FAILED = "DELETE_MESSAGE_FAILED";
    
    // ================================
    // 시스템 이벤트
    // ================================
    
    /** 시스템 메시지 */
    public static final String SYSTEM = "SYSTEM";
    
    /** 에러 메시지 */
    public static final String ERROR = "ERROR";
    

    
    /** 일반 ACK */
    public static final String ACK = "ACK";
    
    // ================================
    // 채널 타입 상수
    // ================================
    
    /** DM (Direct Message) 채널 */
    public static final String CHANNEL_TYPE_DM = "DM";
    
    /** 프로젝트 채널 */
    public static final String CHANNEL_TYPE_PROJECT = "PROJECT";
    
    // ================================
    // 유틸리티 메서드
    // ================================
    
    /**
     * ACK 이벤트인지 확인
     */
    public static boolean isAckEvent(String eventType) {
        return MESSAGE_ACK.equals(eventType) || 
               EDIT_MESSAGE_ACK.equals(eventType) || 
               DELETE_MESSAGE_ACK.equals(eventType);
    }
    
    /**
     * FAILED 이벤트인지 확인
     */
    public static boolean isFailedEvent(String eventType) {
        return MESSAGE_FAILED.equals(eventType) || 
               EDIT_MESSAGE_FAILED.equals(eventType) || 
               DELETE_MESSAGE_FAILED.equals(eventType);
    }
    
    /**
     * 메시지 관련 이벤트인지 확인
     */
    public static boolean isMessageEvent(String eventType) {
        return MESSAGE.equals(eventType) || 
               EDIT_MESSAGE.equals(eventType) || 
               DELETE_MESSAGE.equals(eventType);
    }
    
    /**
     * 시스템 이벤트인지 확인
     */
    public static boolean isSystemEvent(String eventType) {
        return SYSTEM.equals(eventType) || 
               ERROR.equals(eventType) || 
               JOINED_ROOM.equals(eventType) || 
               LEFT_ROOM.equals(eventType) || 
               AUTH_SUCCESS.equals(eventType);
    }
    
    // Private constructor to prevent instantiation
    private WebSocketEventConstants() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}