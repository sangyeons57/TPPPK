package com.example.feature_project.members.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core_common.util.DateTimeUtil
import com.example.core_navigation.core.AppNavigator
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.core_ui.R
import com.example.domain.model.ProjectMember
// ViewModel 및 관련 요소 Import
import com.example.feature_project.members.viewmodel.EditMemberEvent
import com.example.feature_project.members.viewmodel.EditMemberViewModel
import com.example.feature_project.members.viewmodel.RoleSelectionItem // ★ UI 모델 Import
import kotlinx.coroutines.flow.collectLatest


/**
 * EditMemberScreen: 프로젝트 멤버의 역할을 수정하는 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMemberScreen(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: EditMemberViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditMemberEvent.NavigateBack -> appNavigator.navigateBack()
                is EditMemberEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // 저장 성공 시 뒤로가기 (LaunchedEffect 키를 uiState.saveSuccess로)
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            appNavigator.navigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("멤버 역할 편집") },
                navigationIcon = {
                    IconButton(onClick = { appNavigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        // 로딩 및 에러 상태 처리
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("오류: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            }
            uiState.memberInfo == null -> { // 멤버 정보 로드 실패 (이론상 error에서 걸러짐)
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("멤버 정보를 불러올 수 없습니다.")
                }
            }
            else -> {
                // 멤버 정보 및 역할 수정 UI 표시
                EditMemberContent(
                    modifier = Modifier.padding(paddingValues),
                    memberInfo = uiState.memberInfo!!, // Null 체크 완료
                    availableRoles = uiState.availableRoles, // ★ UI 모델 리스트 전달
                    onRoleSelectionChanged = viewModel::onRoleSelectionChanged,
                    onSaveClick = viewModel::saveMemberRoles,
                    isSaving = uiState.isSaving
                )
            }
        }
    }
}

/**
 * EditMemberContent: 멤버 정보 및 역할 선택 UI (Stateless)
 */
@Composable
fun EditMemberContent(
    modifier: Modifier = Modifier,
    memberInfo: ProjectMember, // Domain 모델 직접 사용 (표시용)
    availableRoles: List<RoleSelectionItem>, // ★ UI 모델 사용 (선택용)
    onRoleSelectionChanged: (String, Boolean) -> Unit, // roleId, isSelected 전달
    onSaveClick: () -> Unit,
    isSaving: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 멤버 정보 표시
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(memberInfo.profileImageUrl ?: R.drawable.ic_account_circle_24)
                    .error(R.drawable.ic_account_circle_24)
                    .placeholder(R.drawable.ic_account_circle_24)
                    .build(),
                contentDescription = "${memberInfo.userName} 프로필",
                modifier = Modifier.size(64.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = memberInfo.userName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 역할 할당 섹션
        Text(
            "역할 할당",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 선택 가능한 역할 목록 표시
        if (availableRoles.isEmpty()) {
            Text("설정된 역할이 없습니다.", style = MaterialTheme.typography.bodyMedium)
        } else {
            availableRoles.forEach { roleItem ->
                RoleCheckboxRow(
                    roleItem = roleItem, // ★ UI 모델 전달
                    onCheckedChange = { isChecked ->
                        onRoleSelectionChanged(roleItem.id, isChecked) // ★ roleId 전달
                    },
                    enabled = !isSaving // 저장 중 아닐 때만 활성화
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // 버튼 하단 배치

        // 저장 버튼
        Button(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving // 저장 중 아닐 때 활성화
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("변경사항 저장")
            }
        }
    }
}

/**
 * RoleCheckboxRow: 개별 역할 선택 체크박스 행 (Stateless)
 */
@Composable
fun RoleCheckboxRow(
    roleItem: RoleSelectionItem, // ★ UI 모델 사용
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!roleItem.isSelected) } // 행 클릭으로 토글
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = roleItem.isSelected, // ★ UI 모델 상태 사용
            onCheckedChange = { onCheckedChange(it) }, // 체크박스 직접 클릭
            enabled = enabled
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = roleItem.name, // ★ UI 모델 이름 사용
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.outline
        )
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun EditMemberContentPreview() {
    val previewMember = ProjectMember("u1", "테스트 멤버", null, listOf("관리자", "팀원"), DateTimeUtil.nowInstant())
    val previewRoles = listOf(
        RoleSelectionItem("r1", "관리자", true),
        RoleSelectionItem("r2", "팀원", true),
        RoleSelectionItem("r3", "뷰어", false)
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            EditMemberContent(
                memberInfo = previewMember,
                availableRoles = previewRoles,
                onRoleSelectionChanged = { _, _ -> },
                onSaveClick = {},
                isSaving = false
            )
        }
    }
}

@Preview(showBackground = true, name="Edit Member Saving")
@Composable
private fun EditMemberContentSavingPreview() {
    val previewMember = ProjectMember("u1", "테스트 멤버", null, listOf("관리자", "팀원"), DateTimeUtil.nowInstant())
    val previewRoles = listOf(
        RoleSelectionItem("r1", "관리자", true),
        RoleSelectionItem("r2", "팀원", true),
        RoleSelectionItem("r3", "뷰어", false)
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            EditMemberContent(
                memberInfo = previewMember,
                availableRoles = previewRoles,
                onRoleSelectionChanged = { _, _ -> },
                onSaveClick = {},
                isSaving = true // 저장 중 상태
            )
        }
    }
}