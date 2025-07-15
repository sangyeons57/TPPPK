package com.example.domain.usecase.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMWrapper
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.DMWrapperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * 현재 로그인한 사용자의 DM Wrapper 목록을 스트림으로 가져오는 UseCase.
 * DMWrapper 데이터만 사용하여 DM 리스트에 필요한 모든 정보를 제공합니다.
 * DMChannel 조회 없이 더 빠른 성능을 제공합니다.
 */
class GetUserDmWrappersUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val dmWrapperRepository: DMWrapperRepository
) {
    /**
     * 현재 로그인한 사용자의 모든 DM Wrapper를 Flow로 반환합니다.
     *
     * @return DM Wrapper 목록(List<DMWrapper>)을 포함하는 CustomResult를 발행하는 Flow.
     *         진행 상태에 따라 Loading, Success, Failure를 발행할 수 있습니다.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<CustomResult<List<DMWrapper>, Exception>> =
        authRepository.getUserSessionStream()
            .flatMapLatest { sessionResult ->
                if (sessionResult !is CustomResult.Success) {
                    flowOf(CustomResult.Failure(Exception("Not authenticated")))
                } else {
                    dmWrapperRepository.observeAll().map { dmResult ->
                        when (dmResult) {
                            is CustomResult.Success ->
                                CustomResult.Success(dmResult.data.map { it as DMWrapper })
                            is CustomResult.Failure ->
                                CustomResult.Failure(dmResult.error)
                            else -> CustomResult.Loading
                        }
                    }
                }
            }
            .onStart { emit(CustomResult.Loading) }
            .catch { emit(CustomResult.Failure(Exception("Unexpected error", it))) }
}