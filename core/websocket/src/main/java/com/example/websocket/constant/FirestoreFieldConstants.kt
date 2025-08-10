package com.example.websocket.constant

/**
 * Firestore 필드 상수 관리 (클라이언트 측)
 *
 * 서버의 FirestoreConstants.java와 일치하는 클라이언트 측 Firestore 필드 상수들을 정의한다.
 * 클라이언트에서 Firestore와 직접 통신할 때 또는 데이터 구조를 참조할 때 사용한다.
 *
 * 서버와 클라이언트 간 데이터베이스 스키마 일관성을 보장한다.
 */
object FirestoreFieldConstants {

    // ================================
    // 컬렉션 이름 (서버와 일치)
    // ================================

    /** 메시지 컬렉션 */
    const val COLLECTION_MESSAGES = "messages"

    /** 채널 컬렉션 */
    const val COLLECTION_CHANNELS = "channels"

    /** 프로젝트 컬렉션 */
    const val COLLECTION_PROJECTS = "projects"

    /** 사용자 컬렉션 */
    const val COLLECTION_USERS = "users"

    /** 프로젝트 멤버 컬렉션 */
    const val COLLECTION_PROJECT_MEMBERS = "projectMembers"

    /** 채널 멤버 컬렉션 */
    const val COLLECTION_CHANNEL_MEMBERS = "channelMembers"

    /** 초대 컬렉션 */
    const val COLLECTION_INVITATIONS = "invitations"

    /** 알림 컬렉션 */
    const val COLLECTION_NOTIFICATIONS = "notifications"

    /** 파일 메타데이터 컬렉션 */
    const val COLLECTION_FILES = "files"

    /** 사용자 세션 컬렉션 */
    const val COLLECTION_USER_SESSIONS = "userSessions"

    // ================================
    // 공통 필드 이름 (서버와 일치)
    // ================================

    /** 문서 ID 필드 */
    const val FIELD_ID = "id"

    /** 생성 시간 필드 */
    const val FIELD_CREATED_AT = "createdAt"

    /** 수정 시간 필드 */
    const val FIELD_UPDATED_AT = "updatedAt"

    /** 삭제 여부 필드 */
    const val FIELD_IS_DELETED = "isDeleted"

    /** 삭제 시간 필드 */
    const val FIELD_DELETED_AT = "deletedAt"

    /** 버전 필드 */
    const val FIELD_VERSION = "version"

    // ================================
    // 메시지 관련 필드 (서버와 일치)
    // ================================

    /** 메시지 발신자 ID */
    const val FIELD_SENDER_ID = "senderId"

    /** 메시지 타입 */
    const val FIELD_MESSAGE_TYPE = "messageType"

    /** 메시지 페이로드 */
    const val FIELD_PAYLOAD = "payload"

    /** 답장 대상 메시지 ID */
    const val FIELD_REPLY_TO_MESSAGE_ID = "replyToMessageId"

    /** 메시지가 속한 채널 ID */
    const val FIELD_CHANNEL_ID = "channelId"

    /** 메시지 멘션 목록 */
    const val FIELD_MENTIONS = "mentions"

    /** 메시지 반응 목록 */
    const val FIELD_REACTIONS = "reactions"

    /** 메시지 편집 이력 */
    const val FIELD_EDIT_HISTORY = "editHistory"

    /** 메시지 읽음 상태 */
    const val FIELD_READ_BY = "readBy"

    // ================================
    // 사용자 관련 필드 (서버와 일치)
    // ================================

    /** 사용자 이메일 */
    const val FIELD_EMAIL = "email"

    /** 사용자 이름 */
    const val FIELD_USERNAME = "username"

    /** 사용자 표시 이름 */
    const val FIELD_DISPLAY_NAME = "displayName"

    /** 사용자 아바타 URL */
    const val FIELD_AVATAR_URL = "avatarUrl"

    /** 사용자 상태 */
    const val FIELD_STATUS = "status"

    /** 마지막 온라인 시간 */
    const val FIELD_LAST_SEEN = "lastSeen"

    /** 사용자 설정 */
    const val FIELD_SETTINGS = "settings"

    // ================================
    // 프로젝트 관련 필드 (서버와 일치)
    // ================================

    /** 프로젝트 이름 */
    const val FIELD_PROJECT_NAME = "projectName"

    /** 프로젝트 설명 */
    const val FIELD_PROJECT_DESCRIPTION = "projectDescription"

    /** 프로젝트 소유자 ID */
    const val FIELD_OWNER_ID = "ownerId"

    /** 프로젝트 멤버 목록 */
    const val FIELD_MEMBERS = "members"

    /** 프로젝트 설정 */
    const val FIELD_PROJECT_SETTINGS = "projectSettings"

    /** 프로젝트 아이콘 URL */
    const val FIELD_PROJECT_ICON_URL = "projectIconUrl"

    // ================================
    // 채널 관련 필드 (서버와 일치)
    // ================================

    /** 채널 이름 */
    const val FIELD_CHANNEL_NAME = "channelName"

    /** 채널 설명 */
    const val FIELD_CHANNEL_DESCRIPTION = "channelDescription"

    /** 채널 타입 */
    const val FIELD_CHANNEL_TYPE = "channelType"

    /** 채널이 속한 프로젝트 ID */
    const val FIELD_PROJECT_ID = "projectId"

    /** 채널 비공개 여부 */
    const val FIELD_IS_PRIVATE = "isPrivate"

    /** 채널 멤버 수 */
    const val FIELD_MEMBER_COUNT = "memberCount"

    /** 마지막 메시지 시간 */
    const val FIELD_LAST_MESSAGE_AT = "lastMessageAt"

