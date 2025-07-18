package com.example.feature_home.viewmodel.service

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.UserId
import com.example.domain.provider.dm.DMUseCaseProvider
import com.example.domain.provider.dm.DMUseCases
import com.example.domain.provider.user.UserUseCaseProvider
import com.example.domain.provider.user.UserUseCases
import com.example.feature_home.model.DmUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * DM 데이터 로딩을 담당하는 Service
 * Domain UseCase들을 조합하여 UI에 특화된 DM 데이터를 제공합니다.
 */
class LoadDmsService @Inject constructor(
    private val dmUseCaseProvider: DMUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider
) {
    
    private val userUseCases: UserUseCases = userUseCaseProvider.createForUser()
    private lateinit var dmUseCases: DMUseCases
    
    /**
     * 특정 사용자를 위한 DM UseCase 초기화
     */
    fun initializeForUser(userId: UserId) {
        dmUseCases = dmUseCaseProvider.createForUser(userId)
    }
    
    /**
     * DMWrapper 객체를 UI에 최적화된 DmUiModel로 변환
     */
    private fun toDmUiModel(dmWrapper: DMWrapper): DmUiModel {
        return DmUiModel(
            channelId = dmWrapper.id,
            partnerName = dmWrapper.otherUserName,
            partnerProfileImageUrl = dmWrapper.otherUserImageUrl,
        )
    }
    
    /**
     * 사용자의 DM 목록을 UI에 최적화된 형태로 스트림 제공
     */
    fun getUserDmsStream(): Flow<CustomResult<List<DmUiModel>, Exception>> = flow {
        Log.d("LoadDmsService", "Starting to load DMs")
        
        try {
            // 현재 사용자 확인
            val currentUserResult = userUseCases.getCurrentUserStreamUseCase().first()
            when (currentUserResult) {
                is CustomResult.Failure -> {
                    Log.w("LoadDmsService", "User not authenticated - clearing DMs")
                    emit(CustomResult.Success(emptyList()))
                    return@flow
                }
                
                is CustomResult.Success -> {
                    val currentUser = currentUserResult.data
                    if (currentUser.id.value.isEmpty()) {
                        Log.w("LoadDmsService", "Current user ID is empty - clearing DMs")
                        emit(CustomResult.Success(emptyList()))
                        return@flow
                    }
                    
                    if (!::dmUseCases.isInitialized) {
                        Log.w("LoadDmsService", "DM UseCases not initialized")
                        emit(CustomResult.Failure(IllegalStateException("DM UseCases not initialized")))
                        return@flow
                    }
                    
                    // DM 데이터 로딩
                    emitAll(
                        dmUseCases.getUserDmWrappersUseCase()
                            .map { result ->
                                when (result) {
                                    is CustomResult.Loading -> {
                                        Log.d("LoadDmsService", "Loading DMs...")
                                        CustomResult.Loading
                                    }

                                    is CustomResult.Success -> {
                                        Log.d("LoadDmsService", "Successfully fetched DM wrappers: ${result.data.size}")
                                        val dmUiModels = result.data.map { dmWrapper ->
                                            toDmUiModel(dmWrapper)
                                        }
                                        CustomResult.Success(dmUiModels)
                                    }

                                    is CustomResult.Failure -> {
                                        val errorMessage = result.error.message
                                        val isPermissionError = errorMessage?.contains("permission", ignoreCase = true) == true ||
                                                errorMessage?.contains("PERMISSION_DENIED", ignoreCase = true) == true

                                        if (isPermissionError) {
                                            Log.w("LoadDmsService", "Permission error - user likely logged out")
                                            CustomResult.Success(emptyList())
                                        } else {
                                            Log.e("LoadDmsService", "Failed to load DMs", result.error)
                                            CustomResult.Failure(result.error)
                                        }
                                    }

                                    is CustomResult.Initial -> {
                                        Log.d("LoadDmsService", "Initial state")
                                        CustomResult.Loading
                                    }

                                    is CustomResult.Progress -> {
                                        Log.d("LoadDmsService", "Progress: ${result.progress}%")
                                        CustomResult.Loading
                                    }
                                }
                            }
                        )
                }
                
                else -> {
                    Log.d("LoadDmsService", "User authentication in progress")
                    emit(CustomResult.Loading)
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                Log.d("LoadDmsService", "Load DMs job was cancelled")
            } else {
                Log.e("LoadDmsService", "Unexpected error in loadDms", e)
                emit(CustomResult.Failure(e))
            }
        }
    }
}