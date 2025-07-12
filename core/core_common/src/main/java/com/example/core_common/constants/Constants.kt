package com.example.core_common.constants

/**
 * 프로젝트 전반에서 사용되는 상수 값들을 정의합니다.
 */
object Constants {
    const val DB_NAME = "(default)"

    /**
     * 채널을 추가할 수 있는 최대 개수입니다. (소수점 두 자리로 표현되므로 00-99까지 100개)
     * 한 카테고리 내에서 이 개수를 초과하여 채널을 생성할 수 없습니다.
     */
    const val MAX_CHANNELS_PER_CATEGORY = 100

    const val REGION = "asia-northeast3"

    object Storage {
        const val USER_PROFILE_IMAGES = "user_profile_images"
    }

    /**
     * 네비게이션 관련 상수들
     */
    object Navigation {
        /**
         * 중복 클릭/네비게이션 방지를 위한 debounce 시간 (밀리초)
         * 버튼 클릭, 뒤로가기 등에서 연속 동작을 방지합니다.
         */
        const val DEBOUNCE_TIMEOUT_MS = 500L
        
        /**
         * 앱 종료를 위한 연속 뒤로가기 감지 시간 (밀리초)
         * 이 시간 내에 뒤로가기를 다시 누르면 앱이 종료됩니다.
         */
        const val EXIT_APP_TIMEOUT_MS = 2000L
    }
}
