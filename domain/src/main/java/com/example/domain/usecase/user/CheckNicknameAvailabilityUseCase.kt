package com.example.domain.usecase.user


import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.base.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 닉네임 중복 확인을 수행하는 UseCase 인터페이스
 */
interface CheckNicknameAvailabilityUseCase {
    /**
     * 지정된 닉네임의 사용 가능 여부를 실시간(Flow)으로 확인합니다.
     *
     * @param name 확인할 닉네임
     * @return Flow 로 방출되는 CustomResult<Boolean, Exception>
     *         - Success(true)  : 사용 가능
     *         - Success(false) : 이미 사용 중
     */
    operator fun invoke(name: UserName): Flow<CustomResult<Boolean, Exception>>
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
     * 지정된 닉네임의 사용 가능 여부를 실시간(Flow)으로 확인합니다.
     *
     * @param name 확인할 닉네임
     * @return Flow 로 방출되는 CustomResult<Boolean, Exception>
     *         - Success(true)  : 사용 가능
     *         - Success(false) : 이미 사용 중
     */
    override fun invoke(name: UserName): Flow<CustomResult<Boolean, Exception>> {
        return userRepository
            .observeByName(name)
            .map { result ->
                when (result) {
                    is CustomResult.Success -> {
                        // 동일 닉네임이 존재 → 사용 불가(false)
                        Log.d("CheckNicknameAvailabilityUseCase", "Nickname '$name' is already taken.")
                        CustomResult.Success(false)
                    }
                    is CustomResult.Failure -> {
                        // NoSuchElementException 이면 사용 가능(true)
                        if (result.error is NoSuchElementException) {
                            CustomResult.Success(true)
                        } else {
                            CustomResult.Failure(result.error)
                        }
                    }
                    is CustomResult.Loading -> CustomResult.Loading
                    is CustomResult.Initial -> CustomResult.Initial
                    is CustomResult.Progress -> CustomResult.Progress(result.progress)
                }
            }
            .distinctUntilChanged()
    }
} 