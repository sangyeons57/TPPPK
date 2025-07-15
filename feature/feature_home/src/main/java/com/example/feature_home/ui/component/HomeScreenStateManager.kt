package com.example.feature_home.ui.component

import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.domain.model.vo.DocumentId
import com.example.feature_home.viewmodel.HomeViewModel
import com.example.feature_home.viewmodel.TopSection

// 상태 저장 키 상수
private object HomeScreenStateKeys {
    const val SELECTED_TOP_SECTION = "selected_top_section"
    const val SELECTED_PROJECT_ID = "selected_project_id"
    const val EXPANDED_CATEGORIES = "expanded_categories"
    const val SHOW_FLOATING_MENU = "show_floating_menu"
}

/**
 * HomeScreen의 상태 관리를 담당하는 컴포넌트
 */
@Composable
fun HomeScreenStateManager(
    viewModel: HomeViewModel,
    savedState: Bundle?,
    showFloatingMenu: Boolean,
    onShowFloatingMenuChange: (Boolean) -> Unit,
    selectedTopSection: TopSection,
    selectedProjectId: DocumentId?
) {
    // 상태 복원 (탭 전환 시)
    LaunchedEffect(savedState) {
        savedState?.let { bundle ->
            Log.d("HomeScreenStateManager", "상태 복원: $bundle")
            
            // TopSection 복원
            bundle.getString(HomeScreenStateKeys.SELECTED_TOP_SECTION)?.let { sectionName ->
                try {
                    val section = TopSection.valueOf(sectionName)
                    viewModel.onTopSectionSelect(section)
                } catch (e: IllegalArgumentException) {
                    Log.e("HomeScreenStateManager", "Invalid section name: $sectionName", e)
                }
            }
            
            // 선택된 프로젝트 ID 복원
            bundle.getString(HomeScreenStateKeys.SELECTED_PROJECT_ID)?.let { projectId ->
                if (projectId.isNotEmpty()) {
                    viewModel.onProjectClick(DocumentId(projectId))
                }
            }
            
            // 다이얼로그 상태 복원
            val floatingMenuState = bundle.getBoolean(HomeScreenStateKeys.SHOW_FLOATING_MENU, false)
            onShowFloatingMenuChange(floatingMenuState)
            
            // 확장된 카테고리 복원
            bundle.getStringArray(HomeScreenStateKeys.EXPANDED_CATEGORIES)?.let { expandedIds ->
                viewModel.restoreExpandedCategories(expandedIds.toList())
            }
        }
    }
    
    // 화면 상태 저장 (탭 전환 시)
    DisposableEffect(
        selectedTopSection,
        selectedProjectId,
        showFloatingMenu
    ) {
        onDispose {
            // 화면이 비활성화될 때 상태 저장
            val screenState = Bundle().apply {
                putString(HomeScreenStateKeys.SELECTED_TOP_SECTION, selectedTopSection.name)
                putString(
                    HomeScreenStateKeys.SELECTED_PROJECT_ID,
                    selectedProjectId?.value ?: ""
                )
                putBoolean(HomeScreenStateKeys.SHOW_FLOATING_MENU, showFloatingMenu)
            }
            
            Log.d("HomeScreenStateManager", "상태 저장: $screenState")
        }
    }

    // 앱 시작 시 DM 섹션을 기본 선택되도록 설정 (상태가 없는 경우만)
    LaunchedEffect(Unit) {
        if (savedState == null) {
            viewModel.onTopSectionSelect(TopSection.DMS)
        }
    }
}

/**
 * 다이얼로그 상태를 관리하는 데이터 클래스
 */
data class DialogStates(
    val showAddProjectElementDialog: Boolean = false,
    val showFloatingMenu: Boolean = false,
    val currentProjectIdForDialog: DocumentId? = null,
    val showEditCategoryDialog: Boolean = false,
    val showEditChannelDialog: Boolean = false,
    val editCategoryName: String = "",
    val editChannelName: String = "",
    val editCategoryProjectId: String = "",
    val editCategoryId: String = "",
    val editChannelProjectId: String = "",
    val editChannelId: String = "",
    val showReorderCategoriesDialog: Boolean = false,
    val showReorderChannelsDialog: Boolean = false,
    val reorderCategoryId: String? = null
)

/**
 * 다이얼로그 상태를 관리하는 Composable
 */
@Composable
fun rememberDialogStates(): DialogStates {
    var showAddProjectElementDialog by remember { mutableStateOf(false) }
    var showFloatingMenu by remember { mutableStateOf(false) }
    var currentProjectIdForDialog by remember { mutableStateOf<DocumentId?>(null) }
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var showEditChannelDialog by remember { mutableStateOf(false) }
    var editCategoryName by remember { mutableStateOf("") }
    var editChannelName by remember { mutableStateOf("") }
    var editCategoryProjectId by remember { mutableStateOf("") }
    var editCategoryId by remember { mutableStateOf("") }
    var editChannelProjectId by remember { mutableStateOf("") }
    var editChannelId by remember { mutableStateOf("") }
    var showReorderCategoriesDialog by remember { mutableStateOf(false) }
    var showReorderChannelsDialog by remember { mutableStateOf(false) }
    var reorderCategoryId by remember { mutableStateOf<String?>(null) }

    return DialogStates(
        showAddProjectElementDialog = showAddProjectElementDialog,
        showFloatingMenu = showFloatingMenu,
        currentProjectIdForDialog = currentProjectIdForDialog,
        showEditCategoryDialog = showEditCategoryDialog,
        showEditChannelDialog = showEditChannelDialog,
        editCategoryName = editCategoryName,
        editChannelName = editChannelName,
        editCategoryProjectId = editCategoryProjectId,
        editCategoryId = editCategoryId,
        editChannelProjectId = editChannelProjectId,
        editChannelId = editChannelId,
        showReorderCategoriesDialog = showReorderCategoriesDialog,
        showReorderChannelsDialog = showReorderChannelsDialog,
        reorderCategoryId = reorderCategoryId
    )
}