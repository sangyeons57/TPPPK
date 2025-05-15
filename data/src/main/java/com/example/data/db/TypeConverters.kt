package com.example.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Room 데이터베이스에서 사용자 정의 타입을 변환하기 위한 TypeConverter 클래스입니다.
 * LocalDateTime <-> Long (Timestamp)
 * Instant <-> Long (Timestamp)
 * List<String> <-> String (JSON)
 */
class AppTypeConverters {

    /**
     * Long(타임스탬프) 값을 LocalDateTime 객체로 변환합니다.
     * 저장된 타임스탬프는 UTC 기준이라고 가정합니다.
     *
     * @param value 데이터베이스에 저장된 Long 타임스탬프. null일 수 있습니다.
     * @return 변환된 LocalDateTime 객체. value가 null이면 null을 반환합니다.
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDateTime()
        }
    }

    /**
     * LocalDateTime 객체를 Long(타임스탬프) 값으로 변환합니다.
     * LocalDateTime은 UTC 기준으로 변환되어 저장됩니다.
     *
     * @param dateTime 변환할 LocalDateTime 객체. null일 수 있습니다.
     * @return 변환된 Long 타임스탬프. dateTime이 null이면 null을 반환합니다.
     */
    @TypeConverter
    fun dateToTimestamp(dateTime: LocalDateTime?): Long? {
        return dateTime?.atZone(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli()
    }

    /**
     * Long(타임스탬프) 값을 Instant 객체로 변환합니다.
     *
     * @param value 데이터베이스에 저장된 Long 타임스탬프. null일 수 있습니다.
     * @return 변환된 Instant 객체. value가 null이면 null을 반환합니다.
     */
    @TypeConverter
    fun longToInstant(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    /**
     * Instant 객체를 Long(타임스탬프) 값으로 변환합니다.
     *
     * @param instant 변환할 Instant 객체. null일 수 있습니다.
     * @return 변환된 Long 타임스탬프. instant가 null이면 null을 반환합니다.
     */
    @TypeConverter
    fun instantToLong(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    /**
     * JSON 문자열을 List<String> 객체로 변환합니다.
     *
     * @param value 데이터베이스에 저장된 JSON 문자열. null일 수 있습니다.
     * @return 변환된 List<String> 객체. value가 null이거나 비어있으면 빈 리스트를 반환합니다.
     */
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value == null) {
            return emptyList()
        }
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }

    /**
     * List<String> 객체를 JSON 문자열로 변환합니다.
     *
     * @param list 변환할 List<String> 객체. null일 수 있습니다.
     * @return 변환된 JSON 문자열. list가 null이면 null을 반환합니다.
     */
    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return list?.let { Gson().toJson(it) }
    }
} 