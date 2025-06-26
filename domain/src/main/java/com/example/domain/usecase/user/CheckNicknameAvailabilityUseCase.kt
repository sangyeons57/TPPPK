package com.example.domain.usecase.user


import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.base.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 닉네임 중복 확인을 수행하는 UseCase 인터페이스
 */
interface CheckNicknameAvailabilityUseCase {
    /**
     * 지정된 닉네임의 사용 가능 여부를 확인합니다.
     *
     * @param name 확인할 닉네임
     * @return 성공 시 사용 가능 여부(Boolean)가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(name: UserName): CustomResult<Boolean, Exception>
}

/**
 * 닉네임 중복 확인을 수행하는 UseCase 구현체
 * 
 * @property userRepository 사용자 관련 기능을 제공하는 Repository
 */
class CheckNicknameAvailabilityUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : CheckNicknameAvailabilityUseCase {
    /**
     * 지정된 닉네임의 사용 가능 여부를 확인합니다.
     *
     * @param name 확인할 닉네임
     * @return 성공 시 사용 가능 여부(Boolean)가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    override suspend operator fun invoke(name: UserName): CustomResult<Boolean, Exception> {
        //("CheckNicknameAvailabilityUseCase", "Checking availability for nickname: $nickname")
        // findByNameStream returns a Flow. We are interested in the first emission
        // to determine if a user with that exact name already exists.
        return try {
            when (val result = userRepository.observeByName(name).first()) {
                is CustomResult.Success -> {
                    Log.d("CheckNicknameAvailabilityUseCase", "Nickname '$name' is already taken.")
                    // If a user is found, the nickname is NOT available.
                    //("CheckNicknameAvailabilityUseCase", "Nickname '$nickname' is already taken.")
                    CustomResult.Success(false)
                }
                is CustomResult.Failure -> {
                    // If the specific error is 'NoSuchElementException', it means no user was found, so nickname IS available.
                    if (result.error is NoSuchElementException) {
                        //("CheckNicknameAvailabilityUseCase", "Nickname '$nickname' is available.")
                        CustomResult.Success(true)
                    } else {
                        // Other errors are propagated.
                        // "Error checking nickname '$nickname': ${result.error.localizedMessage}")
                        CustomResult.Failure(result.error)
                    }
                }
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        } catch (e: Exception) {
            // Catch any exceptions from the Flow collection itself (e.g., if the Flow is empty and .first() is called, though findByNameStream should emit NoSuchElementException)
            //("CheckNicknameAvailabilityUseCase", "Exception during nickname check for '$nickname': ${e.localizedMessage}", e)
            CustomResult.Failure(e)
        }
    }
} 