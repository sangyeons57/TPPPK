package com.example.feature_chat.util

import com.example.domain.model.vo.message.MentionInfo
import com.example.domain.model.vo.MentionType
import java.util.regex.Pattern

/**
 * 메시지 텍스트에서 멘션을 파싱하는 유틸리티 클래스
 * @username 패턴을 감지하여 MentionInfo 리스트로 변환
 */
object MentionParser {

    // @username 패턴을 매칭하는 정규식 (알파벳, 숫자, 언더스코어, 한글 지원)
    private val MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_가-힣]+)")

    /**
     * 메시지 텍스트에서 멘션을 파싱
     * @param text 파싱할 메시지 텍스트
     * @param getUserIdByUsername username으로 실제 userId를 찾는 함수
     * @return 파싱된 MentionInfo 리스트
     */
    suspend fun parseMentions(
        text: String,
        getUserIdByUsername: suspend (String) -> String?
    ): List<MentionInfo> {
        val mentions = mutableListOf<MentionInfo>()
        val matcher = MENTION_PATTERN.matcher(text)

        while (matcher.find()) {
            val username = matcher.group(1) ?: continue
            val startIndex = matcher.start()
            val endIndex = matcher.end()

            // username으로 실제 userId 조회
            val userId = getUserIdByUsername(username)
            if (userId != null) {
                val mentionInfo = MentionInfo.create(
                    type = MentionType.USER,
                    id = userId,
                    displayName = "@$username"
                )
                mentions.add(mentionInfo)
            }
        }

        return mentions
    }

    /**
     * 메시지 텍스트에서 @everyone, @here 등의 특수 멘션 파싱
     * @param text 파싱할 메시지 텍스트
     * @return 파싱된 특수 MentionInfo 리스트
     */
    fun parseSpecialMentions(text: String): List<MentionInfo> {
        val mentions = mutableListOf<MentionInfo>()

        // @everyone 패턴 찾기
        val everyonePattern = Pattern.compile("@everyone")
        val everyoneMatcher = everyonePattern.matcher(text)
        while (everyoneMatcher.find()) {
            val mentionInfo = MentionInfo.create(
                type = MentionType.EVERYONE,
                id = "everyone", // 특수 ID
                displayName = "@everyone"
            )
            mentions.add(mentionInfo)
        }

        // @here 패턴 찾기
        val herePattern = Pattern.compile("@here")
        val hereMatcher = herePattern.matcher(text)
        while (hereMatcher.find()) {
            val mentionInfo = MentionInfo.create(
                type = MentionType.HERE,
                id = "here", // 특수 ID
                displayName = "@here"
            )
            mentions.add(mentionInfo)
        }

        return mentions
    }

    /**
     * 텍스트와 멘션 정보를 결합하여 전체 멘션 리스트 생성
     * @param text 파싱할 메시지 텍스트
     * @param getUserIdByUsername username으로 실제 userId를 찾는 함수
     * @return 모든 멘션 정보가 포함된 리스트
     */
    suspend fun parseAllMentions(
        text: String,
        getUserIdByUsername: suspend (String) -> String?
    ): List<MentionInfo> {
        val userMentions = parseMentions(text, getUserIdByUsername)
        val specialMentions = parseSpecialMentions(text)

        return (userMentions + specialMentions)
    }

    /**
     * 멘션이 포함된 텍스트에서 순수 텍스트 추출 (멘션 제거)
     * @param text 원본 텍스트
     * @return 멘션이 제거된 텍스트
     */
    fun extractPlainText(text: String): String {
        return text.replace(MENTION_PATTERN.toRegex(), "")
            .replace("@everyone", "")
            .replace("@here", "")
            .trim()
            .replace(Regex("\\s+"), " ") // 연속된 공백을 하나로 축약
    }

    /**
     * 텍스트에 멘션이 포함되어 있는지 확인
     * @param text 확인할 텍스트
     * @return 멘션 포함 여부
     */
    fun hasMentions(text: String): Boolean {
        return MENTION_PATTERN.matcher(text).find() ||
                text.contains("@everyone") ||
                text.contains("@here")
    }

    /**
     * 특정 사용자가 멘션되었는지 확인
     * @param mentions 멘션 리스트
     * @param userId 확인할 사용자 ID
     * @return 멘션 여부
     */
    fun isUserMentioned(mentions: List<MentionInfo>, userId: String): Boolean {
        return mentions.any { mention ->
            when (mention.type) {
                MentionType.USER -> mention.id == userId
                MentionType.EVERYONE, MentionType.HERE -> true
                else -> false
            }
        }
    }
}