package com.example.teamnovapersonalprojectprojectingkotlin.data.repository

import android.net.Uri
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.UserProfile
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.UserStatus
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import kotlin.Result

class UserRepositoryImpl @Inject constructor(
    // TODO: UserApiService, UserDao, UserPreferencesDataSource 등 주입
) : UserRepository {

    override suspend fun getUserProfile(): Result<UserProfile> {
        println("UserRepositoryImpl: getUserProfile called (returning failure)")
        return Result.failure(NotImplementedError("구현 필요"))
        // 임시 성공 데이터 예시:
        // return Result.success(UserProfile("temp_user", "email@example.com", "임시사용자", null, "온라인"))
    }

    override suspend fun updateProfileImage(imageUri: Uri): Result<String?> {
        println("UserRepositoryImpl: updateProfileImage called (returning success with null URL)")
        return Result.success(null) // 임시로 null URL 반환
    }

    override suspend fun removeProfileImage(): Result<Unit> {
        println("UserRepositoryImpl: removeProfileImage called (returning success)")
        return Result.success(Unit)
    }

    override suspend fun updateUserName(newName: String): Result<Unit> {
        println("UserRepositoryImpl: updateUserName called for '$newName' (returning success)")
        return Result.success(Unit)
    }

    override suspend fun updateStatusMessage(newStatus: String): Result<Unit> {
        println("UserRepositoryImpl: updateStatusMessage called for '$newStatus' (returning success)")
        return Result.success(Unit)
    }

    override suspend fun getCurrentStatus(): Result<UserStatus> {
        println("UserRepositoryImpl: getCurrentStatus called (returning ONLINE)")
        return Result.success(UserStatus.ONLINE) // 임시 값
    }

    override suspend fun updateUserStatus(status: UserStatus): Result<Unit> {
        println("UserRepositoryImpl: updateUserStatus called with $status (returning success)")
        return Result.success(Unit)
    }
}