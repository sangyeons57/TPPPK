package com.example.websocket.constants;

/**
 * WebSocket 메시지 페이로드 키 상수 관리
 *
 * WebSocket 메시지의 payload 맵에서 사용되는 모든 키들을 중앙에서 관리한다.
 * 클라이언트와 서버 간 일관성을 보장하고 오타로 인한 오류를 방지한다.
 */
public final class PayloadConstants {

    // ================================
    // 기본 텍스트 메시지 페이로드 키
    // ================================

    /** 메시지 텍스트 내용 */
    public static final String CONTENT = "content";

    /** 메시지 제목 (시스템 메시지 등에서 사용) */
    public static final String TITLE = "title";

    /** 메시지 부제목 또는 설명 */
    public static final String SUBTITLE = "subtitle";

    // ================================
    // 파일 및 미디어 관련 페이로드 키
    // ================================

    /** 이미지 URL */
    public static final String IMAGE_URL = "imageUrl";

    /** 파일 URL */
    public static final String FILE_URL = "fileUrl";

    /** 파일 이름 */
    public static final String FILE_NAME = "fileName";

    /** 파일 크기 (바이트) */
    public static final String FILE_SIZE = "fileSize";

    /** 파일 타입/확장자 */
    public static final String FILE_TYPE = "fileType";

    /** 파일 MIME 타입 */
    public static final String MIME_TYPE = "mimeType";

    /** 썸네일 URL (이미지/비디오용) */
    public static final String THUMBNAIL_URL = "thumbnailUrl";

    /** 업로드 진행률 (0-100) */
    public static final String UPLOAD_PROGRESS = "uploadProgress";

    // ================================
    // 시스템 메시지 관련 페이로드 키
    // ================================

    /** 시스템 메시지 타입 */
    public static final String SYSTEM_TYPE = "systemType";

    /** 관련 사용자 ID */
    public static final String USER_ID = "userId";

    /** 관련 사용자 이름 */
    public static final String USER_NAME = "userName";

    /** 액션 타입 (입장, 퇴장, 초대 등) */
    public static final String ACTION_TYPE = "actionType";

    /** 대상 사용자 ID */
    public static final String TARGET_USER_ID = "targetUserId";

    /** 대상 사용자 이름 */
    public static final String TARGET_USER_NAME = "targetUserName";

    // ================================
    // 프로젝트 관련 페이로드 키
    // ================================

    /** 프로젝트 이름 */
    public static final String PROJECT_NAME = "projectName";

    /** 채널 이름 */
    public static final String CHANNEL_NAME = "channelName";

    /** 초대 코드 */
    public static final String INVITE_CODE = "inviteCode";

    /** 역할/권한 */
    public static final String ROLE = "role";

    // ================================
    // 메타데이터 관련 페이로드 키
    // ================================

    /** 클라이언트 정보 */
    public static final String CLIENT_INFO = "clientInfo";

    /** 디바이스 타입 */
    public static final String DEVICE_TYPE = "deviceType";

    /** 앱 버전 */
    public static final String APP_VERSION = "appVersion";

    /** 타임스탬프 (메타데이터용) */
    public static final String TIMESTAMP_META = "timestampMeta";

    // ================================
    // 오류 관련 페이로드 키
    // ================================

    /** 오류 메시지 */
    public static final String ERROR_MESSAGE = "errorMessage";

    /** 오류 코드 */
    public static final String ERROR_CODE = "errorCode";

    /** 오류 상세 정보 */
    public static final String ERROR_DETAILS = "errorDetails";

    /** 스택 트레이스 (디버그용) */
    public static final String STACK_TRACE = "stackTrace";

    // ================================
    // 알림 관련 페이로드 키
    // ================================

    /** 알림 제목 */
    public static final String NOTIFICATION_TITLE = "notificationTitle";

    /** 알림 내용 */
    public static final String NOTIFICATION_BODY = "notificationBody";

    /** 알림 아이콘 */
    public static final String NOTIFICATION_ICON = "notificationIcon";

    /** 알림 액션 */
    public static final String NOTIFICATION_ACTION = "notificationAction";

    // ================================
    // 인증 관련 페이로드 키
    // ================================

    /** 인증 토큰 */
    public static final String AUTH_TOKEN = "authToken";

    /** 리프레시 토큰 */
    public static final String REFRESH_TOKEN = "refreshToken";

    /** 토큰 만료 시간 */
    public static final String TOKEN_EXPIRES_AT = "tokenExpiresAt";

    // ================================
    // 기타 통신 관련 페이로드 키
    // ================================

    /** 요청 ID (추적용) */
    public static final String REQUEST_ID = "requestId";

    /** 응답 상태 */
    public static final String RESPONSE_STATUS = "responseStatus";

    /** 추가 데이터 */
    public static final String EXTRA_DATA = "extraData";

    /** 설정 정보 */
    public static final String CONFIG_DATA = "configData";

    // Private constructor to prevent instantiation
    private PayloadConstants() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}