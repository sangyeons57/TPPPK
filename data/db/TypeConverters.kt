package com.example.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Room 데이터베이스에서 사용자 정의 타입을 변환하기 위한 TypeConverter 클래스.
 * LocalDateTime과 Long(epoch millis) 간의 변환,
 * List<String>과 JSON 문자열 간의 변환을 처리합니다.
 */
class TypeConverters {
    private val gson = Gson()

    /**
     * LocalDateTime 객체를 Long 타입 (UTC epoch milliseconds)으로 변환합니다.
     * Room이 데이터베이스에 날짜/시간을 저장할 때 사용합니다.
     *
     * @param dateTime 변환할 LocalDateTime 객체. null일 경우 null 반환.
     * @return Long 형태의 UTC epoch milliseconds. dateTime이 null이면 null.
     */
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): Long? {
        return dateTime?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
    }

    /**
     * Long 타입 (UTC epoch milliseconds)을 LocalDateTime 객체로 변환합니다.
     * Room이 데이터베이스에서 날짜/시간을 읽어올 때 사용합니다.
     *
     * @param value 변환할 Long 값 (UTC epoch milliseconds). null일 경우 null 반환.
     * @return 변환된 LocalDateTime 객체. value가 null이면 null.
     */
    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? {
        return value?.let {
            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDateTime()
        }
    }

    /**
     * List<String> 객체를 JSON 문자열로 변환합니다.
     * Room이 데이터베이스에 문자열 리스트를 저장할 때 사용합니다.
     *
     * @param list 변환할 List<String> 객체. null일 경우 null 반환.
     * @return JSON 문자열. list가 null이면 null.
     */
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }

    /**
     * JSON 문자열을 List<String> 객체로 변환합니다.
     * Room이 데이터베이스에서 문자열 리스트를 읽어올 때 사용합니다.
     *
     * @param value 변환할 JSON 문자열. null일 경우 null 반환.
     * @return 변환된 List<String> 객체. value가 null이면 null.
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, listType)
        }
    }
} 