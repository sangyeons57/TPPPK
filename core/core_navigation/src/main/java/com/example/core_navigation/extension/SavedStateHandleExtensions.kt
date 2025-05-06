package com.example.core_navigation.extension

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SavedStateHandle에 대한 확장 함수들
 * 뷰모델에서 인자를 더 쉽게 처리할 수 있도록 합니다.
 */

/**
 * String 값을 가져오는 확장 함수
 * 
 * @param key 키 값
 * @return String 값 또는 null
 */
fun SavedStateHandle.getString(key: String): String? {
    return get<String>(key)
}

/**
 * null이 아닌 String 값을 반드시 가져오는 확장 함수
 * 값이 없으면 예외를 발생시킵니다.
 * 
 * @param key 키 값
 * @return String 값 (null이 아님)
 * @throws IllegalArgumentException 키에 해당하는 값이 없거나 null인 경우
 */
fun SavedStateHandle.getRequiredString(key: String): String {
    return getString(key) ?: throw IllegalArgumentException("Required parameter '$key' is missing")
}

fun SavedStateHandle.getOptionalString(key: String): String? {
    return getString(key)
}

/**
 * String 값이 없으면 기본값을 반환하는 확장 함수
 * 
 * @param key 키 값
 * @param defaultValue 기본값
 * @return String 값 또는 기본값
 */
fun SavedStateHandle.getStringOrDefault(key: String, defaultValue: String): String {
    return getString(key) ?: defaultValue
}

/**
 * Int 값을 가져오는 확장 함수
 * 
 * @param key 키 값
 * @return Int 값 또는 null
 */
fun SavedStateHandle.getInt(key: String): Int? {
    return get<Int>(key)
}

/**
 * null이 아닌 Int 값을 반드시 가져오는 확장 함수
 * 값이 없으면 예외를 발생시킵니다.
 * 
 * @param key 키 값
 * @return Int 값 (null이 아님)
 * @throws IllegalArgumentException 키에 해당하는 값이 없거나 null인 경우
 */
fun SavedStateHandle.getRequiredInt(key: String): Int {
    return getInt(key) ?: throw IllegalArgumentException("Required parameter '$key' is missing")
}

/**
 * Int 값이 없으면 기본값을 반환하는 확장 함수
 * 
 * @param key 키 값
 * @param defaultValue 기본값
 * @return Int 값 또는 기본값
 */
fun SavedStateHandle.getIntOrDefault(key: String, defaultValue: Int = 0): Int {
    return getInt(key) ?: defaultValue
}

/**
 * Boolean 값을 가져오는 확장 함수
 * 
 * @param key 키 값
 * @return Boolean 값 또는 null
 */
fun SavedStateHandle.getBoolean(key: String): Boolean? {
    return get<Boolean>(key)
}

/**
 * Boolean 값이 없으면 기본값을 반환하는 확장 함수
 * 
 * @param key 키 값
 * @param defaultValue 기본값
 * @return Boolean 값 또는 기본값
 */
fun SavedStateHandle.getBooleanOrDefault(key: String, defaultValue: Boolean = false): Boolean {
    return getBoolean(key) ?: defaultValue
}

/**
 * Long 값을 가져오는 확장 함수
 * 
 * @param key 키 값
 * @return Long 값 또는 null
 */
fun SavedStateHandle.getLong(key: String): Long? {
    return get<Long>(key)
}

/**
 * null이 아닌 Long 값을 반드시 가져오는 확장 함수
 * 값이 없으면 예외를 발생시킵니다.
 * 
 * @param key 키 값
 * @return Long 값 (null이 아님)
 * @throws IllegalArgumentException 키에 해당하는 값이 없거나 null인 경우
 */
fun SavedStateHandle.getRequiredLong(key: String): Long {
    return getLong(key) ?: throw IllegalArgumentException("Required parameter '$key' is missing")
}

/**
 * Long 값이 없으면 기본값을 반환하는 확장 함수
 * 
 * @param key 키 값
 * @param defaultValue 기본값
 * @return Long 값 또는 기본값
 */
fun SavedStateHandle.getLongOrDefault(key: String, defaultValue: Long = 0L): Long {
    return getLong(key) ?: defaultValue
}

/**
 * 키에 해당하는 값의 Flow를 반환하는 확장 함수
 * 
 * @param key 키 값
 * @return 값의 Flow
 */
