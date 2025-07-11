package com.example.feature_edit_member.ui

// Removed direct Coil imports
// ViewModel 및 관련 요소 Import
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.components.user.UserProfileImage
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.base.Member
import com.example.domain.model.base.Role
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.role.RoleIsDefault
import com.example.feature_edit_member.viewmodel.EditMemberEvent
import com.example.feature_edit_member.viewmodel.EditMemberViewModel
import com.example.feature_edit_member.viewmodel.RoleSelectionItem
import kotlinx.coroutines.flow.collectLatest


/**
 * EditMemberScreen: 프로젝트 멤버의 역할을 수정하는 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMemberScreen(
    navigationManger: NavigationManger,
    modifier: Modifier = Modifier,
    viewModel: EditMemberViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditMemberEvent.NavigateBack -> navigationManger.navigateBack()
                is EditMemberEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // 저장 성공 시 뒤로가기 (LaunchedEffect 키를 uiState.saveSuccess로)
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            navigationManger.navigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("멤버 역할 편집") },
                navigationIcon = {
                    IconButton(onClick = { navigationManger.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        // 로딩 및 에러 상태 처리
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("오류: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            }
            uiState.memberInfo == null -> { // 멤버 정보 로드 실패 (이론상 error에서 걸러짐)
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("멤버 정보를 불러올 수 없습니다.")
                }
            }
            else -> {
                // 멤버 정보 및 역할 수정 UI 표시
                EditMemberContent(
                    modifier = Modifier.padding(paddingValues),
                    memberInfo = uiState.memberInfo!!, // Null 체크 완료
                    userInfo = uiState.userInfo, // 사용자 정보 (이름, 이메일 등)
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
    memberInfo: Member, // Domain 모델 직접 사용 (표시용)
    userInfo: com.example.domain.model.base.User?, // 사용자 정보 (이름, 이메일 등)
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
            // Temporary placeholders:
            UserProfileImage(
                userId = memberInfo.id.value, // Member ID used as User ID
                contentDescription = "프로필 이미지",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = userInfo?.name?.value ?: "알 수 없는 사용자", // Display actual user name or fallback
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                userInfo?.email?.let { email ->
                    Text(
                        text = email.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
    // Preview용 Role 객체 생성
    val previewRole1 = Role.create(name = Name("관리자"), isDefault = RoleIsDefault.NON_DEFAULT)
    val previewRole2 = Role.create(name = Name("팀원"), isDefault = RoleIsDefault.DEFAULT)
    // Note: Member constructor and EditMemberContent's usage of memberInfo.userName/profileImageUrl is problematic
    // as per current Member domain model. This is expected to be fixed in a later step.
    // For now, constructing Member according to its domain model using role IDs.
    val previewMember =
        Member.create(id = DocumentId.from(UserId("u1")), roleIds = listOf(previewRole1.id, previewRole2.id))
    
    val previewUser = com.example.domain.model.base.User.create(
        id = DocumentId.from(UserId("u1")),
        email = com.example.domain.model.vo.user.UserEmail("test@example.com"),
        name = com.example.domain.model.vo.user.UserName("테스트 사용자"),
        consentTimeStamp = java.time.Instant.now()
    )

    val previewRoles = listOf(
        RoleSelectionItem("r1", "관리자", true),
        RoleSelectionItem("r2", "팀원", true),
        RoleSelectionItem("r3", "뷰어", false)
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            EditMemberContent(
                memberInfo = previewMember,
                userInfo = previewUser,
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
    // Preview용 Role 객체 생성
    val previewRole1 = Role.create(name = Name("관리자"), isDefault = RoleIsDefault.NON_DEFAULT)
    val previewRole2 = Role.create(name = Name("팀원"), isDefault = RoleIsDefault.DEFAULT)
    // Note: Member constructor and EditMemberContent's usage of memberInfo.userName/profileImageUrl is problematic.
    // Constructing Member according to its domain model using role IDs.
    val previewMember =
        Member.create(id = DocumentId.from(UserId("u1")), roleIds = listOf(previewRole1.id, previewRole2.id))
    
    val previewUser = com.example.domain.model.base.User.create(
        id = DocumentId.from(UserId("u1")),
        email = com.example.domain.model.vo.user.UserEmail("test@example.com"),
        name = com.example.domain.model.vo.user.UserName("테스트 사용자"),
        consentTimeStamp = java.time.Instant.now()
    )

    val previewRoles = listOf(
        RoleSelectionItem("r1", "관리자", true),
        RoleSelectionItem("r2", "팀원", true),
        RoleSelectionItem("r3", "뷰어", false)
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            EditMemberContent(
                memberInfo = previewMember,
                userInfo = previewUser,
                availableRoles = previewRoles,
                onRoleSelectionChanged = { _, _ -> },
                onSaveClick = {},
                isSaving = true // 저장 중 상태
            )
        }
    }
}