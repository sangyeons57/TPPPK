package com.example.data_core.util

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Room에서 Instant 타입을 Long으로 변환하기 위한 TypeConverter
 * UTC 시간을 Unix timestamp로 저장하여 효율적인 정렬과 비교를 지원
 */
class InstantConverter {

    /**
     * Instant를 Long(Unix timestamp)으로 변환
     * @param instant 변환할 Instant 객체
     * @return Unix timestamp (null이면 null 반환)
     */
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.epochSecond
    }

    /**
     * Long(Unix timestamp)을 Instant로 변환
     * @param timestamp Unix timestamp
     * @return Instant 객체 (null이면 null 반환)
     */
    @TypeConverter
    fun toInstant(timestamp: Long?): Instant? {
        return timestamp?.let { Instant.ofEpochSecond(it) }
    }
}