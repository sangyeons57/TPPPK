package com.example.domain.usecase.user

import android.content.Context
import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.core_common.util.MediaUtil
import com.example.domain.model.base.User
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * 사용자 프로필 이미지를 업로드하는 UseCase
 * 
 * 이 UseCase는 사용자가 선택한 이미지 URI를 받아 서버에 업로드하고
 * 프로필 이미지를 업데이트하는 기능을 담당합니다.
 */
class UploadProfileImageUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context // Context 주입을 위해 필요
) {
    /**
     * 이미지 URI를 받아 서버에 업로드하고 프로필 이미지를 업데이트합니다.
     * 
     * @param imageUri 업로드할 이미지의 URI
     * @return 성공 시 이미지 URL을 포함한 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    suspend operator fun invoke(imageUri: Uri): CustomResult<User, Exception> {
        // 1. 현재 사용자 세션 확인
        val session = authRepository.getCurrentUserSession()
        
        return when (session) {
            is CustomResult.Success -> {
                resultTry {
                    // 2. URI에서 InputStream 가져오기
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                        ?: throw IOException("Failed to open input stream from URI")
                    
                    // 3. MIME 타입 가져오기
                    val mimeType = MediaUtil.getMimeType(context, imageUri) ?: "image/jpeg" // 기본값 설정

                    val user = userRepository.getUserStream(session.data.userId).first()
                    if (user !is CustomResult.Success) {
                        throw Exception("Failed to get user")
                    }

                    // 4. 사용자 프로필 이미지 업데이트
                    val result = userRepository.updateUserProfile(
                        userId = session.data.userId,
                        user = user.data,
                        localImageUri = imageUri
                    )

                    // 5. 스트림 닫기
                    inputStream.close()

                    // 6. 결과 반환

                    return result
                }
            }
            is CustomResult.Failure -> {
                CustomResult.Failure(session.error)
            }
            else -> {
                CustomResult.Failure(Exception("Unknown error"))
            }
        }
    }
}
