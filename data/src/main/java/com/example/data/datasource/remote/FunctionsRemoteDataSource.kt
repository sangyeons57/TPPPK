package com.example.data.datasource.remote

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Functions와 통신하는 Remote Data Source
 * Firebase Functions의 callable functions를 호출합니다.
 */
interface FunctionsRemoteDataSource {
    
    /**
     * Firebase Functions의 callable function을 호출합니다.
     */
    suspend fun callFunction(
        functionName: String,
        data: Map<String, Any?>? = null
    ): CustomResult<Map<String, Any?>, Exception>
    
    /**
     * Hello World 메시지를 반환하는 함수를 호출합니다.
     */
    suspend fun getHelloWorld(): CustomResult<String, Exception>
    
    /**
     * 사용자 프로필 이미지를 Firebase Storage에 업로드합니다.
     */
    suspend fun uploadProfileImage(uri: Uri): CustomResult<Unit, Exception>
}

@Singleton
class FunctionsRemoteDataSourceImpl @Inject constructor(
    private val firebaseFunctions: FirebaseFunctions,
    private val firebaseStorage: FirebaseStorage,
    private val firebaseAuth: FirebaseAuth
) : FunctionsRemoteDataSource {
    
    override suspend fun callFunction(
        functionName: String,
        data: Map<String, Any?>?
    ): CustomResult<Map<String, Any?>, Exception> = resultTry {
        val result = firebaseFunctions
            .getHttpsCallable(functionName)
            .call(data)
            .await()
        
        @Suppress("UNCHECKED_CAST")
        result.data as? Map<String, Any?> ?: emptyMap()
    }
    
    override suspend fun getHelloWorld(): CustomResult<String, Exception> = resultTry {
        val data = mapOf("message" to "Android Client")
        
        val result = firebaseFunctions
            .getHttpsCallable("helloWorld")
            .call(data)
            .await()
        
        @Suppress("UNCHECKED_CAST")
        val responseData = result.data as? Map<String, Any?>
        responseData?.get("message") as? String ?: "No message received"
    }
    
    override suspend fun uploadProfileImage(uri: Uri): CustomResult<Unit, Exception> = resultTry {
        val currentUser = firebaseAuth.currentUser
            ?: throw IllegalStateException("User must be authenticated to upload profile image")
        
        val userId = currentUser.uid
        val fileName = "profile_${System.currentTimeMillis()}.jpg"
        val storageRef = firebaseStorage.reference
            .child("user_profile_uploads")
            .child(userId)
            .child(fileName)
        
        storageRef.putFile(uri).await()
        Unit
    }
}