package com.example.domain.usecase.auth

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.delay
import java.time.Instant
import javax.inject.Inject

/**
 * 사용자 회원가입 기능을 수행하는 UseCase
 * 
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 * @property userRepository 사용자 관련 기능을 제공하는 Repository
 */
class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) {
    // Centralized TAG for Logcat filtering
    companion object {
        private const val TAG = "SignUpUseCase"
    }

    /**
     * 이메일, 비밀번호, 닉네임을 이용하여 회원가입을 수행합니다.
     * 1. Firebase Authentication에 계정 생성
     * 2. 닉네임 중복 확인 (선택적)
     * 3. Firestore에 사용자 프로필 생성
     *
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @param nickname 사용자 닉네임
     * @param consentTimeStamp 서비스 정책및 개인정보처리방침 동의 시간 (기본값은 현재 시간)
     * @return 성공 시 사용자 정보가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(
        email: String, 
        password: String, 
        nickname: String,
        consentTimeStamp: Instant = Instant.now()
    ): CustomResult<User, Exception> {

        Log.d(TAG, "Starting sign-up | email=$email, nickname=$nickname")
        // 0. Firebase Authentication 회원가입 시도
        when (val signUpRes = authRepository.signup(email, password)) {
            is CustomResult.Success -> {
                // 신규 회원가입 성공 → Firestore 프로필 생성
                val uid = signUpRes.data
                Log.d(TAG, "Auth signup success. uid=$uid")
                val newUser = User(
                    uid = uid,
                    email = email,
                    name = nickname,
                    consentTimeStamp = consentTimeStamp
                )

                return when (val createRes = userRepository.createUserProfile(newUser)) {
                    is CustomResult.Success -> {
                        Log.d(TAG, "Firestore profile created successfully for uid=$uid")
                        CustomResult.Success(newUser)
                    }
                    is CustomResult.Failure -> {
                        Log.d(TAG, "Failed to create user profile: ${createRes.error}")
                        CustomResult.Failure(createRes.error)
                    }
                    else -> CustomResult.Failure(Exception("Unknown error creating user profile"))
                }
            }
            is CustomResult.Failure -> {
                // 이메일 중복 등으로 회원가입 실패한 경우 → 재활성화 가능성 확인
                Log.d(TAG, "signUpRes.error: ${signUpRes.error}")
                val err = signUpRes.error
                val errMsg = err.message ?: ""
                val isEmailInUse = err is FirebaseAuthUserCollisionException || errMsg.contains("already in use", ignoreCase = true)
                if (isEmailInUse) {
                    when (val userRes = userRepository.getUserByEmail(email)) {
                        is CustomResult.Success -> {
                            val existingUser = userRes.data
                            Log.d(TAG, "Existing user fetched. status=${existingUser.accountStatus}")
                            return if (existingUser.accountStatus == UserAccountStatus.WITHDRAWN) {
                                reactivateWithdrawnAccount(email, password, nickname, consentTimeStamp)
                            } else {
                                CustomResult.Failure(Exception("Email already in use"))
                            }
                        }
                        else -> {
                            Log.d(TAG, "Failed to fetch user by email: ${userRes}")
                            // Firestore에서 사용자를 찾지 못하거나 오류 → 원본 오류 반환
                            return CustomResult.Failure(signUpRes.error)
                        }
                    }
                } else {
                    return CustomResult.Failure(signUpRes.error)
                }
            }
            else -> {
                Log.d(TAG, "Returning unreachable failure (this should not happen)")
                return CustomResult.Failure(Exception("Unknown sign-up error"))
            }
        }

        // Unreachable but required by Kotlin
        @Suppress("UNREACHABLE_CODE")
        return CustomResult.Failure(Exception("Unreachable"))
    }

    /**
     * 탈퇴한 계정의 재활성화 흐름을 처리합니다.
     */
    private suspend fun reactivateWithdrawnAccount(
        email: String,
        newPassword: String,
        newNickname: String,
        consentTimeStamp: Instant,
    ): CustomResult<User, Exception> {
        Log.d(TAG, "Reactivating withdrawn account for email=$email")
        // 1. 기존 계정으로 로그인 (사용자가 입력한 비밀번호가 기존 비밀번호라고 가정)
        val loginRes = authRepository.login(email, newPassword)
        if (loginRes is CustomResult.Failure) {
            Log.d(TAG, "Login failed during reactivation: ${loginRes.error}")
            return CustomResult.Failure(loginRes.error)
        }
        Log.d(TAG, "Login success during reactivation")

        // 2. 인증 메일 발송 (새로 로그인한 사용자로)
        val sendRes = authRepository.sendEmailVerification()
        if (sendRes is CustomResult.Failure) {
            Log.d(TAG, "Send verification email failed: ${sendRes.error}")
            return CustomResult.Failure(sendRes.error)
        }
        Log.d(TAG, "Verification email sent. Waiting for verification...")

        // 3. 이메일 인증 Polling
        val timeoutMs = 3 * 60 * 1000L // 3분
        val intervalMs = 5 * 1000L // 5초
        val startTime = System.currentTimeMillis()
        var verified = false
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            when (val checkRes = authRepository.checkEmailVerification()) {
                is CustomResult.Success -> {
                    if (checkRes.data) {
                        verified = true
                        Log.d(TAG, "Email verification complete!")
                        break
                    }
                }
                is CustomResult.Failure -> {
                    Log.d(TAG, "Verification check failed: ${checkRes.error}")
                    return CustomResult.Failure(checkRes.error)
                }
                else -> {}
            }
            delay(intervalMs)
        }

        if (!verified) {
            Log.d(TAG, "Email verification timeout")
            return CustomResult.Failure(Exception("Email verification timeout"))
        }

        // 4. 비밀번호 업데이트
        val pwUpdateRes = authRepository.updatePassword(newPassword)
        if (pwUpdateRes is CustomResult.Failure) {
            Log.d(TAG, "Password update failed: ${pwUpdateRes.error}")
            return CustomResult.Failure(pwUpdateRes.error)
        }

        // 5. Firestore 사용자 정보 업데이트
        val currentSessionRes = authRepository.getCurrentUserSession()
        if (currentSessionRes is CustomResult.Failure) {
            Log.d(TAG, "Failed to get current user session: ${currentSessionRes.error}")
            return CustomResult.Failure(currentSessionRes.error)
        }

        val uid = (currentSessionRes as CustomResult.Success).data.userId

        val updatedUser = User(
            uid = uid,
            email = email,
            name = newNickname,
            consentTimeStamp = consentTimeStamp,
            accountStatus = UserAccountStatus.ACTIVE
        )

        val updateRes = userRepository.updateUserProfile(uid, updatedUser)
        if (updateRes is CustomResult.Failure) {
            Log.d(TAG, "Firestore update failed: ${updateRes.error}")
            return CustomResult.Failure(updateRes.error)
        }

        Log.d(TAG, "Account reactivated successfully for uid=$uid")
        return CustomResult.Success(updatedUser)
    }
}