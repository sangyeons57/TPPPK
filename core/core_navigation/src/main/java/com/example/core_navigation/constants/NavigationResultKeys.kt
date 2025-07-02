package com.example.core_navigation.constants

/**
 * 네비게이션 결과 전달에 사용되는 키들을 중앙에서 관리하는 상수 객체
 * 
 * 도메인별로 그룹화하여 네임스페이스를 제공하고,
 * 타입 안전성과 유지보수성을 향상시킵니다.
 * 
 * 사용 예시:
 * ```kotlin
 * // 설정
 * navigationManger.setResult(NavigationResultKeys.Auth.SIGNUP_SUCCESS_EMAIL_VERIFICATION, true)
 * 
 * // 읽기
 * val result = navigationManger.getResult<Boolean>(NavigationResultKeys.Auth.SIGNUP_SUCCESS_EMAIL_VERIFICATION)
 * ```
 */
object NavigationResultKeys {
    
    /**
     * 인증 관련 네비게이션 결과 키들
     */
    object Auth {
        /** 회원가입 성공 후 이메일 인증 안내가 필요함을 나타내는 키 */
        const val SIGNUP_SUCCESS_EMAIL_VERIFICATION = "auth_signup_success_email_verification"
        
        /** 이메일 인증 완료 여부를 나타내는 키 */
        const val EMAIL_VERIFICATION_COMPLETED = "auth_email_verification_completed"
        
        /** 비밀번호 재설정 성공 여부를 나타내는 키 */
        const val PASSWORD_RESET_SUCCESS = "auth_password_reset_success"
        
        /** 로그아웃 완료 여부를 나타내는 키 */
        const val LOGOUT_COMPLETED = "auth_logout_completed"
    }
    
    /**
     * 프로젝트 관련 네비게이션 결과 키들
     */
    object Project {
        /** 프로젝트 생성 완료 여부와 생성된 프로젝트 ID */
        const val PROJECT_CREATED = "project_created"
        
        /** 프로젝트 수정 완료 여부 */
        const val PROJECT_UPDATED = "project_updated"
        
        /** 프로젝트 삭제 완료 여부 */
        const val PROJECT_DELETED = "project_deleted"
        
        /** 프로젝트 참여 완료 여부 */
        const val PROJECT_JOINED = "project_joined"
    }
    
    /**
     * 사용자 관련 네비게이션 결과 키들
     */
    object User {
        /** 프로필 수정 완료 여부 */
        const val PROFILE_UPDATED = "user_profile_updated"
        
        /** 친구 추가 완료 여부 */
        const val FRIEND_ADDED = "user_friend_added"
        
        /** 친구 삭제 완료 여부 */
        const val FRIEND_REMOVED = "user_friend_removed"
    }
    
    /**
     * 일정 관련 네비게이션 결과 키들
     */
    object Schedule {
        /** 일정 생성 완료 여부와 생성된 일정 ID */
        const val SCHEDULE_CREATED = "schedule_created"
        
        /** 일정 수정 완료 여부 */
        const val SCHEDULE_UPDATED = "schedule_updated"
        
        /** 일정 삭제 완료 여부 */
        const val SCHEDULE_DELETED = "schedule_deleted"
    }
    
    /**
     * 채팅 관련 네비게이션 결과 키들
     */
    object Chat {
        /** 메시지 전송 완료 여부 */
        const val MESSAGE_SENT = "chat_message_sent"
        
        /** 채널 생성 완료 여부 */
        const val CHANNEL_CREATED = "chat_channel_created"
        
        /** 채널 수정 완료 여부 */
        const val CHANNEL_UPDATED = "chat_channel_updated"
    }
} 