package com.example.domain.usecase.user

import android.net.Uri
import com.example.domain.repository.base.UserRepository
import javax.inject.Inject

/**
 * 사용자 이미지 업데이트 유스케이스 인터페이스
 */
interface UpdateUserImageUseCase {
    suspend operator fun invoke(imageUri: Uri): Result<String> // 성공 시 새 이미지 URL 반환
}

/**
 * UpdateUserImageUseCase의 구현체
 * @param userRepository 사용자 데이터 접근을 위한 Repository
 */
class UpdateUserImageUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : UpdateUserImageUseCase {

    /**
     * 유스케이스를 실행하여 사용자 이미지를 업데이트합니다.
     * @param imageUri 업데이트할 이미지의 Uri
     * @return Result<String> 업데이트 처리 결과 (성공 시 새로운 이미지 URL, 실패 시 Exception)
     */
    override suspend fun invoke(imageUri: Uri): Result<String> {
        // TODO: UserRepository에 updateProfileImage(imageUri) 함수 구현 필요 (이미지 업로드 및 URL 반환)
        // TODO: Firebaes Fuction 에서 구현 필요
        // return userRepository.updateProfileImage(imageUri)
        
        // 임시 구현 (성공 및 임시 URL 반환)
        // kotlin.coroutines.delay(1000) // Remove delay
        val newImageUrl = "https://picsum.photos/seed/${System.currentTimeMillis()}/100" // 임시 URL
        println("UseCase: UpdateUserImageUseCase - $newImageUrl (TODO: Implement actual logic)")
        return Result.success(newImageUrl)
    }
} 