package com.example.core_common.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import com.google.firebase.Timestamp

/**
 * 날짜 및 시간 변환을 위한 유틸리티 클래스
 * 프로젝트 전체에서 일관된 날짜/시간 처리를 위해 사용합니다.
 */
object DateTimeUtil {
    // 자주 사용되는 날짜/시간 포맷 패턴
    private val DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val TIME_PATTERN = DateTimeFormatter.ofPattern("HH:mm")
    private val DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val DATE_TIME_SECONDS_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val CHAT_TIME_PATTERN = DateTimeFormatter.ofPattern("a h:mm") // 오전/오후 표시

    /**
     * LocalDateTime을 Date로 변환합니다.
     * @param localDateTime 변환할 LocalDateTime
     * @return 변환된 Date 객체
     */
    fun toDate(localDateTime: LocalDateTime): Date {
        return Date.from(localDateTime.atZone(ZoneOffset.UTC).toInstant())
    }

    /**
     * Date를 LocalDateTime으로 변환합니다.
     * @param date 변환할 Date 객체
     * @param zoneId 사용할 시간대 (기본값: 시스템 기본 시간대)
     * @return 변환된 LocalDateTime 객체
     */
    fun toLocalDateTime(date: Date, zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        return LocalDateTime.ofInstant(date.toInstant(), zoneId)
    }

    /**
     * Firebase Timestamp를 LocalDateTime으로 변환합니다.
     * @param timestamp 변환할 Firebase Timestamp
     * @param zoneId 사용할 시간대 (기본값: 시스템 기본 시간대)
     * @return 변환된 LocalDateTime 객체 또는 timestamp가 null인 경우 null
     */
    fun toLocalDateTime(timestamp: Timestamp?, zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime? {
        return timestamp?.toDate()?.let { toLocalDateTime(it, zoneId) }
    }

    /**
     * LocalDateTime을 Firebase Timestamp로 변환합니다.
     * @param localDateTime 변환할 LocalDateTime
     * @return 변환된 Firebase Timestamp 객체 또는 localDateTime이 null인 경우 null
     */
    fun toTimestamp(localDateTime: LocalDateTime?): Timestamp? {
        return localDateTime?.let { Timestamp(toDate(it)) }
    }

    /**
     * 에포크 밀리초를 LocalDateTime으로 변환합니다.
     * @param epochMillis 에포크 시간 (밀리초)
     * @param zoneId 사용할 시간대 (기본값: 시스템 기본 시간대)
     * @return 변환된 LocalDateTime 객체
     */
    fun fromEpochMillis(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zoneId)
    }

    /**
     * LocalDateTime을 에포크 밀리초로 변환합니다.
     * @param localDateTime 변환할 LocalDateTime
     * @return 에포크 시간 (밀리초)
     */
    fun toEpochMillis(localDateTime: LocalDateTime): Long {
        return localDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    /**
     * 현재 시간을 LocalDateTime으로 가져옵니다.
     * @param zoneId 사용할 시간대 (기본값: 시스템 기본 시간대)
     * @return 현재 시간의 LocalDateTime 객체
     */
    fun now(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        return LocalDateTime.now(zoneId)
    }

    /**
     * UTC 기준 현재 시간을 LocalDateTime으로 가져옵니다.
     * @return UTC 기준 현재 시간의 LocalDateTime 객체
     */
    fun nowUtc(): LocalDateTime {
        return LocalDateTime.now(ZoneOffset.UTC)
    }

    /**
     * LocalDateTime을 날짜 문자열로 포맷팅합니다.
     * 포맷: "yyyy-MM-dd" (예: "2023-07-15")
     * 
     * @param dateTime 변환할 LocalDateTime 객체
     * @return 포맷팅된 날짜 문자열 또는 dateTime이 null인 경우 빈 문자열
     */
    fun formatDate(dateTime: LocalDateTime?): String {
        return dateTime?.format(DATE_PATTERN) ?: ""
    }

    /**
     * LocalDateTime을 시간 문자열로 포맷팅합니다.
     * 포맷: "HH:mm" (예: "14:30")
     * 
     * @param dateTime 변환할 LocalDateTime 객체
     * @return 포맷팅된 시간 문자열 또는 dateTime이 null인 경우 빈 문자열
     */
    fun formatTime(dateTime: LocalDateTime?): String {
        return dateTime?.format(TIME_PATTERN) ?: ""
    }

    /**
     * LocalDateTime을 채팅 시간 형식으로 포맷팅합니다.
     * 포맷: "a h:mm" (예: "오전 9:30", "오후 2:45")
     * 
     * @param dateTime 변환할 LocalDateTime 객체
     * @return 포맷팅된 채팅 시간 문자열 또는 dateTime이 null인 경우 빈 문자열
     */
    fun formatChatTime(dateTime: LocalDateTime?): String {
        return dateTime?.format(CHAT_TIME_PATTERN) ?: ""
    }

    /**
     * LocalDateTime을 날짜 및 시간 문자열로 포맷팅합니다.
     * 포맷: "yyyy-MM-dd HH:mm" (예: "2023-07-15 14:30")
     * 
     * @param dateTime 변환할 LocalDateTime 객체
     * @return 포맷팅된 날짜 및 시간 문자열 또는 dateTime이 null인 경우 빈 문자열
     */
    fun formatDateTime(dateTime: LocalDateTime?): String {
        return dateTime?.format(DATE_TIME_PATTERN) ?: ""
    }

    /**
     * LocalDateTime을 초 단위까지 포함한 날짜 및 시간 문자열로 포맷팅합니다.
     * 포맷: "yyyy-MM-dd HH:mm:ss" (예: "2023-07-15 14:30:45")
     * 
     * @param dateTime 변환할 LocalDateTime 객체
     * @return 포맷팅된 날짜 및 시간 문자열 또는 dateTime이 null인 경우 빈 문자열
     */
    fun formatDateTimeWithSeconds(dateTime: LocalDateTime?): String {
        return dateTime?.format(DATE_TIME_SECONDS_PATTERN) ?: ""
    }

    /**
     * 사용자 정의 패턴으로 LocalDateTime을 포맷팅합니다.
     * 
     * @param dateTime 변환할 LocalDateTime 객체
     * @param pattern DateTimeFormatter 패턴 문자열 (예: "yyyy년 MM월 dd일")
     * @return 포맷팅된 문자열 또는 dateTime이 null인 경우 빈 문자열
     */
    fun format(dateTime: LocalDateTime?, pattern: String): String {
        return dateTime?.format(DateTimeFormatter.ofPattern(pattern)) ?: ""
    }
} 