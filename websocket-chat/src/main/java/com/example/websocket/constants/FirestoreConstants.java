package com.example.websocket.constants;

/**
 * Firestore 데이터베이스 관련 상수 관리
 *
 * Firestore 컬렉션명, 필드명, 문서 경로 등을 중앙에서 관리한다.
 * 클라이언트와 서버 간 일관성을 보장하고 데이터베이스 구조 변경 시 유지보수성을 향상시킨다.
 */
public final class FirestoreConstants {

    // ================================
    // 컬렉션 이름
    // ================================

    /** 메시지 컬렉션 */
    public static final String COLLECTION_MESSAGES = "messages";

    /** 채널 컬렉션 */
    public static final String COLLECTION_DM_CHANNELS = "dm_channels";

    public static final String COLLECTION_PROJECT_CHANNELS = "project_channels";

    /** 프로젝트 컬렉션 */
    public static final String COLLECTION_PROJECTS = "projects";

    /** 사용자 컬렉션 */
    public static final String COLLECTION_USERS = "users";

    /** 프로젝트 멤버 컬렉션 */
    public static final String COLLECTION_PROJECT_MEMBERS = "members";

    // ================================
    // 공통 필드 이름
    // ================================

    /** 문서 ID 필드 */
    public static final String FIELD_ID = "id";

    /** 생성 시간 필드 */
    public static final String FIELD_CREATED_AT = "createdAt";

    /** 수정 시간 필드 */
    public static final String FIELD_UPDATED_AT = "updatedAt";

    /** 삭제 여부 필드 */
    public static final String FIELD_IS_DELETED = "isDeleted";

    /** 삭제 시간 필드 */
    public static final String FIELD_DELETED_AT = "deletedAt";

    /** 버전 필드 */
    public static final String FIELD_VERSION = "version";

    // ================================
    // 메시지 관련 필드
    // ================================

    /** 메시지 발신자 ID */
    public static final String FIELD_SENDER_ID = "senderId";

    /** 메시지 타입 */
    public static final String FIELD_MESSAGE_TYPE = "messageType";

    /** 메시지 페이로드 */
    public static final String FIELD_PAYLOAD = "payload";

    /** 답장 대상 메시지 ID */
    public static final String FIELD_REPLY_TO_MESSAGE_ID = "replyToMessageId";

    /** 메시지가 속한 채널 ID */
    public static final String FIELD_CHANNEL_ID = "channelId";

    /** 메시지 멘션 목록 */
    public static final String FIELD_MENTIONS = "mentions";

    /** 메시지 반응 목록 */
    public static final String FIELD_REACTIONS = "reactions";

    /** 메시지 편집 이력 */
    public static final String FIELD_EDIT_HISTORY = "editHistory";

    /** 메시지 읽음 상태 */
    public static final String FIELD_READ_BY = "readBy";

    // ================================
    // 사용자 관련 필드
    // ================================

    /** 사용자 이메일 */
    public static final String FIELD_EMAIL = "email";

    /** 사용자 이름 */
    public static final String FIELD_USERNAME = "username";

    /** 사용자 표시 이름 */
    public static final String FIELD_DISPLAY_NAME = "displayName";

    /** 사용자 아바타 URL */
    public static final String FIELD_AVATAR_URL = "avatarUrl";

    /** 사용자 상태 (온라인/오프라인) */
    public static final String FIELD_STATUS = "status";

    /** 마지막 온라인 시간 */
    public static final String FIELD_LAST_SEEN = "lastSeen";

    /** 사용자 설정 */
    public static final String FIELD_SETTINGS = "settings";

    // ================================
    // 프로젝트 관련 필드
    // ================================

    /** 프로젝트 이름 */
    public static final String FIELD_PROJECT_NAME = "projectName";

    /** 프로젝트 설명 */
    public static final String FIELD_PROJECT_DESCRIPTION = "projectDescription";

    /** 프로젝트 소유자 ID */
    public static final String FIELD_OWNER_ID = "ownerId";

    /** 프로젝트 멤버 목록 */
    public static final String FIELD_MEMBERS = "members";

    /** 프로젝트 설정 */
    public static final String FIELD_PROJECT_SETTINGS = "projectSettings";

    /** 프로젝트 아이콘 URL */
    public static final String FIELD_PROJECT_ICON_URL = "projectIconUrl";

    // ================================
    // 채널 관련 필드
    // ================================

    /** 채널 이름 */
    public static final String FIELD_CHANNEL_NAME = "channelName";

    /** 채널 설명 */
    public static final String FIELD_CHANNEL_DESCRIPTION = "channelDescription";

