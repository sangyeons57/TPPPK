package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.Token
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.repository.base.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeAuthRepository : AuthRepository {

    private var userSession: UserSession? = null
    private var shouldThrowError = false

    fun setShouldThrowError(shouldThrow: Boolean) {
        shouldThrowError = shouldThrow
    }

    fun setUserSession(session: UserSession?) {
        userSession = session
    }

    override suspend fun login(
        email: UserEmail,
        password: String
    ): CustomResult<UserSession, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Login failed"))
        }
        return userSession?.let {
            CustomResult.Success(it)
        } ?: CustomResult.Failure(Exception("User not found"))
    }

    override suspend fun isLoggedIn(): Boolean {
        return userSession != null
    }

    override suspend fun logout(): CustomResult<Unit, Exception> {
        userSession = null
        return CustomResult.Success(Unit)
    }

    override suspend fun signup(email: String, password: String): CustomResult<String, Exception> {
        if (shouldThrowError) {
            return CustomResult.Failure(Exception("Signup failed"))
        }
        val newUserId = "new_user_id"
        userSession = UserSession(
            userId = UserId(newUserId),
            token = Token("new_token"),
            email = UserEmail(email)
        )
        return CustomResult.Success(newUserId)
    }

    override suspend fun requestPasswordResetCode(email: String): CustomResult<Unit, Exception> {
        return if (shouldThrowError) CustomResult.Failure(Exception("Reset failed")) else CustomResult.Success(
            Unit
        )
    }

    override suspend fun sendEmailVerification(): CustomResult<Unit, Exception> {
        return if (shouldThrowError) CustomResult.Failure(Exception("Verification failed")) else CustomResult.Success(
            Unit
        )
    }

    override suspend fun checkEmailVerification(): CustomResult<Boolean, Exception> {
        return if (shouldThrowError) CustomResult.Failure(Exception("Check verification failed")) else CustomResult.Success(
            true
        )
    }

    override suspend fun updatePassword(newPassword: String): CustomResult<Unit, Exception> {
        return if (shouldThrowError) CustomResult.Failure(Exception("Update password failed")) else CustomResult.Success(
            Unit
        )
    }

    override suspend fun withdrawCurrentUser(): CustomResult<Unit, Exception> {
        userSession = null
        return CustomResult.Success(Unit)
    }

    override suspend fun getLoginErrorMessage(exception: Throwable): String {
        return "Login error: ${exception.message}"
    }

    override suspend fun getSignUpErrorMessage(exception: Throwable): String {
        return "Signup error: ${exception.message}"
    }

    override suspend fun getPasswordResetErrorMessage(exception: Throwable): String {
        return "Password reset error: ${exception.message}"
    }

    override suspend fun getCurrentUserSession(): CustomResult<UserSession, Exception> {
        return userSession?.let {
            CustomResult.Success(it)
        } ?: CustomResult.Failure(Exception("No session"))
    }

    override suspend fun getUserSessionStream(): Flow<CustomResult<UserSession, Exception>> {
        return flowOf(getCurrentUserSession())
    }
}