fun <T> SavedStateHandle.getFlow(key: String): Flow<T?> {
    return getStateFlow<T?>(key, null)
}

/**
 * 키에 해당하는 String 값의 Flow를 반환하는 확장 함수
 * 
 * @param key 키 값
 * @return String 값의 Flow
 */
fun SavedStateHandle.getStringFlow(key: String): Flow<String?> {
    return getStateFlow<String?>(key, null)
}

/**
 * 키에 해당하는 String 값의 Flow를 반환하는 확장 함수
 * null인 경우 기본값 사용
 * 
 * @param key 키 값
 * @param defaultValue 기본값
 * @return String 값의 Flow (null이 아님)
 */
fun SavedStateHandle.getStringFlowOrDefault(key: String, defaultValue: String): Flow<String> {
    return getStringFlow(key).map { it ?: defaultValue }
}

/**
 * 키에 해당하는 Int 값의 Flow를 반환하는 확장 함수
 * 
 * @param key 키 값
 * @return Int 값의 Flow
 */
fun SavedStateHandle.getIntFlow(key: String): Flow<Int?> {
    return getStateFlow<Int?>(key, null)
}

/**
 * 키에 해당하는 Int 값의 Flow를 반환하는 확장 함수
 * null인 경우 기본값 사용
 * 
 * @param key 키 값
 * @param defaultValue 기본값
 * @return Int 값의 Flow (null이 아님)
 */
fun SavedStateHandle.getIntFlowOrDefault(key: String, defaultValue: Int = 0): Flow<Int> {
    return getIntFlow(key).map { it ?: defaultValue }
}

/**
 * 키에 해당하는 Boolean 값의 Flow를 반환하는 확장 함수
 * 
 * @param key 키 값
 * @return Boolean 값의 Flow
 */
fun SavedStateHandle.getBooleanFlow(key: String): Flow<Boolean?> {
    return getStateFlow<Boolean?>(key, null)
}

/**
 * 키에 해당하는 Boolean 값의 Flow를 반환하는 확장 함수
 * null인 경우 기본값 사용
 * 
 * @param key 키 값
 * @param defaultValue 기본값
 * @return Boolean 값의 Flow (null이 아님)
 */
fun SavedStateHandle.getBooleanFlowOrDefault(key: String, defaultValue: Boolean = false): Flow<Boolean> {
    return getBooleanFlow(key).map { it ?: defaultValue }
}

// --- Result를 반환하는 함수들 ---

/**
 * Result로 감싼 String 값을 가져오는 확장 함수
 * 
 * @param key 키 값
 * @return 성공: 문자열 값, 실패: 해당 파라미터 없음 오류
 */
fun SavedStateHandle.getStringResult(key: String): Result<String> {
    return runCatching { 
        getString(key) ?: throw IllegalArgumentException("Parameter '$key' is missing") 
    }
}

/**
 * Result로 감싼 Int 값을 가져오는 확장 함수
 * 
 * @param key 키 값
 * @return 성공: Int 값, 실패: 해당 파라미터 없음 오류
 */
fun SavedStateHandle.getIntResult(key: String): Result<Int> {
    return runCatching { 
        getInt(key) ?: throw IllegalArgumentException("Parameter '$key' is missing") 
    }
}

/**
 * Result로 감싼 Long 값을 가져오는 확장 함수
 * 
 * @param key 키 값
 * @return 성공: Long 값, 실패: 해당 파라미터 없음 오류
 */
fun SavedStateHandle.getLongResult(key: String): Result<Long> {
    return runCatching { 
        getLong(key) ?: throw IllegalArgumentException("Parameter '$key' is missing") 
    }
}

/**
 * Result로 감싼 Boolean 값을 가져오는 확장 함수
 * 
 * @param key 키 값
 * @return 성공: Boolean 값, 실패: 해당 파라미터 없음 오류
 */
fun SavedStateHandle.getBooleanResult(key: String): Result<Boolean> {
    return runCatching { 
        getBoolean(key) ?: throw IllegalArgumentException("Parameter '$key' is missing") 
    }
}

/**
 * Result로 감싼 제네릭 타입 값을 가져오는 확장 함수
 * 
 * @param key 키 값
 * @return 성공: T 타입 값, 실패: 해당 파라미터 없음 오류
 */
inline fun <reified T> SavedStateHandle.getResult(key: String): Result<T> {
    return runCatching { 
        get<T>(key) ?: throw IllegalArgumentException("Parameter '$key' is missing") 
    }
} 