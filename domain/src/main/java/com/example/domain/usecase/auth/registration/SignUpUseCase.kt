package com.example.domain.usecase.auth.registration

import com.example.core_common.result.CustomResult
import com.example.domain.event.EventDispatcher
import com.example.domain.exception.AccountAlreadyExistsException
import com.example.domain.model.base.User
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.flow.first
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
    private val userRepository: UserRepository
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
        nickname: UserName,
        consentTimeStamp: Instant
    ): CustomResult<User, Exception> {
        // "Starting sign-up | email=$email, nickname=$nickname")

        when (val signUpRes = authRepository.signup(email, password)) {
            is CustomResult.Success -> {
                val uid = signUpRes.data
                // "Auth signup success. uid=$uid")
                val newUser = User.create(
                    id = DocumentId.from(uid),
                    email = UserEmail(email),
                    name = nickname,
                    consentTimeStamp = consentTimeStamp
                )
                return userRepository.save(newUser).let { saveResult ->
                    when (saveResult) {
                        is CustomResult.Success -> {
                            // "User aggregate persisted successfully for uid=$uid")
                            EventDispatcher.publish(newUser)
                            CustomResult.Success(newUser)
                        }

                        is CustomResult.Failure -> {
                            // Failed to persist user aggregate: ${saveResult.error}
                            saveResult
                        }
                        else -> CustomResult.Failure(Exception("Unknown error creating user profile"))
                    }
                }
            }
            is CustomResult.Failure -> {
                val err = signUpRes.error
                val isEmailInUse = err is FirebaseAuthUserCollisionException || err.message?.contains("already in use", true) == true

                if (!isEmailInUse) {
                    // Sign-up failed for a reason other than email collision: ${err.message}
                    return CustomResult.Failure(err)
                }

                // Email collision detected. Checking for withdrawn account...
                try {
                    return when (val userRes = userRepository.observeByEmail(email).first()) {
                        is CustomResult.Success -> {
                            val existingUser = userRes.data
                            // Existing user found with status: ${existingUser.accountStatus}
                            if (existingUser.accountStatus == UserAccountStatus.WITHDRAWN) {
                                // Reactivate withdrawn account using user repository
                                // Reactivating withdrawn account for email: $email
                                existingUser.reactivateAccount()
                                existingUser.changeName(nickname)
                                
                                when (val saveResult = userRepository.save(existingUser)) {
                                    is CustomResult.Success -> {
                                        // "Account reactivated and saved for email: $email")
                                        EventDispatcher.publish(existingUser)
                                        CustomResult.Success(existingUser)
                                    }
                                    is CustomResult.Failure -> {
                                        // "Failed to save reactivated user data for email: $email", saveResult.error)
                                        saveResult
                                    }
                                    else -> CustomResult.Failure(Exception("Unknown error during saving reactivated user."))
                                }
                            } else {
                                CustomResult.Failure(AccountAlreadyExistsException("An account with this email already exists."))
                            }
                        }
                        is CustomResult.Failure -> {
                            // "Error fetching user by email after collision", userRes.error)
                            CustomResult.Failure(userRes.error)
                        }
                        else -> CustomResult.Failure(Exception("Unknown state while fetching user by email."))
                    }
                } catch (e: NoSuchElementException) {
                    // "Auth reported email collision, but findByEmailStream was empty for $email", e)
                    return CustomResult.Failure(Exception("Inconsistent state: Email is reported as in use, but no user profile was found."))
                } catch (e: Exception) {
                    // "An unexpected error occurred while checking for an existing user.", e)
                    return CustomResult.Failure(e)
                }
            }
            else -> {
                // "Sign-up process in an intermediate state (Loading/Initial)")
                return CustomResult.Failure(Exception("Sign-up process is currently in progress."))
            }
        }
    }
}