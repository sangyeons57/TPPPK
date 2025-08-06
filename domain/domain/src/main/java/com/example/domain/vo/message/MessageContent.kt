package com.example.domain.vo.message

/**
 * 메시지 페이로드를 나타내는 값 객체 (기존 MessageContent 대체)
 *
 * JSON 형태의 페이로드 데이터를 저장하며, 메시지 타입에 따라 다른 구조를 가짐
 *
 * 예시:
 * - TEXT: {"content": "실제 메시지 내용"}
 * - SYSTEM_PROJECT_JOIN: {"projectId": "...", "projectName": "...", "actionText": "참여하기"}
 * - SYSTEM_DATE: {"date": "2024-01-01", "displayText": "2024년 1월 1일"}
 */
@JvmInline
value class MessagePayload(val value: String) {
    
    init {
        require(value.isNotEmpty()) { "MessagePayload cannot be empty." }
        // JSON 유효성 검사는 간단하게 처리 (도메인 레이어에서는 외부 라이브러리 의존성 최소화)
        require(value.startsWith("{") && value.endsWith("}")) { "MessagePayload must be JSON format." }
    }

    /**
     * TEXT 타입 메시지용 팩토리 메서드
     */
    companion object {
        /**
         * 일반 텍스트 메시지용 페이로드 생성
         */
        fun forText(content: String): MessagePayload {
            val json = """{"content": "${content.replace("\"", "\\\"")}"}"""
            return MessagePayload(json)
        }

        /**
         * 프로젝트 참여 시스템 메시지용 페이로드 생성
         */
        fun forProjectJoin(
            projectId: String,
            projectName: String,
            actionText: String = "참여하기"
        ): MessagePayload {
            val json = """{"projectId": "$projectId", "projectName": "${
                projectName.replace(
                    "\"",
                    "\\\""
                )
            }", "actionText": "$actionText"}"""
            return MessagePayload(json)
        }

        /**
         * 날짜 시스템 메시지용 페이로드 생성
         */
        fun forDateSystem(date: String, displayText: String): MessagePayload {
            val json =
                """{"date": "$date", "displayText": "${displayText.replace("\"", "\\\"")}"}"""
            return MessagePayload(json)
        }

        /**
         * 채팅 시작 시스템 메시지용 페이로드 생성
         */
        fun forChatStart(channelName: String, welcomeText: String = "채팅이 시작되었습니다"): MessagePayload {
            val json = """{"channelName": "${
                channelName.replace(
                    "\"",
                    "\\\""
                )
            }", "welcomeText": "$welcomeText"}"""
            return MessagePayload(json)
        }

        // 간단한 JSON 유효성 검사 (도메인 레이어에서는 외부 라이브러리 의존성 최소화)
        private fun isValidJsonFormat(jsonString: String): Boolean {
            return jsonString.startsWith("{") && jsonString.endsWith("}")
        }
    }

    /**
     * TEXT 타입 메시지의 content 추출 (간단한 문자열 파싱)
     */
    fun getTextContent(): String? {
        return try {
            // 간단한 JSON 파싱: {"content": "value"} 형태에서 content 값 추출
            val contentPattern = """"content"\s*:\s*"([^"]*)"""".toRegex()
            val matchResult = contentPattern.find(value)
            matchResult?.groupValues?.get(1)?.replace("\\\"", "\"")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * JSON 값 추출 (간단한 문자열 파싱)
     */
    fun getValue(key: String): String? {
        return try {
            val pattern = """"$key"\s*:\s*"([^"]*)"""".toRegex()
            val matchResult = pattern.find(value)
            matchResult?.groupValues?.get(1)?.replace("\\\"", "\"")
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 하위 호환성을 위한 MessageContent 타입 별칭
 * @deprecated MessagePayload를 사용하세요
 */
@Deprecated("Use MessagePayload instead", ReplaceWith("MessagePayload"))
typealias MessageContent = MessagePayload