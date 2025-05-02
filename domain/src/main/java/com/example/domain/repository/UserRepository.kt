// 경로: domain/repository/UserRepository.kt (신규 생성 또는 기존 인터페이스에 함수 추가)
package com.example.domain.repository

import android.net.Uri // ViewModel에서 Uri를 받아 처리
import com.example.domain.model.User
import com.example.domain.model.UserStatus
import com.google.firebase.auth.FirebaseUser
import kotlin.Result

interface UserRepository {

    /** 사용자 프로필 정보 가져오기 */
    suspend fun getUser(): Result<User> // Flow<User> 사용 가능

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



    /**
     * 현재 FirebaseUser 정보를 기반으로 Firestore 사용자 문서가 존재하는지 확인하고,
     * 없으면 필요한 정보를 사용하여 문서를 생성합니다.
     * 최종적으로 Firestore 문서에 해당하는 User 객체를 반환합니다.
     * @param firebaseUser 현재 로그인된 Firebase 사용자 객체
     * @return 성공 시 Firestore 문서 데이터가 반영된 User 객체, 실패 시 에러 포함 Result
     */
    suspend fun ensureUserProfileExists(firebaseUser: FirebaseUser): Result<User>
}