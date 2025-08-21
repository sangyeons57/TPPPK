package com.example.feature_chat.util

import com.example.domain.vo.MentionType
import com.example.domain.vo.message.MentionInfo
import java.util.regex.Pattern

/**
 * 단순화된 멘션 파싱 유틸리티 클래스
 * 복잡한 인코딩/디코딩 없이 간단한 @username 패턴 처리
 */
object MentionParser {

    // @username 패턴을 매칭하는 정규식 (알파벳, 숫자, 언더스코어, 한글 지원)
    private val MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_가-힣]+)")

    /**
     * 단순화된 멘션 파싱 - 텍스트에서 @username 패턴만 감지
     * @param text 파싱할 메시지 텍스트
     * @param mentionMappings 미리 정의된 멘션 매핑 (displayName -> MentionInfo)
     * @return 파싱된 MentionInfo 리스트
     */
    fun parseMentionsSimple(
        text: String,
        mentionMappings: Map<String, MentionInfo> = emptyMap()
    ): List<MentionInfo> {
        val mentions = mutableListOf<MentionInfo>()
        val matcher = MENTION_PATTERN.matcher(text)

        while (matcher.find()) {
            val fullMatch = matcher.group() ?: continue // @username 전체
            val username = matcher.group(1) ?: continue // username 부분만

            // 미리 정의된 매핑에서 찾기
            val mentionInfo = mentionMappings[fullMatch] ?: MentionInfo.create(
                type = MentionType.USER,
                id = username, // 임시로 username을 id로 사용
                displayName = fullMatch
            )
            mentions.add(mentionInfo)
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

        // @here 제거됨

        return mentions
    }

    /**
     * 모든 멘션 파싱 (일반 사용자 + 특수 멘션)
     * @param text 파싱할 메시지 텍스트
     * @param mentionMappings 미리 정의된 멘션 매핑
     * @return 모든 멘션 정보가 포함된 리스트
     */
    fun parseAllMentionsSimple(
        text: String,
        mentionMappings: Map<String, MentionInfo> = emptyMap()
    ): List<MentionInfo> {
        val userMentions = parseMentionsSimple(text, mentionMappings)
        val specialMentions = parseSpecialMentions(text)

        return (userMentions + specialMentions).distinctBy { it.displayName }
    }

    /**
     * 멘션이 포함된 텍스트에서 순수 텍스트 추출 (멘션 제거)
     * @param text 원본 텍스트
     * @return 멘션이 제거된 텍스트
     */
    fun extractPlainText(text: String): String {
        return text.replace(MENTION_PATTERN.toRegex(), "")
            .replace("@everyone", "")
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
                text.contains("@everyone")
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
                MentionType.EVERYONE -> true
                else -> false
            }
        }
    }
}
