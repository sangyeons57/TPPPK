// 경로: domain/repository/UserRepository.kt (신규 생성 또는 기존 인터페이스에 함수 추가)
package com.example.teamnovapersonalprojectprojectingkotlin.domain.repository

import android.net.Uri // ViewModel에서 Uri를 받아 처리
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.UserProfile
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.UserStatus
import kotlinx.coroutines.flow.Flow
import kotlin.Result

interface UserRepository {

    /** 사용자 프로필 정보 가져오기 */
    suspend fun getUserProfile(): Result<UserProfile> // Flow<UserProfile> 사용 가능

    /** 사용자 프로필 이미지 업데이트 */
    suspend fun updateProfileImage(imageUri: Uri): Result<String?> // 성공 시 새 이미지 URL 반환

    /** 사용자 프로필 이미지 제거 */
    suspend fun removeProfileImage(): Result<Unit>

    /** 사용자 이름 업데이트 */
    suspend fun updateUserName(newName: String): Result<Unit>

    /** 사용자 상태 메시지 업데이트 */
    suspend fun updateStatusMessage(newStatus: String): Result<Unit>

    /** 현재 사용자 상태 가져오기 */
    suspend fun getCurrentStatus(): Result<UserStatus> // 또는 Flow 사용

    /** 사용자 상태 업데이트 */
    suspend fun updateUserStatus(status: UserStatus): Result<Unit>
}