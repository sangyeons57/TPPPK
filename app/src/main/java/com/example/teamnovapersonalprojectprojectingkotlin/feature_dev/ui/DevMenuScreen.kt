package com.example.teamnovapersonalprojectprojectingkotlin.feature_dev.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.teamnovapersonalprojectprojectingkotlin.navigation.* // 모든 Destination import
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import java.time.LocalDate // Calendar24Hour, AddSchedule 임시 인자용

/**
 * DevMenuScreen: 개발 및 테스트 목적으로 각 화면으로 이동하는 버튼 제공
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevMenuScreen(
    navController: NavHostController, // 네비게이션 제어
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("개발 메뉴 (임시)") },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                        }
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

            // --- 인증 관련 ---
            Text("--- 인증 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "스플래시 (Splash)") { navController.navigate(Splash.route)} // Splash는 시작점이지만 테스트용
            DevMenuButton(text = "로그인 (Login)") { navController.navigate(Login.route) }
            DevMenuButton(text = "회원가입 (SignUp)") { navController.navigate(SignUp.route) }
            DevMenuButton(text = "비밀번호 찾기 (FindPassword)") { navController.navigate(FindPassword.route) }

            // --- 메인 화면 ---
            Text("--- 메인 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "메인 (Main - 하단탭)") { navController.navigate(Main.route) }

            // --- 프로젝트 관련 ---
            Text("--- 프로젝트 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "프로젝트 생성 (AddProject)") { navController.navigate(AddProject.route) }
            DevMenuButton(text = "프로젝트 이름 설정 (SetProjectName)") { navController.navigate(SetProjectName.route) }
            DevMenuButton(text = "프로젝트 참여 (JoinProject)") { navController.navigate(JoinProject.route) }
            DevMenuButton(text = "프로젝트 설정 (ProjectSetting - 임시ID)") { navController.navigate(ProjectSetting.createRoute("temp_project_1")) }
            DevMenuButton(text = "카테고리 생성 (CreateCategory - 임시ID)") { navController.navigate(CreateCategory.createRoute("temp_project_1")) }
            DevMenuButton(text = "채널 생성 (CreateChannel - 임시ID)") { navController.navigate(CreateChannel.createRoute("temp_project_1", "temp_category_1")) }
            DevMenuButton(text = "카테고리 편집 (EditCategory - 임시ID)") { navController.navigate(EditCategory.createRoute("temp_project_1", "temp_category_1")) }
            DevMenuButton(text = "채널 편집 (EditChannel - 임시ID)") { navController.navigate(EditChannel.createRoute("temp_project_1", "temp_category_1", "temp_channel_1")) }

            // --- 멤버/역할 관련 ---
            Text("--- 멤버/역할 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "멤버 목록 (MemberList - 임시ID)") { navController.navigate(MemberList.createRoute("temp_project_1")) }
            DevMenuButton(text = "멤버 편집 (EditMember - 임시ID)") { navController.navigate(EditMember.createRoute("temp_project_1", "temp_user_1")) }
            DevMenuButton(text = "역할 목록 (RoleList - 임시ID)") { navController.navigate(RoleList.createRoute("temp_project_1")) }
            DevMenuButton(text = "역할 추가 (EditRole - 임시ID, 생성모드)") { navController.navigate(EditRole.createAddRoute("temp_project_1")) }
            DevMenuButton(text = "역할 편집 (EditRole - 임시ID, 수정모드)") { navController.navigate(EditRole.createEditRoute("temp_project_1", "temp_role_1")) }

            // --- 친구 관련 ---
            Text("--- 친구 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "친구 목록 (Friends)") { navController.navigate(Friends.route) }
            DevMenuButton(text = "친구 요청 수락 (AcceptFriends)") { navController.navigate(AcceptFriends.route) }

            // --- 설정 관련 ---
            Text("--- 설정 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "프로필 편집 (EditProfile)") { navController.navigate(EditProfile.route) }
            DevMenuButton(text = "비밀번호 변경 (ChangePassword)") { navController.navigate(ChangePassword.route) }
            // DevMenuButton(text = "개인 설정 (PersonalSetting)") { /* TODO: Navigate */ }

            // --- 채팅 관련 ---
            Text("--- 채팅 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "채팅 (Chat - 임시 ID)") { navController.navigate(Chat.createRoute("temp_channel_123")) }

            // --- 캘린더/스케줄 관련 ---
            Text("--- 캘린더/스케줄 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "24시간 캘린더 (Calendar24Hour - 오늘)") {
                val today = LocalDate.now()
                navController.navigate(Calendar24Hour.createRoute(today.year, today.monthValue, today.dayOfMonth))
            }
            DevMenuButton(text = "일정 추가 (AddSchedule - 오늘)") {
                val today = LocalDate.now()
                navController.navigate(AddSchedule.createRoute(today.year, today.monthValue, today.dayOfMonth))
            }
            DevMenuButton(text = "일정 상세 (ScheduleDetail - 임시ID)") { navController.navigate(ScheduleDetail.createRoute("temp_schedule_456")) }

            // --- 검색 ---
            Text("--- 검색 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "검색 (Search)") { navController.navigate(Search.route) }

        }
    }
}

/**
 * 개발 메뉴 화면용 버튼 Composable
 */
@Composable
private fun DevMenuButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(text = text, textAlign = TextAlign.Center)
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun DevMenuScreenPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        DevMenuScreen(navController = rememberNavController())
    }
}