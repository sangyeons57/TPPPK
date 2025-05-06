package com.example.feature_dev

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import java.time.LocalDate // Calendar24Hour, AddSchedule 임시 인자용
import com.example.core_logging.SentryUtil
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.core.NavigationCommand
import kotlinx.coroutines.launch

/**
 * DevMenuScreen: 개발 및 테스트 목적으로 각 화면으로 이동하는 버튼 제공
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevMenuScreen(
    navigationManager: ComposeNavigationHandler,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("개발 메뉴 (임시)") },
                navigationIcon = {
                    // showBackButton이 true일 때만 뒤로가기 버튼 표시
                    if (showBackButton) {
                        IconButton(onClick = { navigationManager.navigateBack() }) {
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

            // --- 각 버튼의 onClick에서 NavigationManager의 해당 메서드 호출 ---
            Text("--- 인증 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "스플래시 (Splash)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Auth.SPLASH)) }
            DevMenuButton(text = "로그인 (Login)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Auth.LOGIN)) }
            DevMenuButton(text = "회원가입 (SignUp)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Auth.SIGN_UP)) }
            DevMenuButton(text = "비밀번호 찾기 (FindPassword)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Auth.FIND_PASSWORD)) }

            Text("--- 메인 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "메인 (Main - 하단탭)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Main.ROOT)) }

            Text("--- 프로젝트 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "프로젝트 생성 (AddProject)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Main.Home.ADD_PROJECT)) }
            DevMenuButton(text = "프로젝트 이름 설정 (SetProjectName)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Main.Home.SET_PROJECT_NAME)) }
            DevMenuButton(text = "프로젝트 참여 (JoinProject)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Main.Home.JOIN_PROJECT)) }
            DevMenuButton(text = "프로젝트 설정 (ProjectSetting - 임시ID)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.settings("temp_project_1"))) }
            DevMenuButton(text = "카테고리 생성 (CreateCategory - 임시ID)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.createCategory("temp_project_1"))) }
            DevMenuButton(text = "채널 생성 (CreateChannel - 임시ID)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.createChannel("temp_project_1", "temp_category_1"))) }
            DevMenuButton(text = "카테고리 편집 (EditCategory - 임시ID)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.editCategory("temp_project_1", "temp_category_1"))) }
            DevMenuButton(text = "채널 편집 (EditChannel - 임시ID)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.editChannel("temp_project_1", "temp_category_1", "temp_channel_1"))) }

            Text("--- 멤버/역할 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "멤버 목록 (MemberList - 임시ID)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.memberList("temp_project_1"))) }
            DevMenuButton(text = "멤버 편집 (EditMember - 임시ID)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.editMember("temp_project_1", "temp_user_1"))) }
            DevMenuButton(text = "역할 목록 (RoleList - 임시ID)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.roleList("temp_project_1"))) }
            DevMenuButton(text = "역할 추가 (EditRole - 임시ID, 생성모드)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.addRole("temp_project_1"))) }
            DevMenuButton(text = "역할 편집 (EditRole - 임시ID, 수정모드)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Project.editRole("temp_project_1", "temp_role_1"))) }

            Text("--- 친구 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "친구 목록 (Friends)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Friends.LIST)) }
            DevMenuButton(text = "친구 요청 수락 (AcceptFriends)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Friends.ACCEPT_REQUESTS)) }

            Text("--- 설정 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "프로필 편집 (EditProfile)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Main.Profile.EDIT_PROFILE)) }
            DevMenuButton(text = "비밀번호 변경 (ChangePassword)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Main.Profile.CHANGE_PASSWORD)) }

            Text("--- 채팅 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "채팅 (Chat - 임시 ID)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Chat.chat("temp_channel_123"))) }

            Text("--- 캘린더/스케줄 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "24시간 캘린더 (Calendar24Hour - 오늘)") {
                val today = LocalDate.now()
                navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Main.Calendar.calendar24Hour(today.year, today.monthValue, today.dayOfMonth)))
            }
            DevMenuButton(text = "일정 추가 (AddSchedule - 오늘)") {
                val today = LocalDate.now()
                navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Main.Calendar.addSchedule(today.year, today.monthValue, today.dayOfMonth)))
            }
            DevMenuButton(text = "일정 상세 (ScheduleDetail - 임시ID)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Main.Calendar.scheduleDetail("temp_schedule_456"))) }

            Text("--- 검색 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "검색 (Search)") { navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Search.ROOT)) }

            // Sentry 테스트 섹션 추가
            Text("--- Sentry 테스트 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            
            // 에러 로깅 테스트
            DevMenuButton(text = "에러 로깅 테스트") {
                try {
                    throw RuntimeException("테스트 예외")
                } catch (e: Exception) {
                    SentryUtil.captureError(e, "의도적인 테스트 예외")
                }
            }
            
            // 성능 트랜잭션 테스트
            DevMenuButton(text = "성능 트랜잭션 테스트") {
                val transaction = SentryUtil.startTransaction(
                    name = "test.transaction",
                    operation = "test.operation"
                )
                
                coroutineScope.launch {
                    // 성능 스팬 테스트
                    SentryUtil.withSpan(transaction, "test.child", "테스트 스팬") {
                        // 작업 시뮬레이션
                        Thread.sleep(500)
                    }
                    
                    // 비동기 스팬 테스트
                    SentryUtil.withAsyncSpan(transaction, "test.async", "비동기 테스트 스팬") {
                        // 비동기 작업 시뮬레이션
                        Thread.sleep(1000)
                    }
                    
                    // 트랜잭션 완료
                    transaction.finish()
                }
            }
            
            // 복합 로깅 테스트 (여러 이벤트 전송)
            DevMenuButton(text = "복합 로깅 테스트") {
                // 사용자 정보 설정
                SentryUtil.setUserInfo(
                    userId = "test_user_${System.currentTimeMillis()}",
                    email = "test@example.com",
                    username = "테스트 사용자"
                )
                
                // 태그 설정
                SentryUtil.setCustomTag("test_scenario", "complex_test")
                
                // 다양한 레벨의 로그 기록
                SentryUtil.logInfo("복합 테스트 시작")
                SentryUtil.logWarning("테스트 경고 메시지")
                SentryUtil.logError("테스트 오류 메시지")
                
                // 사용자 액션 기록
                SentryUtil.trackUserAction("button_click", "복합 테스트 버튼", "DevMenu")
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