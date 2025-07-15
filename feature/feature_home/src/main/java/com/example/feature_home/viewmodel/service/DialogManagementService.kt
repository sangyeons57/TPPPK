package com.example.feature_home.viewmodel.service

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialogBuilder
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialogItem
import com.example.domain.model.vo.DocumentId
import com.example.feature_home.model.CategoryUiModel
import com.example.feature_home.model.ChannelUiModel
import com.example.feature_home.model.DmUiModel
import com.example.feature_home.model.ProjectUiModel
/**
 * 다이얼로그 상태 관리를 담당하는 Service
 * UI에 특화된 다이얼로그 생성 및 관리 기능을 제공합니다.
 */
class DialogManagementService() {
    
    /**
     * 프로젝트 아이템 액션 시트 생성
     */
    fun createProjectItemActionSheet(project: ProjectUiModel): List<BottomSheetDialogItem> {
        Log.d("DialogManagementService", "Creating project action sheet for: ${project.name}")
        
        return BottomSheetDialogBuilder()
            .button(
                label = "프로젝트 설정",
                icon = Icons.Default.Settings,
                onClick = { 
                    Log.d("DialogManagementService", "Project settings clicked for: ${project.name}")
                    // 프로젝트 설정 클릭 처리는 ViewModel에서 처리
                }
            )
            .button(
                label = "프로젝트 나가기",
                icon = Icons.Default.Block,
                onClick = { 
                    Log.d("DialogManagementService", "Leave project clicked for: ${project.name}")
                    // 프로젝트 나가기 처리는 ViewModel에서 처리
                }
            )
            .build()
    }
    
    /**
     * 카테고리 롱프레스 액션 시트 생성
     */
    fun createCategoryLongPressActionSheet(category: CategoryUiModel): List<BottomSheetDialogItem> {
        Log.d("DialogManagementService", "Creating category action sheet for: ${category.name}")
        
        return BottomSheetDialogBuilder()
            .button(
                label = "카테고리 편집",
                icon = Icons.Default.Edit,
                onClick = { 
                    Log.d("DialogManagementService", "Edit category clicked for: ${category.name}")
                    // 카테고리 편집 처리는 ViewModel에서 처리
                }
            )
            .button(
                label = "순서 변경",
                icon = Icons.Default.SwapVert,
                onClick = { 
                    Log.d("DialogManagementService", "Reorder category clicked for: ${category.name}")
                    // 순서 변경 처리는 ViewModel에서 처리
                }
            )
            .build()
    }
    
    /**
     * 채널 롱프레스 액션 시트 생성
     */
    fun createChannelLongPressActionSheet(channel: ChannelUiModel): List<BottomSheetDialogItem> {
        Log.d("DialogManagementService", "Creating channel action sheet for: ${channel.name}")
        
        return BottomSheetDialogBuilder()
            .button(
                label = "채널 편집",
                icon = Icons.Default.Edit,
                onClick = { 
                    Log.d("DialogManagementService", "Edit channel clicked for: ${channel.name}")
                    // 채널 편집 처리는 ViewModel에서 처리
                }
            )
            .button(
                label = "순서 변경",
                icon = Icons.Default.SwapVert,
                onClick = { 
                    Log.d("DialogManagementService", "Reorder channel clicked for: ${channel.name}")
                    // 순서 변경 처리는 ViewModel에서 처리
                }
            )
            .build()
    }
    
    /**
     * DM 롱프레스 액션 시트 생성
     */
    fun createDmLongPressActionSheet(dm: DmUiModel): List<BottomSheetDialogItem> {
        Log.d("DialogManagementService", "Creating DM action sheet for: ${dm.partnerName}")
        
        return BottomSheetDialogBuilder()
            .button(
                label = "DM 차단",
                icon = Icons.Default.Block,
                onClick = { 
                    Log.d("DialogManagementService", "Block DM clicked for: ${dm.partnerName}")
                    // DM 차단 처리는 ViewModel에서 처리
                }
            )
            .build()
    }
    
    /**
     * 다이얼로그 상태를 나타내는 데이터 클래스
     */
    data class DialogState(
        val showBottomSheet: Boolean = false,
        val bottomSheetItems: List<BottomSheetDialogItem> = emptyList(),
        val showAddProjectElementDialog: Boolean = false,
        val currentProjectIdForDialog: DocumentId? = null,
        val showEditCategoryDialog: Boolean = false,
        val showEditChannelDialog: Boolean = false,
        val showReorderCategoriesDialog: Boolean = false,
        val showReorderChannelsDialog: Boolean = false,
        val editCategoryName: String = "",
        val editChannelName: String = "",
        val editCategoryProjectId: String = "",
        val editCategoryId: String = "",
        val editChannelProjectId: String = "",
        val editChannelId: String = "",
        val reorderCategoryId: String? = null
    )
    
    /**
     * 초기 다이얼로그 상태 반환
     */
    fun getInitialDialogState(): DialogState {
        return DialogState()
    }
    
    /**
     * 바텀시트 다이얼로그 표시
     */
    fun showBottomSheet(
        currentState: DialogState,
        items: List<BottomSheetDialogItem>
    ): DialogState {
        Log.d("DialogManagementService", "Showing bottom sheet with ${items.size} items")
        return currentState.copy(
            showBottomSheet = true,
            bottomSheetItems = items
        )
    }
    
    /**
     * 바텀시트 다이얼로그 닫기
     */
    fun dismissBottomSheet(currentState: DialogState): DialogState {
        Log.d("DialogManagementService", "Dismissing bottom sheet")
        return currentState.copy(
            showBottomSheet = false,
            bottomSheetItems = emptyList()
        )
    }
    
    /**
     * 프로젝트 요소 추가 다이얼로그 표시
     */
    fun showAddProjectElementDialog(
        currentState: DialogState,
        projectId: DocumentId
    ): DialogState {
        Log.d("DialogManagementService", "Showing add project element dialog for: $projectId")
        return currentState.copy(
            showAddProjectElementDialog = true,
            currentProjectIdForDialog = projectId
        )
    }
    
    /**
     * 프로젝트 요소 추가 다이얼로그 닫기
     */
    fun dismissAddProjectElementDialog(currentState: DialogState): DialogState {
        Log.d("DialogManagementService", "Dismissing add project element dialog")
        return currentState.copy(
            showAddProjectElementDialog = false,
            currentProjectIdForDialog = null
        )
    }
}