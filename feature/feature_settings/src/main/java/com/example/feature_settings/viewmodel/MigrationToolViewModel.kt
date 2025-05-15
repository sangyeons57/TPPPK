package com.example.feature_settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.migration.ChannelMigrationTool
import com.example.feature_settings.ui.MigrationStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 채널 마이그레이션 도구 ViewModel
 * 마이그레이션 작업을 관리하고 UI 상태를 제공합니다.
 */
@HiltViewModel
class MigrationToolViewModel @Inject constructor(
    private val migrationTool: ChannelMigrationTool
) : ViewModel() {
    
    // UI 상태
    private val _uiState = MutableStateFlow(MigrationToolUiState())
    val uiState: StateFlow<MigrationToolUiState> = _uiState
    
    /**
     * 프로젝트 ID 입력값 업데이트
     */
    fun updateProjectIdInput(projectId: String) {
        _uiState.update { it.copy(projectIdInput = projectId) }
    }
    
    /**
     * DM 마이그레이션 시작
     */
    fun startDmMigration() {
        if (_uiState.value.isDmMigrationRunning) return
        
        _uiState.update { it.copy(isDmMigrationRunning = true) }
        
        viewModelScope.launch {
            migrationTool.migrateDmConversations()
                .onSuccess { result ->
                    _uiState.update { state ->
                        state.copy(
                            isDmMigrationRunning = false,
                            dmMigrationStats = MigrationStats(
                                channelsCreated = result.channelsCreated,
                                messagesTransferred = result.messagesTransferred,
                                failureCount = result.failureCount,
                                errors = result.errors
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isDmMigrationRunning = false,
                            dmMigrationStats = MigrationStats(
                                channelsCreated = 0,
                                messagesTransferred = 0,
                                failureCount = 1,
                                errors = listOf(error.message ?: "Unknown error occurred")
                            )
                        )
                    }
                }
        }
    }
    
    /**
     * 프로젝트 채널 마이그레이션 시작
     */
    fun startProjectMigration() {
        val projectId = _uiState.value.projectIdInput.trim()
        if (projectId.isEmpty() || _uiState.value.isProjectMigrationRunning) return
        
        _uiState.update { it.copy(isProjectMigrationRunning = true) }
        
        viewModelScope.launch {
            migrationTool.migrateProjectChannels(projectId)
                .onSuccess { result ->
                    _uiState.update { state ->
                        state.copy(
                            isProjectMigrationRunning = false,
                            projectMigrationStats = MigrationStats(
                                channelsCreated = result.channelsCreated,
                                messagesTransferred = result.messagesTransferred,
                                failureCount = result.failureCount,
                                errors = result.errors
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isProjectMigrationRunning = false,
                            projectMigrationStats = MigrationStats(
                                channelsCreated = 0,
                                messagesTransferred = 0,
                                failureCount = 1,
                                errors = listOf(error.message ?: "Unknown error occurred")
                            )
                        )
                    }
                }
        }
    }
}

/**
 * 마이그레이션 도구 UI 상태
 */
data class MigrationToolUiState(
    val isDmMigrationRunning: Boolean = false,
    val isProjectMigrationRunning: Boolean = false,
    val projectIdInput: String = "",
    val dmMigrationStats: MigrationStats? = null,
    val projectMigrationStats: MigrationStats? = null
) 