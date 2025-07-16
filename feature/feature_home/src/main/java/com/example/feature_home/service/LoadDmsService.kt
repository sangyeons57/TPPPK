package com.example.feature_home.viewmodel.service

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.UserId
import com.example.domain.provider.dm.DMUseCases
import com.example.feature_home.model.DmUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DM 데이터 로딩을 담당하는 Service
 * Domain UseCase들을 조합하여 UI에 특화된 DM 데이터를 제공합니다.
 */
class LoadDmsService(
    private val dmUseCases: DMUseCases
) {
    
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
            // DM 데이터 로딩
            emitAll(
                dmUseCases.getUserDmWrappersUseCase().map { result ->
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