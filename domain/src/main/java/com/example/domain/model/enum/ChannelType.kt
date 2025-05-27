// 경로: domain/model/ChannelType.kt (ProjectSettingViewModel, EditChannelViewModel 등 기반)
package com.example.domain.model

// import com.example.core_common.constants.FirestoreConstants // 더 이상 직접 참조하지 않음

/**
 * 채널의 컨텍스트 타입을 나타내는 열거형 (도메인 모델)
 * 현재 DM과 PROJECT 두 가지 타입만 지원합니다.
 */
enum class ChannelType(val value: String) { 
    DM("DM"), // 직접 메시지 채널
    PROJECT("PROJECT"), // 프로젝트 채널
    UNKNOWN("UNKNOWN"); // 알 수 없는 타입

    companion object {
        /**
         * 문자열로부터 ChannelType을 파싱합니다.
         * 일치하는 타입이 없으면 UNKNOWN을 반환합니다.
         */
        fun fromString(typeString: String?): ChannelType {
            return entries.find { it.value.equals(typeString, ignoreCase = true) }
                ?: UNKNOWN // 일치하는 값 없으면 UNKNOWN 반환
        }
    }
}

// 필요시 다른 타입 추가 (예: 공지 채널, 스테이지 채널)