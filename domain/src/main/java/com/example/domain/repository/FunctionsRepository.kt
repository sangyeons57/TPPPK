package com.example.domain.repository

import android.net.Uri
import com.example.core_common.result.CustomResult

/**
 * Firebase Functions와 관련된 작업을 처리하는 Repository 인터페이스
 * Clean Architecture 원칙에 따라 domain layer에서 정의되며, data layer에서 구현됩니다.
 */
interface FunctionsRepository : Repository {

    /**
     * Firebase Functions의 callable function을 호출합니다.
     * 
     * @param functionName 호출할 함수 이름
     * @param data 함수에 전달할 데이터 (nullable)
     * @return 함수 실행 결과를 담은 CustomResult
     */
    suspend fun callFunction(
        functionName: String,
        data: Map<String, Any?>? = null
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * "Hello World" 메시지를 반환하는 함수를 호출합니다.
     * 
     * @return Hello World 메시지를 담은 CustomResult
     */
    suspend fun getHelloWorld(): CustomResult<String, Exception>

    /**
     * 사용자 정의 데이터와 함께 함수를 호출합니다.
     * 
     * @param functionName 호출할 함수 이름
     * @param userId 사용자 ID
     * @param customData 사용자 정의 데이터
     * @return 함수 실행 결과를 담은 CustomResult
     */
    suspend fun callFunctionWithUserData(
        functionName: String,
        userId: String,
        customData: Map<String, Any?>? = null
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * 사용자 프로필 이미지를 업로드합니다.
     * Firebase Storage에 업로드 후 자동으로 Firebase Functions가 처리합니다.
     *
     * @param uri 업로드할 이미지의 URI
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun uploadUserProfileImage(uri: Uri): CustomResult<Unit, Exception>

}