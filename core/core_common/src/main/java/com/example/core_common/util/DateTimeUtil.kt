package com.example.core_common.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import com.google.firebase.Timestamp
import java.time.Duration
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Singleton

/**
 * 날짜 및 시간 변환을 위한 유틸리티 클래스
 * 프로젝트 전체에서 일관된 날짜/시간 처리를 위해 사용합니다.
 */
object DateTimeUtil {
    // 자주 사용되는 날짜/시간 포맷 패턴
    private val DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val TIME_PATTERN = DateTimeFormatter.ofPattern("HH:mm")
    private val DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val DATE_TIME_PATTERN2 = DateTimeFormatter.ofPattern("yyyy.MM.dd a h:mm")
    private val DATE_TIME_SECONDS_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val CHAT_TIME_PATTERN = DateTimeFormatter.ofPattern("a h:mm") // 오전/오후 표시
    private val SCHEDULE_DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)")

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
     * Date를 LocalTime으로 변환합니다.
     * @param date 변환할 Date 객체
     * @param zoneId 사용할 시간대 (기본값: 시스템 기본 시간대)
     * @return 변환된 LocalTime 객체
     */
    fun toLocalTime(date: Date, zoneId: ZoneId = ZoneId.systemDefault()): LocalTime {
        // API 레벨 호환성을 위해 LocalDateTime을 거쳐서 LocalTime을 얻음
        return toLocalDateTime(date, zoneId).toLocalTime()
    }

    /**
     * Firebase Timestamp를 LocalDateTime으로 변환합니다.
     * @param timestamp 변환할 Firebase Timestamp
     * @param zoneId 사용할 시간대 (기본값: 시스템 기본 시간대)
     * @return 변환된 LocalDateTime 객체
     */
    fun toLocalDateTime(timestamp: Timestamp, zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        return timestamp.toDate().let { toLocalDateTime(it, zoneId) }
    }

    /**
     * Firebase Timestamp를 LocalTime으로 변환합니다.
     * @param timestamp 변환할 Firebase Timestamp
     * @param zoneId 사용할 시간대 (기본값: 시스템 기본 시간대)
     * @return 변환된 LocalTime 객체
     */
    fun toLocalTime(timestamp: Timestamp, zoneId: ZoneId = ZoneId.systemDefault()): LocalTime {
        // API 레벨 호환성을 위해 LocalDateTime을 거쳐서 LocalTime을 얻음
        return toLocalDateTime(timestamp, zoneId).toLocalTime()
    }

    /**
     * LocalDateTime을 Firebase Timestamp로 변환합니다.
     * @param localDateTime 변환할 LocalDateTime
     * @return 변환된 Firebase Timestamp 객체
     */
    fun toTimestamp(localDateTime: LocalDateTime): Timestamp {
        return Timestamp(toDate(localDateTime))
    }

    /**
     * LocalTime을 Firebase Timestamp로 변환합니다.
     * (현재 날짜의 지정 시간으로 변환합니다)
     * @param localTime 변환할 LocalTime
     * @return 변환된 Firebase Timestamp 객체
     */
    fun toTimestamp(localTime: LocalTime): Timestamp {
        return localTime.let { 
            val today = LocalDateTime.now().toLocalDate()
            val dateTime = LocalDateTime.of(today, it)
            Timestamp(toDate(dateTime)) 
        }
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
     * 에포크 밀리초를 LocalTime으로 변환합니다.
     * @param epochMillis 에포크 시간 (밀리초)
     * @param zoneId 사용할 시간대 (기본값: 시스템 기본 시간대)
     * @return 변환된 LocalTime 객체
     */
    fun fromEpochMillisToLocalTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalTime {
        // API 레벨 호환성을 위해 LocalDateTime을 거쳐서 LocalTime을 얻음
        return fromEpochMillis(epochMillis, zoneId).toLocalTime()
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
     * LocalTime을 에포크 밀리초로 변환합니다.
     * (현재 날짜의 지정 시간으로 변환합니다)
     * @param localTime 변환할 LocalTime
     * @return 에포크 시간 (밀리초)
     */
    fun toEpochMillis(localTime: LocalTime): Long {
        val today = LocalDateTime.now().toLocalDate()
        val dateTime = LocalDateTime.of(today, localTime)
        return dateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    /**
     * Instant를 LocalDateTime으로 변환합니다.
     * 
     * @param instant 변환할 Instant
     * @param zoneId 사용할 시간대 (기본값: 시스템 기본 시간대)
     * @return 변환된 LocalDateTime 객체
     */
    fun toLocalDateTime(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        return LocalDateTime.ofInstant(instant, zoneId)
    }

    /**
     * Instant를 LocalTime으로 변환합니다.
     * 
     * @param instant 변환할 Instant
     * @param zoneId 사용할 시간대 (기본값: 시스템 기본 시간대)
     * @return 변환된 LocalTime 객체
     */
    fun toLocalTime(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): LocalTime {
        // API 레벨 호환성을 위해 LocalDateTime을 거쳐서 LocalTime을 얻음
        return toLocalDateTime(instant, zoneId).toLocalTime()
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
     * @return 포맷팅된 날짜 문자열
     */
    fun formatDate(dateTime: LocalDateTime): String {
        return dateTime.format(DATE_PATTERN)
    }

    /**
     * LocalDateTime을 시간 문자열로 포맷팅합니다.
     * 포맷: "HH:mm" (예: "14:30")
     * 
     * @param dateTime 변환할 LocalDateTime 객체
     * @return 포맷팅된 시간 문자열
     */
    fun formatTime(dateTime: LocalDateTime): String {
        return dateTime.format(TIME_PATTERN)
    }

    /**
     * LocalDateTime을 채팅 시간 형식으로 포맷팅합니다.
     * 포맷: "a h:mm" (예: "오전 9:30", "오후 2:45")
     * 
     * @param dateTime 변환할 LocalDateTime 객체
     * @return 포맷팅된 채팅 시간 문자열
     */
    fun formatChatTime(dateTime: LocalDateTime): String {
        return dateTime.format(CHAT_TIME_PATTERN)
    }

    fun formatScheduleDate(dateTime: LocalDateTime): String {
        return dateTime.format(SCHEDULE_DATE_PATTERN)
    }

    /**
     * LocalDateTime을 날짜 및 시간 문자열로 포맷팅합니다.
     * 포맷: "yyyy-MM-dd HH:mm" (예: "2023-07-15 14:30")
     * 
     * @param dateTime 변환할 LocalDateTime 객체
     * @return 포맷팅된 날짜 및 시간 문자열
     */
    fun formatDateTime(dateTime: LocalDateTime): String {
        return dateTime.format(DATE_TIME_PATTERN)
    }

    fun formatDateTime2(dateTime: LocalDateTime): String {
        return dateTime.format(DATE_TIME_PATTERN2)
    }

    /**
     * LocalDateTime을 초 단위까지 포함한 날짜 및 시간 문자열로 포맷팅합니다.
     * 포맷: "yyyy-MM-dd HH:mm:ss" (예: "2023-07-15 14:30:45")
     * 
     * @param dateTime 변환할 LocalDateTime 객체
     * @return 포맷팅된 날짜 및 시간 문자열
     */
    fun formatDateTimeWithSeconds(dateTime: LocalDateTime): String {
        return dateTime.format(DATE_TIME_SECONDS_PATTERN)
    }

    /**
     * 사용자 정의 패턴으로 LocalDateTime을 포맷팅합니다.
     * 
     * @param dateTime 변환할 LocalDateTime 객체
     * @param pattern DateTimeFormatter 패턴 문자열 (예: "yyyy년 MM월 dd일")
     * @return 포맷팅된 문자열
     */
    fun format(dateTime: LocalDateTime, pattern: String): String {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern))
    }

    /**
     * LocalDateTime을 Instant로 변환합니다.
     * 
     * @param localDateTime 변환할 LocalDateTime
     * @param zoneId 변환할 때 사용할 시간대 (기본값: UTC)
     * @return 변환된 Instant 객체
     */
    fun toInstant(localDateTime: LocalDateTime, zoneId: ZoneId = ZoneOffset.UTC): Instant {
        return localDateTime.atZone(zoneId).toInstant()
    }

    /**
     * Instant를 Date로 변환합니다.
     * 
     * @param instant 변환할 Instant
     * @return 변환된 Date 객체 또는 instant가 null인 경우 null
     */
    fun toDate(instant: Instant): Date {
        return instant.let { Date.from(it) }
    }
    
    /**
     * Date를 Instant로 변환합니다.
     * 
     * @param date 변환할 Date 객체
     * @return 변환된 Instant 객체 또는 date가 null인 경우 null
     */
    fun toInstant(date: Date): Instant {
        return date.toInstant()
    }

    /**
     * Instant를 Firebase Timestamp로 변환합니다.
     * 
     * @param instant 변환할 Instant
     * @return 변환된 Firebase Timestamp 객체 또는 instant가 null인 경우 null
     */
    fun instantToFirebaseTimestamp(instant: Instant): Timestamp {
        return Timestamp(instant.epochSecond, instant.nano)
    }

    /**
     * Firebase Timestamp를 Instant로 변환합니다.
     * 
     * @param timestamp 변환할 Firebase Timestamp
     * @return 변환된 Instant 객체 또는 timestamp가 null인 경우 null
     */
    fun firebaseTimestampToInstant(timestamp: Timestamp): Instant {
        return Instant.ofEpochSecond(timestamp.seconds, timestamp.nanoseconds.toLong())
    }

    /**
     * Epoch 밀리초를 Instant로 변환합니다.
     * 
     * @param epochMillis 에포크 시간 (밀리초)
     * @return 변환된 Instant 객체
     */
    fun fromEpochMillisToInstant(epochMillis: Long): Instant {
        return Instant.ofEpochMilli(epochMillis)
    }
    

    /**
     * LocalTime을 Instant로 변환합니다. (현재 날짜 사용)
     * 
     * @param localTime 변환할 LocalTime
     * @param zoneId 변환할 때 사용할 시간대 (기본값: UTC)
     * @return 변환된 Instant 객체
     */
    fun toInstant(localTime: LocalTime, zoneId: ZoneId = ZoneOffset.UTC): Instant {
        return localTime.let { 
            val today = LocalDateTime.now().toLocalDate()
            val dateTime = LocalDateTime.of(today, it)
            dateTime.atZone(zoneId).toInstant()
        }
    }

    /**
     * LocalDateTime을 LocalTime으로 변환합니다.
     * 
     * @param localDateTime 변환할 LocalDateTime
     * @return 변환된 LocalTime 객체
     */
    fun toLocalTime(localDateTime: LocalDateTime): LocalTime {
        return localDateTime.toLocalTime()
    }

    /**
     * Instant에서 하루 중 경과한 초를 계산합니다.
     * 
     * @param instant 변환할 Instant
     * @param zoneId 사용할 시간대 (기본값: 시스템 기본 시간대)
     * @return 하루 중 경과한 초 (0 ~ 86399)
     */
    fun getSecondOfDayFromInstant(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): Int {
        return toLocalTime(instant, zoneId).toSecondOfDay()
    }

    /**
     * 현재 시간을 Instant로 가져옵니다.
     *
     * @return 현재 시간의 Instant 객체
     */
    fun nowInstant(): Instant {
        return Instant.now()
    }

    /**
     * 현재 시간을 firestore timestamp로 가져옵니다.
     * 
     * @return 현재 시간의 firestore timestamp 객체
     */
    fun nowFirebaseTimestamp(): Timestamp {
        return Timestamp.now()
    }

    /**
     * Instant를 에포크 밀리초로 변환합니다.
     * 
     * @param instant 변환할 Instant
     * @return 에포크 시간 (밀리초)
     */
    fun toEpochMillis(instant: Instant): Long {
        return instant.toEpochMilli()
    }

    /**
     * Instant를 날짜 문자열로 포맷팅합니다.
     * 포맷: "yyyy-MM-dd"
     * 
     * @param instant 변환할 Instant
     * @param zoneId 시간대 (기본값: 시스템 기본)
     * @return 포맷팅된 날짜 문자열
     */
    fun formatDate(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return formatDate(toLocalDateTime(instant, zoneId))
    }

    /**
     * Instant를 시간 문자열로 포맷팅합니다.
     * 포맷: "HH:mm"
     * 
     * @param instant 변환할 Instant
     * @param zoneId 시간대 (기본값: 시스템 기본)
     * @return 포맷팅된 시간 문자열
     */
    fun formatTime(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return formatTime(toLocalDateTime(instant, zoneId))
    }

    /**
     * Instant를 채팅 시간 형식으로 포맷팅합니다.
     * 포맷: "a h:mm"
     * 
     * @param instant 변환할 Instant
     * @param zoneId 시간대 (기본값: 시스템 기본)
     * @return 포맷팅된 채팅 시간 문자열
     */
    fun formatChatTime(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return formatChatTime(toLocalDateTime(instant, zoneId))
    }

    /**
     * Instant를 날짜 및 시간 문자열로 포맷팅합니다.
     * 포맷: "yyyy-MM-dd HH:mm"
     * 
     * @param instant 변환할 Instant
     * @param zoneId 시간대 (기본값: 시스템 기본)
     * @return 포맷팅된 날짜/시간 문자열
     */
    fun formatDateTime(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return formatDateTime(toLocalDateTime(instant, zoneId))
    }

    /**
     * Instant를 초 단위까지 포함한 날짜 및 시간 문자열로 포맷팅합니다.
     * 포맷: "yyyy-MM-dd HH:mm:ss"
     * 
     * @param instant 변환할 Instant
     * @param zoneId 시간대 (기본값: 시스템 기본)
     * @return 포맷팅된 날짜/시간 문자열
     */
    fun formatDateTimeWithSeconds(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return formatDateTimeWithSeconds(toLocalDateTime(instant, zoneId))
    }

    /**
     * Instant를 사용자 정의 패턴으로 포맷팅합니다.
     * 
     * @param instant 변환할 Instant
     * @param pattern DateTimeFormatter 패턴 문자열
     * @param zoneId 시간대 (기본값: 시스템 기본)
     * @return 포맷팅된 문자열
     */
    fun format(instant: Instant, pattern: String, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return format(toLocalDateTime(instant, zoneId), pattern)
    }

    // LocalTime 관련 추가 유틸리티 (예: LocalTime을 초로 변환)
    /**
     * LocalTime을 자정 기준 초로 변환합니다.
     * @param localTime 변환할 LocalTime
     * @return 초 단위 값
     */
    fun toSecondOfDay(localTime: LocalTime): Int {
        return localTime.toSecondOfDay()
    }

    /**
     * 자정 기준 초를 LocalTime으로 변환합니다.
     * @param secondOfDay 초 단위 값
     * @return 변환된 LocalTime 객체
     */
    fun fromSecondOfDay(secondOfDay: Int): LocalTime {
        return LocalTime.ofSecondOfDay(secondOfDay.toLong())
    }

    /**
     * Instant를 자정 기준 초로 변환합니다.
     * @param instant 변환할 Instant
     * @param zoneId 시간대 (기본값: 시스템 기본)
     * @return 초 단위 값
     */
    fun toSecondOfDay(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): Int {
        return toLocalTime(instant, zoneId).toSecondOfDay()
    }

    // --- Methods for converting UI input types (String/LocalDateTime/LocalDate/LocalTime) back to Instant ---

    /**
     * yyyy-MM-dd 형식의 날짜 문자열과 HH:mm 형식의 시간 문자열을 Instant로 변환합니다.
     * @param dateString "yyyy-MM-dd"
     * @param timeString "HH:mm"
     * @param zoneId 해당 로컬 날짜/시간의 시간대 (기본값: 시스템 기본값)
     * @return 변환된 Instant
     */
    fun parseDateTimeStringsToInstant(dateString: String, timeString: String, zoneId: ZoneId = ZoneId.systemDefault()): Instant {
        return try {
            val localDate = LocalDate.parse(dateString, DATE_PATTERN)
            val localTime = LocalTime.parse(timeString, TIME_PATTERN)
            LocalDateTime.of(localDate, localTime).atZone(zoneId).toInstant()
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * LocalDate를 해당 날짜의 시작 시간(00:00:00) Instant로 변환합니다.
     * @param localDate 변환할 LocalDate
     * @param zoneId 해당 로컬 날짜의 시간대 (기본값: 시스템 기본값)
     * @return 변환된 Instant
     */
    fun localDateToStartOfDayInstant(localDate: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Instant {
        return localDate.atStartOfDay(zoneId).toInstant()
    }

    /**
     * LocalDate를 해당 날짜의 종료 시간(23:59:59.999999999) Instant로 변환합니다.
     * @param localDate 변환할 LocalDate
     * @param zoneId 해당 로컬 날짜의 시간대 (기본값: 시스템 기본값)
     * @return 변환된 Instant
     */
    fun localDateToEndOfDayInstant(localDate: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Instant {
        return localDate.atTime(LocalTime.MAX).atZone(zoneId).toInstant()
    }

    //endregion

    //region Epoch Milliseconds <-> Instant Conversion

    /**
     * Epoch milliseconds (Long)를 Instant로 변환합니다.
     * Unix timestamp (long) 값을 Instant 객체로 변환할 때 사용합니다.
     *
     * @param epochMilli 변환할 epoch milliseconds 값
     * @return 변환된 Instant 객체.
     */
    fun epochMilliToInstant(epochMilli: Long): Instant {
        return Instant.ofEpochMilli(epochMilli)
    }

    /**
     * Instant를 Epoch milliseconds (Long)로 변환합니다.
     * Instant 객체를 Unix timestamp (long) 값으로 변환할 때 사용합니다.
     *
     * @param instant 변환할 Instant 객체
     * @return 변환된 epoch milliseconds 값.
     */
    fun instantToEpochMilli(instant: Instant): Long {
        return instant.toEpochMilli()
    }

    //endregion

    /**
     * 현재 시간을 기준으로 "방금 전", "n분 전", "n시간 전", "어제", "날짜" 형식으로 표시합니다.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun formatRelativeTime(dateTime: LocalDateTime): String {
        val now = LocalDateTime.now()
        val diff = Duration.between(dateTime, now)

        if (diff.toDays() > 0) {
            return formatDate(dateTime)
        } else if (diff.toHours() > 0) {
            return "${diff.toHours()}시간 전"
        } else if (diff.toMinutes() > 0) {
            return "${diff.toMinutes()}분 전"
        } else if (diff.toSeconds() > 0) {
            return "${diff.toSeconds()}초 전"
        } else {
            return "방금 전"
        }
    }

    /**
     * LocalDate와 LocalTime을 결합하여 Instant로 변환합니다.
     *
     * @param localDate 변환할 LocalDate
     * @param localTime 변환할 LocalTime
     * @param zoneId 변환할 때 사용할 시간대 (기본값: 시스템 기본 시간대)
     * @return 결합되고 변환된 Instant 객체
     */
    fun localDateAndTimeToInstant(localDate: LocalDate, localTime: LocalTime, zoneId: ZoneId = ZoneId.systemDefault()): Instant {
        return LocalDateTime.of(localDate, localTime).atZone(zoneId).toInstant()
    }

    /**
     * Firebase Timestamp를 LocalDateTime으로 변환합니다. (Instant를 통해 변환)
     * @param timestamp 변환할 Firebase Timestamp
     * @param zoneId 결과 LocalDateTime에 적용할 시간대 (기본값: 시스템 기본 시간대)
     * @return 변환된 LocalDateTime 객체
     */
    fun firebaseTimestampToLocalDateTime(timestamp: Timestamp, zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        return firebaseTimestampToInstant(timestamp).let { instant ->
            LocalDateTime.ofInstant(instant, zoneId)
        }
    }

    /**
     * YearMonth를 해당 월의 시작 시점 Instant로 변환합니다.
     * 예: 2023년 5월 -> 2023-05-01T00:00:00Z (UTC 기준)
     *
     * @param yearMonth 변환할 YearMonth 객체
     * @param zoneId 기준 시간대 (기본값: UTC)
     * @return 해당 월의 1일 00:00:00 시점의 Instant
     */
    fun yearMonthToStartOfMonthInstant(yearMonth: YearMonth, zoneId: ZoneId = ZoneOffset.UTC): Instant {
        return yearMonth.atDay(1).atStartOfDay(zoneId).toInstant()
    }

    /**
     * YearMonth를 다음 달의 시작 시점 Instant로 변환합니다.
     * 이는 특정 월의 스케줄을 조회할 때 종료 시점(exclusive, 해당 시점 미포함)으로 유용하게 사용될 수 있습니다.
     * 예: 2023년 5월 -> 2023-06-01T00:00:00Z (UTC 기준)
     *
     * @param yearMonth 변환할 YearMonth 객체
     * @param zoneId 기준 시간대 (기본값: UTC)
     * @return 다음 달의 1일 00:00:00 시점의 Instant
     */
    fun yearMonthToEndOfMonthExclusiveInstant(yearMonth: YearMonth, zoneId: ZoneId = ZoneOffset.UTC): Instant {
        return yearMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant()
    }

    /**
     * YearMonth를 해당 월의 시작 시점 Firebase Timestamp로 변환합니다.
     * 예: 2023년 5월 -> 2023-05-01T00:00:00Z (UTC 기준)에 해당하는 Timestamp
     *
     * @param yearMonth 변환할 YearMonth 객체
     * @param zoneId 기준 시간대 (기본값: UTC)
     * @return 해당 월의 1일 00:00:00 시점의 Firebase Timestamp
     */
    fun yearMonthToStartOfMonthTimestamp(yearMonth: YearMonth, zoneId: ZoneId = ZoneOffset.UTC): Timestamp {
        val instant = yearMonthToStartOfMonthInstant(yearMonth, zoneId)
        // 기존 instantToTimestamp 함수가 null을 반환할 수 있지만, 여기서는 yearMonthToStartOfMonthInstant가 항상 non-null Instant를 반환하므로
        // instantToTimestamp(instant)!! 와 같이 단언하거나, 아래처럼 직접 변환합니다.
        return Timestamp(instant.epochSecond, instant.nano)
    }

    /**
     * YearMonth를 다음 달의 시작 시점 Firebase Timestamp로 변환합니다.
     * 이는 특정 월의 스케줄을 조회할 때 종료 시점(exclusive, 해당 시점 미포함)으로 유용하게 사용될 수 있습니다.
     * 예: 2023년 5월 -> 2023-06-01T00:00:00Z (UTC 기준)에 해당하는 Timestamp
     *
     * @param yearMonth 변환할 YearMonth 객체
     * @param zoneId 기준 시간대 (기본값: UTC)
     * @return 다음 달의 1일 00:00:00 시점의 Firebase Timestamp
     */
    fun yearMonthToEndOfMonthExclusiveTimestamp(yearMonth: YearMonth, zoneId: ZoneId = ZoneOffset.UTC): Timestamp {
        val instant = yearMonthToEndOfMonthExclusiveInstant(yearMonth, zoneId)
        // yearMonthToEndOfMonthExclusiveInstant가 항상 non-null Instant를 반환하므로
        // instantToTimestamp(instant)!! 와 같이 단언하거나, 아래처럼 직접 변환합니다.
        return Timestamp(instant.epochSecond, instant.nano)
    }
}