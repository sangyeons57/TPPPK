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
    operator fun invoke(): Flow<CustomResult<List<DMWrapper>, Exception>> = flow {
        Log.d("GetUserDmWrappersUseCase", "Flow started → emitting Loading")
        emit(CustomResult.Loading)

        // 1. Get current user session
        val currentUserSessionResult = authRepository.getCurrentUserSession().also {
            Log.d("GetUserDmWrappersUseCase", "getCurrentUserSession() result = $it")
        }
        if (currentUserSessionResult !is CustomResult.Success) {
            val error = (currentUserSessionResult as? CustomResult.Failure)?.error
                ?: Exception("User not authenticated or session could not be retrieved.")
            emit(CustomResult.Failure(error))
            return@flow
        }

        // 2. Get DMWrappers for the current user (simplified approach)
        Log.d("GetUserDmWrappersUseCase", "Start collecting dmWrapperRepository.observeAll()")
        dmWrapperRepository.observeAll().collect { dmWrappersResult ->
            Log.d("GetUserDmWrappersUseCase", "dmWrapperRepository.observeAll() emitted: $dmWrappersResult")
            when (dmWrappersResult) {
                is CustomResult.Loading -> {
                    // Initial loading already emitted. Can emit specific loading if needed.
                }

                is CustomResult.Success -> {
                    val wrappers = dmWrappersResult.data
                    Log.d("GetUserDmWrappersUseCase", "Successfully retrieved ${wrappers.size} DM wrappers")
                    emit(CustomResult.Success(wrappers))
                }

                is CustomResult.Failure -> {
                    // 권한 에러인 경우 특별 처리: 빈 리스트 반환
                    val isPermissionError = dmWrappersResult.error is FirebaseFirestoreException &&
                            (dmWrappersResult.error as FirebaseFirestoreException).code == FirebaseFirestoreException.Code.PERMISSION_DENIED

                    if (isPermissionError) {
                        Log.w("GetUserDmWrappersUseCase", "Permission denied for DM wrappers - returning empty list (user likely logged out)")
                        emit(CustomResult.Success(emptyList()))
                    } else {
                        emit(
                            CustomResult.Failure(
                                Exception(
                                    "Failed to get DM wrappers.",
                                    dmWrappersResult.error
                                )
                            )
                        )
                    }
                }

                is CustomResult.Initial -> { /* Optional: Handle Initial state from DMWrapper stream */
                }

                is CustomResult.Progress -> { /* Optional: Handle Progress state from DMWrapper stream */
                }
            }
        }
    }.catch { exception ->
        // 전체 Flow에서 발생하는 예외를 catch하여 graceful하게 처리
        Log.e("GetUserDmWrappersUseCase", "Unexpected error in flow", exception)
        
        // 권한 에러인 경우 빈 리스트 반환
        val isPermissionError = exception is FirebaseFirestoreException &&
                exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        
        if (isPermissionError) {
            Log.w("GetUserDmWrappersUseCase", "Permission denied - returning empty list (user likely logged out)")
            emit(CustomResult.Success(emptyList()))
        } else {
            emit(CustomResult.Failure(Exception("Unexpected error occurred while loading DM wrappers", exception)))
        }
    }
}