    // ================================
    // 초대 관련 필드 (서버와 일치)
    // ================================

    /** 초대 코드 */
    const val FIELD_INVITE_CODE = "inviteCode"

    /** 초대자 ID */
    const val FIELD_INVITER_ID = "inviterId"

    /** 초대 받은 사용자 ID */
    const val FIELD_INVITEE_ID = "inviteeId"

    /** 초대 받은 사용자 이메일 */
    const val FIELD_INVITEE_EMAIL = "inviteeEmail"

    /** 초대 상태 */
    const val FIELD_INVITE_STATUS = "inviteStatus"

    /** 초대 만료 시간 */
    const val FIELD_EXPIRES_AT = "expiresAt"

    /** 초대 사용 시간 */
    const val FIELD_USED_AT = "usedAt"

    // ================================
    // 파일 관련 필드 (서버와 일치)
    // ================================

    /** 파일 이름 */
    const val FIELD_FILE_NAME = "fileName"

    /** 파일 크기 */
    const val FIELD_FILE_SIZE = "fileSize"

    /** 파일 타입 */
    const val FIELD_FILE_TYPE = "fileType"

    /** 파일 MIME 타입 */
    const val FIELD_MIME_TYPE = "mimeType"

    /** 파일 URL */
    const val FIELD_FILE_URL = "fileUrl"

    /** 썸네일 URL */
    const val FIELD_THUMBNAIL_URL = "thumbnailUrl"

    /** 파일 업로드한 사용자 ID */
    const val FIELD_UPLOADED_BY = "uploadedBy"

    /** 파일 업로드 시간 */
    const val FIELD_UPLOADED_AT = "uploadedAt"

    // ================================
    // 알림 관련 필드 (서버와 일치)
    // ================================

    /** 알림 타입 */
    const val FIELD_NOTIFICATION_TYPE = "notificationType"

    /** 알림 제목 */
    const val FIELD_NOTIFICATION_TITLE = "notificationTitle"

    /** 알림 내용 */
    const val FIELD_NOTIFICATION_BODY = "notificationBody"

    /** 알림 읽음 여부 */
    const val FIELD_IS_READ = "isRead"

    /** 알림 읽은 시간 */
    const val FIELD_READ_AT = "readAt"

    /** 알림 대상 사용자 ID */
    const val FIELD_RECIPIENT_ID = "recipientId"

    // ================================
    // 역할 및 권한 관련 필드 (서버와 일치)
    // ================================

    /** 사용자 역할 */
    const val FIELD_ROLE = "role"

    /** 권한 목록 */
    const val FIELD_PERMISSIONS = "permissions"

    /** 역할 부여 시간 */
    const val FIELD_ASSIGNED_AT = "assignedAt"

    /** 역할 부여한 사용자 ID */
    const val FIELD_ASSIGNED_BY = "assignedBy"

    // ================================
    // 세션 관련 필드 (서버와 일치)
    // ================================

    /** 세션 토큰 */
    const val FIELD_SESSION_TOKEN = "sessionToken"

    /** 디바이스 정보 */
    const val FIELD_DEVICE_INFO = "deviceInfo"

    /** 세션 시작 시간 */
    const val FIELD_SESSION_START = "sessionStart"

    /** 세션 종료 시간 */
    const val FIELD_SESSION_END = "sessionEnd"

    /** IP 주소 */
    const val FIELD_IP_ADDRESS = "ipAddress"

    /** 사용자 에이전트 */
    const val FIELD_USER_AGENT = "userAgent"

    // ================================
    // 유틸리티 함수 (서버와 일치하는 로직)
    // ================================

    /**
     * 메시지 컬렉션 경로 생성
     */
    fun getMessagesCollectionPath(channelId: String): String {
        return "$COLLECTION_CHANNELS/$channelId/$COLLECTION_MESSAGES"
    }

    /**
     * 프로젝트 멤버 컬렉션 경로 생성
     */
    fun getProjectMembersCollectionPath(projectId: String): String {
        return "$COLLECTION_PROJECTS/$projectId/$COLLECTION_PROJECT_MEMBERS"
    }

    /**
     * 채널 멤버 컬렉션 경로 생성
     */
    fun getChannelMembersCollectionPath(channelId: String): String {
        return "$COLLECTION_CHANNELS/$channelId/$COLLECTION_CHANNEL_MEMBERS"
    }

    /**
     * 사용자 알림 컬렉션 경로 생성
     */
    fun getUserNotificationsCollectionPath(userId: String): String {
        return "$COLLECTION_USERS/$userId/$COLLECTION_NOTIFICATIONS"
    }

    /**
     * 프로젝트 채널 컬렉션 경로 생성
     */
    fun getProjectChannelsCollectionPath(projectId: String): String {
        return "$COLLECTION_PROJECTS/$projectId/$COLLECTION_CHANNELS"
    }

    /**
     * 사용자 프로젝트 컬렉션 경로 생성 (사용자가 참여한 프로젝트)
     */
    fun getUserProjectsCollectionPath(userId: String): String {
        return "$COLLECTION_USERS/$userId/$COLLECTION_PROJECTS"
    }

    /**
     * 파일 메타데이터 문서 경로 생성
     */
    fun getFileMetadataDocumentPath(fileId: String): String {
        return "$COLLECTION_FILES/$fileId"
    }

    /**
     * 사용자 세션 문서 경로 생성
     */
    fun getUserSessionDocumentPath(userId: String, sessionId: String): String {
        return "$COLLECTION_USER_SESSIONS/${userId}_$sessionId"
    }
}