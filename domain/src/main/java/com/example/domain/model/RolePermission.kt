package com.example.domain.model

/**
 * 프로젝트 내 역할에 부여될 수 있는 권한 종류를 정의하는 Enum 클래스
 *
 * @property description 각 권한에 대한 UI 표시용 설명 문자열
 */
enum class RolePermission(val description: String) {
    // --- 멤버 관련 ---
    MANAGE_MEMBERS("멤버 전체 관리 권한"),         // 멤버관련 전체 권한
    INVITE_MEMBERS("멤버 초대 권한"),         // 새 멤버를 프로젝트에 초대할 수 있는 권한
    KICK_MEMBERS("멤버 추방 권한"),           // 다른 멤버를 프로젝트에서 추방할 수 있는 권한 (신중히 사용)

    // --- 역할 관련 ---
    MANAGE_ROLES("역할 생성/편집/삭제 권한"), // 역할을 관리(추가, 수정, 삭제)할 수 있는 권한
    ASSIGN_ROLES("멤버에게 역할 할당 권한"),   // 다른 멤버에게 역할을 부여하거나 변경할 수 있는 권한

    // --- 채널/카테고리 관련 ---
    MANAGE_CHANNELS("채널 생성/편집/삭제 권한"), // 채널/카테고리를 관리할 수 있는 권한
    // MANAGE_CATEGORY("카테고리 생성/편집/삭제 권한"), // 채널과 통합하거나 분리 가능

    // --- 메시지 관련 ---
    DELETE_OTHERS_MESSAGES("다른 멤버 메시지 삭제 권한"), // 다른 사람의 메시지를 삭제할 수 있는 권한
    PIN_MESSAGES("메시지 고정 권한"),           // 중요 메시지를 채널 상단에 고정하는 권한

    // --- 프로젝트 설정 관련 ---
    EDIT_PROJECT_INFO("프로젝트 이름/정보 변경 권한"), // 프로젝트의 이름이나 설명을 변경하는 권한
    MANAGE_PROJECT_SETTINGS("프로젝트 전반 설정 변경 권 Adası"), // 초대 방식, 공개 여부 등 주요 설정 변경 권한

    // --- 기타 ---
    CREATE_SCHEDULE("일정 생성/편집 권한"),      // 프로젝트 일정을 생성하거나 편집하는 권한
    MENTION_EVERYONE("@everyone 언급 권한"),       // @everyone, @here 등 전체 알림을 보낼 수 있는 권한

    // --- 채널 관련 권한 (ChannelPermissionType에서 이전) ---
    READ_MESSAGES("채널 메시지 읽기 권한"),           // 채널의 메시지를 읽을 수 있는 권한
    SEND_MESSAGES("채널 메시지 전송 권한"),         // 채널에 메시지를 보낼 수 있는 권한
    // MANAGE_MESSAGES 대신 DELETE_MESSAGES 와 DELETE_OTHERS_MESSAGES 사용 고려
    DELETE_MESSAGES("자신의 메시지 삭제 권한"),       // 자신이 보낸 메시지를 삭제할 수 있는 권한
    UPLOAD_FILES("파일 업로드 권한"),            // 채널에 파일을 업로드할 수 있는 권한
    MENTION_MEMBERS("멤버 언급(@) 권한"),          // 채널에서 다른 멤버를 @로 언급할 수 있는 권한
    MANAGE_MESSAGE_THREADS("메시지 스레드 관리 권한") // 메시지 스레드를 생성/관리하는 권한 (예시)

    // TODO: 앱의 실제 기획에 맞게 필요한 권한들을 추가, 수정, 삭제하세요.
}