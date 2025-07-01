package com.example.data.datasource.remote.special

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.core_common.util.MediaUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

interface FunctionsRemoteDataSource {

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

    /**
     * 프로젝트 프로필 이미지를 업로드합니다.
     * Firebase Storage에 업로드 후 자동으로 Firebase Functions가 처리합니다.
     *
     * @param projectId 프로젝트 ID
     * @param uri 업로드할 이미지의 URI
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun uploadProjectProfileImage(
        projectId: com.example.domain.model.vo.DocumentId,
        uri: Uri
    ): CustomResult<Unit, Exception>

    /**
     * 사용자 프로필을 업데이트합니다.
     * Firebase Functions를 통해 이름, 메모 등의 프로필 정보를 업데이트합니다.
     *
     * @param name 새로운 사용자 이름 (nullable)
     * @param memo 새로운 사용자 메모 (nullable)
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun updateUserProfile(
        name: String? = null,
        memo: String? = null
    ): CustomResult<Unit, Exception>
}

@Singleton
class FunctionsRemoteDataSourceImpl @Inject constructor(
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) : FunctionsRemoteDataSource {

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 30000L
    }

    override suspend fun callFunction(
        functionName: String,
        data: Map<String, Any?>?
    ): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val callable = functions.getHttpsCallable(functionName)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                if (data != null) {
                    callable.call(data).await()
                } else {
                    callable.call().await()
                }
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Function call timed out after ${DEFAULT_TIMEOUT_MS}ms"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun getHelloWorld(): CustomResult<String, Exception> = withContext(Dispatchers.IO) {
        try {
            val callable = functions.getHttpsCallable("helloWorld")
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call().await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val responseData = result.data as? Map<String, Any?>
                val message = responseData?.get("message") as? String 
                    ?: result.data as? String 
                    ?: result.data.toString()
                CustomResult.Success(message)
            } else {
                CustomResult.Failure(Exception("Hello World function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun callFunctionWithUserData(
        functionName: String,
        userId: String,
        customData: Map<String, Any?>?
    ): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val requestData = mutableMapOf<String, Any?>("userId" to userId)
            customData?.let { requestData.putAll(it) }

            val callable = functions.getHttpsCallable(functionName)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Function call with user data timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun uploadUserProfileImage(uri: Uri): CustomResult<Unit, Exception> = resultTry {
        // 현재 인증된 사용자 ID 가져오기
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        val userId = currentUser.uid

        // mimeType 및 확장자 계산
        val mimeType = MediaUtil.getMimeType(storage.app.applicationContext, uri) ?: "image/jpeg"

        // 파일 크기 제한 확인 (1MB)
        val fileSize = MediaUtil.getFileSize(storage.app.applicationContext, uri)
        if (fileSize > 0 && fileSize > 1 * 1024 * 1024) {
            throw Exception("File size exceeds 1MB limit. Please choose a smaller image.")
        }

        // 이미지 파일 타입 확인
        if (!mimeType.startsWith("image/")) {
            throw Exception("Only image files are allowed for profile pictures.")
        }
        
        val extFromUri = MediaUtil.getFileExtension(uri)
        val extFromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        val extension = extFromUri ?: extFromMime ?: "jpg"

        val profileImageRef = storage.reference.child(
            "user_profile_images/$userId/${System.currentTimeMillis()}_profile.$extension"
        )

        // 메타데이터 설정
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType(mimeType)
            .build()

        try {
            // 이미지 업로드
            profileImageRef.putFile(uri, metadata).await()

            // 업로드가 성공하면 Firebase Functions onUserProfileImageUpload가 자동으로 처리됨
            // 별도의 URL 처리나 Firestore 업데이트 불필요
        } catch (e: Exception) {
            // 권한 오류에 대한 더 명확한 메시지 제공
            when {
                e.message?.contains("403") == true || e.message?.contains("Permission denied") == true ->
                    throw Exception("You don't have permission to update your profile image. Please ensure you are logged in.")
                e.message?.contains("413") == true || e.message?.contains("too large") == true ->
                    throw Exception("Image file is too large. Please choose an image smaller than 1MB.")
                else -> throw Exception("Failed to upload profile image: ${e.message}")
            }
        }
    }

    override suspend fun uploadProjectProfileImage(
        projectId: com.example.domain.model.vo.DocumentId,
        uri: Uri
    ): CustomResult<Unit, Exception> = resultTry {
        // 현재 인증된 사용자 확인
        Log.d("FunctionRemoteDatasource", "start projectId: ${projectId.value}")
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        Log.d("FunctionRemoteDatasource", "currentUser:  ${currentUser}")
        // Firebase Storage에 이미지 업로드 (프로젝트별 경로 사용)
        val mimeType = MediaUtil.getMimeType(storage.app.applicationContext, uri) ?: "image/jpeg"

        // 파일 크기 제한 확인 (2MB)
        val fileSize = MediaUtil.getFileSize(storage.app.applicationContext, uri)
        if (fileSize > 0 && fileSize > 2 * 1024 * 1024) {
            throw Exception("File size exceeds 2MB limit. Please choose a smaller image.")
        }

        // 이미지 파일 타입 확인
        if (!mimeType.startsWith("image/")) {
            throw Exception("Only image files are allowed for project profile pictures.")
        }

        // 확장자 보존을 위해 파일 이름에 extension 추가
        val extension = MediaUtil.getFileExtension(uri)
        val projectImageRef = storage.reference.child(
            "project_profile_images/${projectId.value}/${System.currentTimeMillis()}_profile.$extension"
        )
        Log.d("FunctionRemoteDatasource", projectImageRef.path)
        Log.d("FunctionRemoteDatasource", projectImageRef.bucket)
        Log.d("FunctionRemoteDatasource", projectImageRef.name)

        // 메타데이터 설정 (contentType 필수)
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType(mimeType)
            .build()

        try {
            Log.d("FunctionRemoteDatasource", "upload")
            // 이미지 업로드 (확장자 유지)
            projectImageRef.putFile(uri, metadata).await()

            // 업로드가 성공하면 Firebase Functions onProjectProfileImageUpload가 자동으로 처리됨
            // 별도의 URL 처리나 Firestore 업데이트 불필요
        } catch (e: Exception) {
            // 권한 오류에 대한 더 명확한 메시지 제공
            when {
                e.message?.contains("403") == true || e.message?.contains("Permission denied") == true ->
                    throw Exception("You don't have permission to update this project's profile image. Only project members can change project profile pictures.")
                e.message?.contains("413") == true || e.message?.contains("too large") == true ->
                    throw Exception("Image file is too large. Please choose an image smaller than 2MB.")
                else -> throw Exception("Failed to upload project profile image: ${e.message}")
            }
        }
    }

    override suspend fun updateUserProfile(
        name: String?,
        memo: String?
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        try {
            // 업데이트할 데이터가 없으면 에러
            if (name == null && memo == null) {
                return@withContext CustomResult.Failure(Exception("No data to update"))
            }

            // 요청 데이터 구성
            val requestData = mutableMapOf<String, Any?>()
            name?.let { requestData["name"] = it }
            memo?.let { requestData["memo"] = it }

            val callable = functions.getHttpsCallable("updateUserProfile")
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val responseData = result.data as? Map<String, Any?>
                val success = responseData?.get("success") as? Boolean ?: false
                
                if (success) {
                    CustomResult.Success(Unit)
                } else {
                    val message = responseData?.get("message") as? String ?: "Profile update failed"
                    CustomResult.Failure(Exception(message))
                }
            } else {
                CustomResult.Failure(Exception("Update user profile function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }
}