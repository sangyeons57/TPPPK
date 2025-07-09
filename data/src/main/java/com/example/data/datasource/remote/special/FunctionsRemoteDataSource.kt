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
import com.example.core_common.constants.FirebaseFunctionParameters
import com.example.domain.model.base.ProjectInvitation
import kotlinx.coroutines.flow.Flow

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
        name: String?,
        memo: String?
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * 친구 요청을 보냅니다.
     *
     * @param targetUserId 친구 요청을 보낼 대상 사용자 ID
     * @return 성공 시 친구 요청 정보, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun sendFriendRequest(targetUserId: String): CustomResult<Map<String, Any?>, Exception>

    /**
     * 친구 요청을 수락합니다.
     *
     * @param friendRequestId 수락할 친구 요청 ID
     * @return 성공 시 수락 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun acceptFriendRequest(friendRequestId: String): CustomResult<Map<String, Any?>, Exception>

    /**
     * 친구 요청을 거절합니다.
     *
     * @param friendRequestId 거절할 친구 요청 ID
     * @return 성공 시 거절 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun rejectFriendRequest(friendRequestId: String): CustomResult<Map<String, Any?>, Exception>

    /**
     * 친구를 제거합니다.
     *
     * @param friendUserId 제거할 친구의 사용자 ID
     * @return 성공 시 제거 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun removeFriend(friendUserId: String): CustomResult<Map<String, Any?>, Exception>

    /**
     * 친구 목록을 조회합니다.
     *
     * @param status 조회할 친구 상태 (nullable)
     * @param limit 조회할 최대 개수 (nullable)
     * @param offset 조회 시작 위치 (nullable)
     * @return 성공 시 친구 목록, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun getFriends(
        status: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * 친구 요청 목록을 조회합니다.
     *
     * @param type 조회할 요청 타입 ("received" 또는 "sent")
     * @param limit 조회할 최대 개수 (nullable)
     * @param offset 조회 시작 위치 (nullable)
     * @return 성공 시 친구 요청 목록, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun getFriendRequests(
        type: String,
        limit: Int? = null,
        offset: Int? = null
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * 사용자 이름을 통해 DM 채널을 생성합니다.
     *
     * @param targetUserName 대상 사용자 이름
     * @return 성공 시 DM 채널 정보, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun createDMChannel(targetUserName: String): CustomResult<Map<String, Any?>, Exception>

    /**
     * DM 채널을 차단합니다.
     *
     * @param channelId 차단할 DM 채널 ID
     * @return 성공 시 차단 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun blockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception>

    /**
     * DM 채널 차단을 해제합니다.
     *
     * @param channelId 차단 해제할 DM 채널 ID
     * @return 성공 시 차단 해제 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun unblockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception>
    
    /**
     * 사용자 이름을 통해 DM 채널 차단을 해제합니다.
     *
     * @param targetUserName 차단 해제할 대상 사용자 이름
     * @return 성공 시 차단 해제 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun unblockDMChannelByUserName(targetUserName: String): CustomResult<Map<String, Any?>, Exception>

    /**
     * 프로젝트 초대 링크를 생성합니다.
     *
     * @param projectId 프로젝트 ID
     * @param expiresInHours 만료 시간 (시간 단위, 기본 24시간)
     * @param maxUses 최대 사용 횟수 (nullable)
     * @return 성공 시 초대 링크 정보, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun generateInviteLink(
        projectId: String,
        expiresInHours: Int = 24,
        maxUses: Int? = null
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * 초대 코드를 검증합니다.
     *
     * @param inviteCode 초대 코드
     * @return 성공 시 초대 정보, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun validateInviteCode(inviteCode: String): CustomResult<Map<String, Any?>, Exception>

    /**
     * 초대 코드를 사용하여 프로젝트에 참여합니다.
     *
     * @param inviteCode 초대 코드
     * @return 성공 시 참여 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun joinProjectWithInvite(inviteCode: String): CustomResult<Map<String, Any?>, Exception>

    /**
     * 프로젝트를 삭제합니다 (soft delete).
     *
     * @param projectId 삭제할 프로젝트 ID
     * @return 성공 시 삭제 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun deleteProject(projectId: String): CustomResult<Map<String, Any?>, Exception>

    /**
     * 프로젝트에서 나갑니다.
     *
     * @param projectId 나갈 프로젝트 ID
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun leaveProject(projectId: String): CustomResult<Unit, Exception>

    // ================== 프로젝트 초대 관련 메서드 ==================

    /**
     * 프로젝트 초대를 보냅니다.
     *
     * @param projectId 프로젝트 ID
     * @param inviteeId 초대받을 사용자 ID
     * @param message 초대 메시지 (선택사항)
     * @param expiresInHours 만료 시간 (시간 단위, 기본 72시간)
     * @return 성공 시 생성된 초대 객체, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun sendProjectInvitation(
        projectId: String,
        inviteeId: String,
        message: String? = null,
        expiresInHours: Long = 72L
    ): CustomResult<com.example.domain.model.base.ProjectInvitation, Exception>

    /**
     * 프로젝트 초대를 수락합니다.
     *
     * @param invitationId 초대 ID
     * @return 성공 시 수락된 초대 객체, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun acceptProjectInvitation(
        invitationId: String
    ): CustomResult<com.example.domain.model.base.ProjectInvitation, Exception>

    /**
     * 프로젝트 초대를 거절합니다.
     *
     * @param invitationId 초대 ID
     * @return 성공 시 거절된 초대 객체, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun rejectProjectInvitation(
        invitationId: String
    ): CustomResult<com.example.domain.model.base.ProjectInvitation, Exception>

    /**
     * 프로젝트 초대를 취소합니다.
     *
     * @param invitationId 초대 ID
     * @return 성공 시 취소된 초대 객체, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun cancelProjectInvitation(
        invitationId: String
    ): CustomResult<com.example.domain.model.base.ProjectInvitation, Exception>

    /**
     * 받은 초대 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param status 조회할 상태 (null이면 모든 상태)
     * @return 성공 시 초대 목록 Flow, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun getReceivedInvitations(
        userId: String,
        status: String? = null
    ): kotlinx.coroutines.flow.Flow<CustomResult<List<com.example.domain.model.base.ProjectInvitation>, Exception>>

    /**
     * 보낸 초대 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param projectId 프로젝트 ID (null이면 모든 프로젝트)
     * @param status 조회할 상태 (null이면 모든 상태)
     * @return 성공 시 초대 목록 Flow, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun getSentInvitations(
        userId: String,
        projectId: String? = null,
        status: String? = null
    ): kotlinx.coroutines.flow.Flow<CustomResult<List<com.example.domain.model.base.ProjectInvitation>, Exception>>

    /**
     * 특정 프로젝트의 초대 목록을 조회합니다.
     *
     * @param projectId 프로젝트 ID
     * @param status 조회할 상태 (null이면 모든 상태)
     * @return 성공 시 초대 목록 Flow, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun getProjectInvitations(
        projectId: String,
        status: String? = null
    ): kotlinx.coroutines.flow.Flow<CustomResult<List<com.example.domain.model.base.ProjectInvitation>, Exception>>

    /**
     * 특정 초대를 조회합니다.
     *
     * @param invitationId 초대 ID
     * @return 성공 시 초대 객체, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun getProjectInvitation(
        invitationId: String
    ): CustomResult<com.example.domain.model.base.ProjectInvitation, Exception>

    /**
     * 중복 초대 확인 (같은 프로젝트에 같은 사용자가 이미 초대받았는지)
     *
     * @param projectId 프로젝트 ID
     * @param inviteeId 초대받을 사용자 ID
     * @return 성공 시 중복 초대 여부, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun hasPendingInvitation(
        projectId: String,
        inviteeId: String
    ): CustomResult<Boolean, Exception>

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
            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.HELLO_WORLD)
            
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
    ): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val requestData = mutableMapOf<String, Any?>(
                FirebaseFunctionParameters.User.USER_ID to currentUser.uid
            )
            
            name?.let { requestData[FirebaseFunctionParameters.User.NAME] = it }
            memo?.let { requestData[FirebaseFunctionParameters.User.MEMO] = it }

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.UPDATE_USER_PROFILE)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Update user profile function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun sendFriendRequest(targetUserId: String): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val requestData = mapOf(
                FirebaseFunctionParameters.Friend.REQUESTER_ID to currentUser.uid,
                FirebaseFunctionParameters.Friend.RECEIVER_USER_ID to targetUserId
            )

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.SEND_FRIEND_REQUEST)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Send friend request function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun acceptFriendRequest(friendRequestId: String): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val requestData = mapOf(
                FirebaseFunctionParameters.Friend.FRIEND_REQUEST_ID to friendRequestId,
                FirebaseFunctionParameters.Friend.USER_ID to currentUser.uid
            )

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.ACCEPT_FRIEND_REQUEST)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Accept friend request function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun rejectFriendRequest(friendRequestId: String): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val requestData = mapOf(
                FirebaseFunctionParameters.Friend.FRIEND_REQUEST_ID to friendRequestId,
                FirebaseFunctionParameters.Friend.USER_ID to currentUser.uid
            )

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.REJECT_FRIEND_REQUEST)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Reject friend request function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun removeFriend(friendUserId: String): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val requestData = mapOf(
                FirebaseFunctionParameters.Friend.USER_ID to currentUser.uid,
                FirebaseFunctionParameters.Friend.FRIEND_USER_ID to friendUserId
            )

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.REMOVE_FRIEND)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Remove friend function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun getFriends(
        status: String?,
        limit: Int?,
        offset: Int?
    ): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val requestData = mutableMapOf<String, Any?>(FirebaseFunctionParameters.Friend.USER_ID to currentUser.uid)
            status?.let { requestData[FirebaseFunctionParameters.Friend.STATUS] = it }
            limit?.let { requestData[FirebaseFunctionParameters.Friend.LIMIT] = it }
            offset?.let { requestData[FirebaseFunctionParameters.Friend.OFFSET] = it }

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.GET_FRIENDS)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Get friends function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun getFriendRequests(
        type: String,
        limit: Int?,
        offset: Int?
    ): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val requestData = mutableMapOf<String, Any?>(
                FirebaseFunctionParameters.Friend.USER_ID to currentUser.uid,
                FirebaseFunctionParameters.Friend.TYPE to type
            )
            limit?.let { requestData[FirebaseFunctionParameters.Friend.LIMIT] = it }
            offset?.let { requestData[FirebaseFunctionParameters.Friend.OFFSET] = it }

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.GET_FRIEND_REQUESTS)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Get friend requests function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun createDMChannel(targetUserName: String): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val requestData = mapOf(
                FirebaseFunctionParameters.DM.CURRENT_USER_ID to currentUser.uid,
                FirebaseFunctionParameters.DM.TARGET_USER_NAME to targetUserName
            )

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.CREATE_DM_CHANNEL)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Create DM channel function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun blockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val requestData = mapOf(
                FirebaseFunctionParameters.DM.CURRENT_USER_ID to currentUser.uid,
                FirebaseFunctionParameters.DM.CHANNEL_ID to channelId
            )

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.BLOCK_DM_CHANNEL)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Block DM channel function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun unblockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val requestData = mapOf(
                FirebaseFunctionParameters.DM.CURRENT_USER_ID to currentUser.uid,
                FirebaseFunctionParameters.DM.CHANNEL_ID to channelId
            )

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.UNBLOCK_DM_CHANNEL)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Unblock DM channel function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun unblockDMChannelByUserName(targetUserName: String): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val requestData = mapOf(
                FirebaseFunctionParameters.DM.CURRENT_USER_ID to currentUser.uid,
                FirebaseFunctionParameters.DM.TARGET_USER_NAME to targetUserName
            )

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.UNBLOCK_DM_CHANNEL_BY_USER_NAME)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Unblock DM channel by user name function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun generateInviteLink(
        projectId: String,
        expiresInHours: Int,
        maxUses: Int?
    ): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val requestData = mutableMapOf<String, Any?>(
                FirebaseFunctionParameters.Project.PROJECT_ID to projectId,
                FirebaseFunctionParameters.Project.INVITER_ID to currentUser.uid,
                FirebaseFunctionParameters.Project.EXPIRES_IN_HOURS to expiresInHours
            )
            maxUses?.let { requestData[FirebaseFunctionParameters.Project.MAX_USES] = it }

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.GENERATE_INVITE_LINK)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Generate invite link function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun validateInviteCode(inviteCode: String): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val requestData = mutableMapOf<String, Any?>(FirebaseFunctionParameters.Project.INVITE_CODE to inviteCode)
            val currentUser = auth.currentUser
            currentUser?.let { requestData[FirebaseFunctionParameters.User.USER_ID] = it.uid }

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.VALIDATE_INVITE_CODE)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Validate invite code function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun joinProjectWithInvite(inviteCode: String): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            val requestData = mapOf(
                FirebaseFunctionParameters.Project.INVITE_CODE to inviteCode,
                FirebaseFunctionParameters.User.USER_ID to currentUser.uid
            )

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.JOIN_PROJECT_WITH_INVITE)
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                CustomResult.Failure(Exception("Join project with invite function call timed out"))
            }
        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteProject(projectId: String): CustomResult<Map<String, Any?>, Exception> = withContext(Dispatchers.IO) {
        try {
            Log.d("FunctionsRemoteDataSource", "Starting deleteProject for projectId: $projectId")
            
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            Log.d("FunctionsRemoteDataSource", "Current user: ${currentUser.uid}")
            
            val requestData = mapOf(
                FirebaseFunctionParameters.Project.PROJECT_ID to projectId,
                FirebaseFunctionParameters.Project.DELETED_BY to currentUser.uid
            )
            Log.d("FunctionsRemoteDataSource", "Request data: $requestData")

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.DELETE_PROJECT)
            Log.d("FunctionsRemoteDataSource", "Calling Firebase Function: ${FirebaseFunctionParameters.Functions.DELETE_PROJECT}")
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                Log.d("FunctionsRemoteDataSource", "Firebase Function call successful, result: ${result.data}")
                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any?> ?: mapOf("result" to result.data)
                CustomResult.Success(resultData)
            } else {
                Log.e("FunctionsRemoteDataSource", "Firebase Function call timed out after ${DEFAULT_TIMEOUT_MS}ms")
                CustomResult.Failure(Exception("Delete project function call timed out"))
            }
        } catch (e: Exception) {
            Log.e("FunctionsRemoteDataSource", "Exception in deleteProject", e)
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun leaveProject(projectId: String): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        try {
            Log.d("FunctionsRemoteDataSource", "Starting leaveProject for projectId: $projectId")
            
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            Log.d("FunctionsRemoteDataSource", "Current user: ${currentUser.uid}")
            
            val requestData = mapOf(
                FirebaseFunctionParameters.Project.PROJECT_ID to projectId,
                FirebaseFunctionParameters.User.USER_ID to currentUser.uid
            )
            Log.d("FunctionsRemoteDataSource", "Request data: $requestData")

            val callable = functions.getHttpsCallable(FirebaseFunctionParameters.Functions.LEAVE_PROJECT)
            Log.d("FunctionsRemoteDataSource", "Calling Firebase Function: ${FirebaseFunctionParameters.Functions.LEAVE_PROJECT}")
            
            val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                callable.call(requestData).await()
            }

            if (result != null) {
                Log.d("FunctionsRemoteDataSource", "Leave project function call successful")
                CustomResult.Success(Unit)
            } else {
                Log.e("FunctionsRemoteDataSource", "Leave project function call timed out after ${DEFAULT_TIMEOUT_MS}ms")
                CustomResult.Failure(Exception("Leave project function call timed out"))
            }
        } catch (e: Exception) {
            Log.e("FunctionsRemoteDataSource", "Exception in leaveProject", e)
            if (e is java.util.concurrent.CancellationException) throw e
            CustomResult.Failure(e)
        }
    }

    override suspend fun sendProjectInvitation(
        projectId: String,
        inviteeId: String,
        message: String?,
        expiresInHours: Long
    ): CustomResult<ProjectInvitation, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun acceptProjectInvitation(invitationId: String): CustomResult<ProjectInvitation, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun rejectProjectInvitation(invitationId: String): CustomResult<ProjectInvitation, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun cancelProjectInvitation(invitationId: String): CustomResult<ProjectInvitation, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun getReceivedInvitations(
        userId: String,
        status: String?
    ): Flow<CustomResult<List<ProjectInvitation>, Exception>> {
        TODO("Not yet implemented")
    }

    override suspend fun getSentInvitations(
        userId: String,
        projectId: String?,
        status: String?
    ): Flow<CustomResult<List<ProjectInvitation>, Exception>> {
        TODO("Not yet implemented")
    }

    override suspend fun getProjectInvitations(
        projectId: String,
        status: String?
    ): Flow<CustomResult<List<ProjectInvitation>, Exception>> {
        TODO("Not yet implemented")
    }

    override suspend fun getProjectInvitation(invitationId: String): CustomResult<ProjectInvitation, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun hasPendingInvitation(
        projectId: String,
        inviteeId: String
    ): CustomResult<Boolean, Exception> {
        TODO("Not yet implemented")
    }

}