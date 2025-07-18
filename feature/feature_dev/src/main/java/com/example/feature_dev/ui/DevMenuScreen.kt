package com.example.feature_dev.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.core_navigation.core.AcceptFriendsRoute
import com.example.core_navigation.core.AddRoleRoute
import com.example.core_navigation.core.ChangePasswordRoute
import com.example.core_navigation.core.CreateCategoryRoute
import com.example.core_navigation.core.CreateChannelRoute
import com.example.core_navigation.core.EditCategoryRoute
import com.example.core_navigation.core.EditChannelRoute
import com.example.core_navigation.core.EditMemberRoute
import com.example.core_navigation.core.EditRoleRoute
import com.example.core_navigation.core.FindPasswordRoute
import com.example.core_navigation.core.GlobalSearchRoute
import com.example.core_navigation.core.LoginRoute
import com.example.core_navigation.core.MemberListRoute
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.core.RoleListRoute
import com.example.core_navigation.core.SetProjectNameRoute
import com.example.core_navigation.core.SignUpRoute
import com.example.core_navigation.core.SplashRoute
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_dev.viewmodel.DevMenuViewModel
import java.time.LocalDate

/**
 * DevMenuScreen: 개발 및 테스트 목적으로 각 화면으로 이동하는 버튼 제공
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevMenuScreen(
    navigationManger: NavigationManger,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    viewModel: DevMenuViewModel = hiltViewModel()
) {
    rememberCoroutineScope()
    val isLoading by viewModel.isLoading.collectAsState()
    val cacheClearResult by viewModel.cacheClearResult.collectAsState()
    val isCacheClearing by viewModel.isCacheClearing.collectAsState()
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("개발 메뉴 (임시)") },
                navigationIcon = {
                    // showBackButton이 true일 때만 뒤로가기 버튼 표시
                    if (showBackButton) {
                        DebouncedBackButton(onClick = { navigationManger.navigateBack() })
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("화면 이동 버튼 목록", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))

            // --- 각 버튼의 onClick에서 NavigationManager의 해당 메서드 호출 ---
            Text("--- 인증 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "스플래시 (Splash)") { navigationManger.navigateTo(SplashRoute) }
            DevMenuButton(text = "로그인 (Login)") { navigationManger.navigateTo(LoginRoute) }
            DevMenuButton(text = "회원가입 (SignUp)") { navigationManger.navigateTo(SignUpRoute) }
            DevMenuButton(text = "비밀번호 찾기 (FindPassword)") {
                navigationManger.navigateTo(
                    FindPasswordRoute
                )
            }

            Text("--- 메인 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "메인 (Main - 하단탭)") { navigationManger.navigateToMain() }

            Text("--- 프로젝트 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "프로젝트 생성 (AddProject)") { navigationManger.navigateToAddProject() }
            DevMenuButton(text = "프로젝트 이름 설정 (SetProjectName)") {
                navigationManger.navigateTo(
                    SetProjectNameRoute
                )
            }
            DevMenuButton(text = "프로젝트 참여 (JoinProject)") { navigationManger.navigateToJoinProject() }
            DevMenuButton(text = "프로젝트 설정 (ProjectSetting - 임시ID)") {
                navigationManger.navigateToProjectSettings(
                    "temp_project_1"
                )
            }
            DevMenuButton(text = "카테고리 생성 (CreateCategory - 임시ID)") {
                navigationManger.navigateTo(
                    CreateCategoryRoute("temp_project_1")
                )
            }
            DevMenuButton(text = "채널 생성 (CreateChannel - 임시ID)") {
                navigationManger.navigateTo(
                    CreateChannelRoute("temp_project_1", "temp_category_1")
                )
            }
            DevMenuButton(text = "카테고리 편집 (EditCategory - 임시ID)") {
                navigationManger.navigateTo(
                    EditCategoryRoute("temp_project_1", "temp_category_1")
                )
            }
            DevMenuButton(text = "채널 편집 (EditChannel - 임시ID)") {
                navigationManger.navigateTo(
                    EditChannelRoute("temp_project_1", "temp_channel_1")
                )
            }

            Text("--- 멤버/역할 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "멤버 목록 (MemberList - 임시ID)") {
                navigationManger.navigateTo(
                    MemberListRoute("temp_project_1")
                )
            }
            DevMenuButton(text = "멤버 편집 (EditMember - 임시ID)") {
                navigationManger.navigateTo(
                    EditMemberRoute("temp_project_1", "temp_user_1")
                )
            }
            DevMenuButton(text = "역할 목록 (RoleList - 임시ID)") {
                navigationManger.navigateTo(
                    RoleListRoute("temp_project_1")
                )
            }
            DevMenuButton(text = "역할 추가 (EditRole - 임시ID, 생성모드)") {
                navigationManger.navigateTo(
                    AddRoleRoute("temp_project_1")
                )
            }
            DevMenuButton(text = "역할 편집 (EditRole - 임시ID, 수정모드)") {
                navigationManger.navigateTo(
                    EditRoleRoute("temp_project_1", "temp_role_1")
                )
            }

            Text("--- 친구 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "친구 목록 (Friends)") { navigationManger.navigateToFriends() }
            DevMenuButton(text = "친구 요청 수락 (AcceptFriends)") {
                navigationManger.navigateTo(
                    AcceptFriendsRoute
                )
            }

            Text("--- 설정 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "프로필 편집 (EditProfile)") { navigationManger.navigateToEditProfile() }
            DevMenuButton(text = "비밀번호 변경 (ChangePassword)") {
                navigationManger.navigateTo(
                    ChangePasswordRoute
                )
            }

            Text("--- 채팅 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "채팅 (DM - 임시 ID: temp_dm_channel_123)") {
                navigationManger.navigateToChat(
                    "temp_dm_channel_123"
                )
            }
            DevMenuButton(text = "채팅 (프로젝트 직속 - 임시 IDs)") { navigationManger.navigateToChat("dev_direct_channel_id") }
            DevMenuButton(text = "채팅 (프로젝트 카테고리 - 임시 IDs)") { navigationManger.navigateToChat("dev_category_channel_id") }

            Text("--- 캘린더/스케줄 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "24시간 캘린더 (Calendar24Hour - 오늘)") {
                val today = LocalDate.now()
                navigationManger.navigateToCalendar(today.year, today.monthValue, today.dayOfMonth)
            }
            DevMenuButton(text = "일정 추가 (AddSchedule - 오늘)") {
                val today = LocalDate.now()
                navigationManger.navigateToAddSchedule(
                    today.year,
                    today.monthValue,
                    today.dayOfMonth
                )
            }
            DevMenuButton(text = "일정 상세 (ScheduleDetail - 임시ID)") {
                navigationManger.navigateToScheduleDetail(
                    "temp_schedule_456"
                )
            }

            Text("--- 검색 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "검색 (Search)") { navigationManger.navigateTo(GlobalSearchRoute) }

            Text(
                "--- Firebase Functions 테스트 ---",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Hello World 테스트 버튼
            if (isLoading) {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("호출 중...")
                }
            }

            /* ----------------------------------------- */
            /* Firestore 캐시 삭제 테스트                   */
            /* ----------------------------------------- */
            Text(
                "--- Firestore 캐시 ---",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp)
            )

            if (isCacheClearing) {
                Button(
                    onClick = viewModel::clearFirestoreCache,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("캐시 삭제 중...")
                }
            } else {
                DevMenuButton(text = "Firestore 캐시 삭제") {
                    viewModel.clearFirestoreCache()
                }
            }

            // 캐시 삭제 결과 표시
            if (cacheClearResult.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (cacheClearResult.startsWith("성공"))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "결과:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = cacheClearResult,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.clearResult() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("지우기")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp)) // 하단 여백
        }
    }
}

/**
 * DevMenuButton: 개발 메뉴에서 사용하는 버튼 (Stateless)
 */
@Composable
fun DevMenuButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center
        )
    }
}

// --- Preview ---
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun DevMenuScreenPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        // NavController 관련 내용은 제거하고 프리뷰용 UI만 표시
        Scaffold(
            topBar = { TopAppBar(title = { Text("개발 메뉴 (임시)") }) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("화면 이동 버튼 목록", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("로그인 (Login)")
                }
                
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("회원가입 (SignUp)")
                }
                
                // ... 기타 버튼들 ...
            }
        }
    }
}