    /** 채널 타입 */
    public static final String FIELD_CHANNEL_TYPE = "channelType";

    /** 채널이 속한 프로젝트 ID */
    public static final String FIELD_PROJECT_ID = "projectId";

    /** 채널 비공개 여부 */
    public static final String FIELD_IS_PRIVATE = "isPrivate";

    /** 채널 멤버 수 */
    public static final String FIELD_MEMBER_COUNT = "memberCount";

    /** 마지막 메시지 시간 */
    public static final String FIELD_LAST_MESSAGE_AT = "lastMessageAt";

    // ================================
    // 초대 관련 필드
    // ================================

    /** 초대 코드 */
    public static final String FIELD_INVITE_CODE = "inviteCode";

    /** 초대자 ID */
    public static final String FIELD_INVITER_ID = "inviterId";

    /** 초대 받은 사용자 ID */
    public static final String FIELD_INVITEE_ID = "inviteeId";

    /** 초대 받은 사용자 이메일 */
    public static final String FIELD_INVITEE_EMAIL = "inviteeEmail";

    /** 초대 상태 */
    public static final String FIELD_INVITE_STATUS = "inviteStatus";

    /** 초대 만료 시간 */
    public static final String FIELD_EXPIRES_AT = "expiresAt";

    /** 초대 사용 시간 */
    public static final String FIELD_USED_AT = "usedAt";

    // ================================
    // 파일 관련 필드
    // ================================

    /** 파일 이름 */
    public static final String FIELD_FILE_NAME = "fileName";

    /** 파일 크기 */
    public static final String FIELD_FILE_SIZE = "fileSize";

    /** 파일 타입 */
    public static final String FIELD_FILE_TYPE = "fileType";

    /** 파일 MIME 타입 */
    public static final String FIELD_MIME_TYPE = "mimeType";

    /** 파일 URL */
    public static final String FIELD_FILE_URL = "fileUrl";

    /** 썸네일 URL */
    public static final String FIELD_THUMBNAIL_URL = "thumbnailUrl";

    /** 파일 업로드한 사용자 ID */
    public static final String FIELD_UPLOADED_BY = "uploadedBy";

    /** 파일 업로드 시간 */
    public static final String FIELD_UPLOADED_AT = "uploadedAt";

    // ================================
    // 알림 관련 필드
    // ================================

    /** 알림 타입 */
    public static final String FIELD_NOTIFICATION_TYPE = "notificationType";

    /** 알림 제목 */
    public static final String FIELD_NOTIFICATION_TITLE = "notificationTitle";

    /** 알림 내용 */
    public static final String FIELD_NOTIFICATION_BODY = "notificationBody";

    /** 알림 읽음 여부 */
    public static final String FIELD_IS_READ = "isRead";

    /** 알림 읽은 시간 */
    public static final String FIELD_READ_AT = "readAt";

    /** 알림 대상 사용자 ID */
    public static final String FIELD_RECIPIENT_ID = "recipientId";

    // ================================
    // 역할 및 권한 관련 필드
    // ================================

    /** 사용자 역할 */
    public static final String FIELD_ROLE = "role";

    /** 권한 목록 */
    public static final String FIELD_PERMISSIONS = "permissions";

    /** 역할 부여 시간 */
    public static final String FIELD_ASSIGNED_AT = "assignedAt";

    /** 역할 부여한 사용자 ID */
    public static final String FIELD_ASSIGNED_BY = "assignedBy";

    // ================================
    // 세션 관련 필드
    // ================================

    /** 세션 토큰 */
    public static final String FIELD_SESSION_TOKEN = "sessionToken";

    /** 디바이스 정보 */
    public static final String FIELD_DEVICE_INFO = "deviceInfo";

    /** 세션 시작 시간 */
    public static final String FIELD_SESSION_START = "sessionStart";

    /** 세션 종료 시간 */
    public static final String FIELD_SESSION_END = "sessionEnd";

    /** IP 주소 */
    public static final String FIELD_IP_ADDRESS = "ipAddress";

    /** 사용자 에이전트 */
    public static final String FIELD_USER_AGENT = "userAgent";

    // ================================
    // 유틸리티 메서드
    // ================================

    public static String getDMChannelsCollectionPath(String channelId) {
        return COLLECTION_DM_CHANNELS + "/" + channelId + "/" + COLLECTION_MESSAGES;
     }

    public static String getProjectChannelsCollectionPath(String projectId, String channelId) {
        return COLLECTION_PROJECTS + "/" + projectId + "/" + COLLECTION_PROJECT_CHANNELS + "/" + channelId + "/" + COLLECTION_MESSAGES;
    }

}