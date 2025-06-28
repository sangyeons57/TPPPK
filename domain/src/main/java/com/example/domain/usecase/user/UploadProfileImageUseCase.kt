package com.example.domain.usecase.user

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.repository.FunctionsRepository
import javax.inject.Inject

/**
 * 사용자 프로필 이미지를 업로드하는 UseCase
 *
 * Firebase Functions를 통해 이미지를 업로드하고 자동으로 처리되도록 합니다.
 * Firebase Functions가 이미지 최적화 및 Firestore 업데이트를 자동으로 처리합니다.
 */
class UploadProfileImageUseCase @Inject constructor(
    private val functionsRepository: FunctionsRepository
) {
    /**
     * 이미지 URI를 Firebase Storage에 업로드합니다.
     * 업로드 후 Firebase Functions가 자동으로 이미지 처리 및 Firestore 업데이트를 수행합니다.
     * 
     * @param imageUri 업로드할 이미지의 URI
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend operator fun invoke(imageUri: Uri): CustomResult<Unit, Exception> {
        return functionsRepository.uploadUserProfileImage(imageUri)
    }
}