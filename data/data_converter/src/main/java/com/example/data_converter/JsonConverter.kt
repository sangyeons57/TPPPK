package com.example.data_converter

import com.example.domain.model.AggregateRoot

/**
 * JSON과 Domain Model 간의 변환을 담당하는 인터페이스
 * Clean Architecture 원칙에 따라 Domain 계층은 JSON에 의존하지 않고,
 * Data 계층에서만 JSON 변환을 처리함
 */
interface JsonConverter<T> where T : AggregateRoot {

    /**
     * Domain Model을 JSON 문자열로 변환
     * @param data 변환할 Domain Model
     * @return JSON 문자열
     */
    fun toJson(data: T): String

    /**
     * JSON 문자열을 Domain Model로 변환
     * @param json JSON 문자열
     * @return Domain Model 객체
     * @throws JsonConversionException JSON 파싱 실패 시
     */
    fun fromJson(json: String): T

    /**
     * JSON 변환 가능 여부 확인
     * @param json 확인할 JSON 문자열
     * @return 변환 가능하면 true, 불가능하면 false
     */
    fun canConvert(json: String): Boolean = try {
        fromJson(json)
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * JSON 변환 실패 시 발생하는 예외
 */
class JsonConversionException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)