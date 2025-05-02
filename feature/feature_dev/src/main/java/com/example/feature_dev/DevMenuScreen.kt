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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import java.time.LocalDate // Calendar24Hour, AddSchedule 임시 인자용
import com.example.core_logging.SentryUtil
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * DevMenuScreen: 개발 및 테스트 목적으로 각 화면으로 이동하는 버튼 제공 (람다 파라미터 사용)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevMenuScreen(
    // navController 파라미터 제거
    modifier: Modifier = Modifier,
    // --- 네비게이션 액션 람다 파라미터 추가 ---
    onNavigateBack: (() -> Unit)? = null, // 뒤로가기 (필요 시)
    onNavigateToSplash: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToFindPassword: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToAddProject: () -> Unit,
    onNavigateToSetProjectName: () -> Unit,
    onNavigateToJoinProject: () -> Unit,
    onNavigateToProjectSetting: (projectId: String) -> Unit,
    onNavigateToCreateCategory: (projectId: String) -> Unit,
    onNavigateToCreateChannel: (projectId: String, categoryId: String) -> Unit,
    onNavigateToEditCategory: (projectId: String, categoryId: String) -> Unit,
    onNavigateToEditChannel: (projectId: String, categoryId: String, channelId: String) -> Unit,
    onNavigateToMemberList: (projectId: String) -> Unit,
    onNavigateToEditMember: (projectId: String, userId: String) -> Unit,
    onNavigateToRoleList: (projectId: String) -> Unit,
    onNavigateToAddRole: (projectId: String) -> Unit,
    onNavigateToEditRole: (projectId: String, roleId: String) -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToAcceptFriends: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToChat: (channelId: String) -> Unit,
    onNavigateToCalendar24Hour: (year: Int, month: Int, day: Int) -> Unit,
    onNavigateToAddSchedule: (year: Int, month: Int, day: Int) -> Unit,
    onNavigateToScheduleDetail: (scheduleId: String) -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("개발 메뉴 (임시)") },
                navigationIcon = {
                    // onNavigateBack 람다가 null이 아닐 때만 뒤로가기 버튼 표시
                    onNavigateBack?.let {
                        IconButton(onClick = it) {
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

            // --- 각 버튼의 onClick에서 navController.navigate 대신 전달받은 람다 호출 ---
            Text("--- 인증 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "스플래시 (Splash)") { onNavigateToSplash() }
            DevMenuButton(text = "로그인 (Login)") { onNavigateToLogin() }
            DevMenuButton(text = "회원가입 (SignUp)") { onNavigateToSignUp() }
            DevMenuButton(text = "비밀번호 찾기 (FindPassword)") { onNavigateToFindPassword() }

            Text("--- 메인 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "메인 (Main - 하단탭)") { onNavigateToMain() }

            Text("--- 프로젝트 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "프로젝트 생성 (AddProject)") { onNavigateToAddProject() }
            DevMenuButton(text = "프로젝트 이름 설정 (SetProjectName)") { onNavigateToSetProjectName() }
            DevMenuButton(text = "프로젝트 참여 (JoinProject)") { onNavigateToJoinProject() }
            DevMenuButton(text = "프로젝트 설정 (ProjectSetting - 임시ID)") { onNavigateToProjectSetting("temp_project_1") }
            DevMenuButton(text = "카테고리 생성 (CreateCategory - 임시ID)") { onNavigateToCreateCategory("temp_project_1") }
            DevMenuButton(text = "채널 생성 (CreateChannel - 임시ID)") { onNavigateToCreateChannel("temp_project_1", "temp_category_1") }
            DevMenuButton(text = "카테고리 편집 (EditCategory - 임시ID)") { onNavigateToEditCategory("temp_project_1", "temp_category_1") }
            DevMenuButton(text = "채널 편집 (EditChannel - 임시ID)") { onNavigateToEditChannel("temp_project_1", "temp_category_1", "temp_channel_1") }

            Text("--- 멤버/역할 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "멤버 목록 (MemberList - 임시ID)") { onNavigateToMemberList("temp_project_1") }
            DevMenuButton(text = "멤버 편집 (EditMember - 임시ID)") { onNavigateToEditMember("temp_project_1", "temp_user_1") }
            DevMenuButton(text = "역할 목록 (RoleList - 임시ID)") { onNavigateToRoleList("temp_project_1") }
            DevMenuButton(text = "역할 추가 (EditRole - 임시ID, 생성모드)") { onNavigateToAddRole("temp_project_1") }
            DevMenuButton(text = "역할 편집 (EditRole - 임시ID, 수정모드)") { onNavigateToEditRole("temp_project_1", "temp_role_1") }

            Text("--- 친구 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "친구 목록 (Friends)") { onNavigateToFriends() }
            DevMenuButton(text = "친구 요청 수락 (AcceptFriends)") { onNavigateToAcceptFriends() }

            Text("--- 설정 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "프로필 편집 (EditProfile)") { onNavigateToEditProfile() }
            DevMenuButton(text = "비밀번호 변경 (ChangePassword)") { onNavigateToChangePassword() }

            Text("--- 채팅 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "채팅 (Chat - 임시 ID)") { onNavigateToChat("temp_channel_123") }

            Text("--- 캘린더/스케줄 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "24시간 캘린더 (Calendar24Hour - 오늘)") {
                val today = LocalDate.now()
                onNavigateToCalendar24Hour(today.year, today.monthValue, today.dayOfMonth)
            }
            DevMenuButton(text = "일정 추가 (AddSchedule - 오늘)") {
                val today = LocalDate.now()
                onNavigateToAddSchedule(today.year, today.monthValue, today.dayOfMonth)
            }
            DevMenuButton(text = "일정 상세 (ScheduleDetail - 임시ID)") { onNavigateToScheduleDetail("temp_schedule_456") }

            Text("--- 검색 ---", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DevMenuButton(text = "검색 (Search)") { onNavigateToSearch() }

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
                
                // 네트워크 요청 시뮬레이션
                SentryUtil.trackNetworkRequestStart("https://api.example.com/data", "GET")
                
                coroutineScope.launch {
                    delay(1500)
                    SentryUtil.trackNetworkRequestEnd("https://api.example.com/data", "GET", 200, 1500)
                }
            }
            
            // 세션 리플레이 테스트
            DevMenuButton(text = "세션 리플레이 시뮬레이션 시작") {
                SentryUtil.startSessionReplay()
            }
            
            DevMenuButton(text = "세션 리플레이 시뮬레이션 중지") {
                SentryUtil.stopSessionReplay()
            }
            
            // 사용자 피드백 테스트
            DevMenuButton(text = "사용자 피드백 테스트") {
                SentryUtil.sendUserFeedback(
                    name = "테스트 사용자",
                    email = "feedback@example.com",
                    comments = "앱이 매우 유용합니다. 테스트 피드백입니다.",
                    feedbackType = "positive"
                )
            }
            
            // 테스트 정보 표시
            Text(
                text = "Sentry 구현 정보",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            
            Text(
                text = "• 싱글톤으로 앱 시작 시 자동 초기화\n" +
                      "• 에러 추적 및 브레드크럼 기록\n" +
                      "• 화면 이동 자동 추적\n" +
                      "• 네트워크 요청 추적\n" +
                      "• 사용자 액션 및 이벤트 기록\n" +
                      "• 세션 리플레이 (제한적 지원)",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
            )
            
            // OOM 시뮬레이션
            DevMenuButton(text = "OOM 시뮬레이션 (주의: 앱 충돌)") {
                coroutineScope.launch {
                    SentryUtil.logWarning("의도적인 OOM 테스트 시작")
                    try {
                        val list = ArrayList<ByteArray>()
                        while (true) {
                            list.add(ByteArray(10 * 1024 * 1024)) // 10MB 할당
                        }
                    } catch (e: OutOfMemoryError) {
                        SentryUtil.captureFatal(e, "의도적인 OOM 테스트")
                        throw e
                    }
                }
            }
            
            // ANR 시뮬레이션
            DevMenuButton(text = "ANR 시뮬레이션 (UI 잠금)") {
                SentryUtil.logWarning("의도적인 ANR 테스트 시작")
                // 메인 스레드 차단 (10초)
                Thread.sleep(10000)
            }
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
        // Preview에서는 모든 람다를 비워둠
        DevMenuScreen(
            onNavigateBack = {},
            onNavigateToSplash = {},
            onNavigateToLogin = {},
            onNavigateToSignUp = {},
            onNavigateToFindPassword = {},
            onNavigateToMain = {},
            onNavigateToAddProject = {},
            onNavigateToSetProjectName = {},
            onNavigateToJoinProject = {},
            onNavigateToProjectSetting = {},
            onNavigateToCreateCategory = {},
            onNavigateToCreateChannel = { _, _ -> },
            onNavigateToEditCategory = { _, _ -> },
            onNavigateToEditChannel = { _, _, _ -> },
            onNavigateToMemberList = {},
            onNavigateToEditMember = { _, _ -> },
            onNavigateToRoleList = {},
            onNavigateToAddRole = {},
            onNavigateToEditRole = { _, _ -> },
            onNavigateToFriends = {},
            onNavigateToAcceptFriends = {},
            onNavigateToEditProfile = {},
            onNavigateToChangePassword = {},
            onNavigateToChat = {},
            onNavigateToCalendar24Hour = { _, _, _ -> },
            onNavigateToAddSchedule = { _, _, _ -> },
            onNavigateToScheduleDetail = {},
            onNavigateToSearch = {}
        )
    }
}