package com.example.teamnovapersonalprojectprojectingkotlin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme

/**
 * MainActivity - 앱의 메인 화면
 * 각 기능별 화면으로 이동할 수 있는 테스트용 네비게이션 화면을 제공합니다.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeamnovaPersonalProjectProjectingKotlinTheme {
                MainScreen()
            }
        }
    }
}

/**
 * MainScreen - 메인 화면의 UI를 구성하는 Composable
 * 각 기능별 화면으로 이동하는 버튼들을 포함합니다.
 */
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                text = "Auth Feature",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            NavigationButton("Login") { navigateToScreen(context, "feature_auth.ui.LoginActivity") }
            NavigationButton("Join") { navigateToScreen(context, "feature_auth.ui.JoinActivity") }
            NavigationButton("Find Password") { navigateToScreen(context, "feature_auth.ui.FindPasswordActivity") }

            Text(
                text = "Project Feature",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            NavigationButton("Project List") { navigateToScreen(context, "feature_project.ui.ProjectListActivity") }
            NavigationButton("Project Detail") { navigateToScreen(context, "feature_project.ui.ProjectDetailActivity") }

            Text(
                text = "Calendar Feature",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            NavigationButton("Calendar") { navigateToScreen(context, "feature_calendar.ui.CalendarActivity") }

            Text(
                text = "Chat Feature",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            NavigationButton("Chat") { navigateToScreen(context, "feature_chat.ui.ChatActivity") }

            Text(
                text = "Friends Feature",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            NavigationButton("Friends") { navigateToScreen(context, "feature_friends.ui.FriendsActivity") }

            Text(
                text = "Profile Feature",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            NavigationButton("Profile") { navigateToScreen(context, "feature_profile.ui.ProfileActivity") }

            Text(
                text = "Settings Feature",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            NavigationButton("Settings") { navigateToScreen(context, "feature_settings.ui.SettingsActivity") }

            Text(
                text = "Member Feature",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            NavigationButton("Member") { navigateToScreen(context, "feature_member.ui.MemberActivity") }
        }
    }
}

/**
 * 화면 이동을 처리하는 함수
 * @param context Context
 * @param activityPath Activity 경로
 */
private fun navigateToScreen(context: android.content.Context, activityPath: String) {
    try {
        val className = "${context.packageName}.$activityPath"
        val activityClass = Class.forName(className)
        context.startActivity(Intent(context, activityClass))
    } catch (e: Exception) {
        Toast.makeText(context, "화면을 찾을 수 없습니다: $activityPath", Toast.LENGTH_SHORT).show()
    }
}

/**
 * NavigationButton - 화면 이동을 위한 공통 버튼 Composable
 * @param text 버튼에 표시될 텍스트
 * @param onClick 버튼 클릭 시 실행될 동작
 */
@Composable
fun NavigationButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text)
    }
}

/**
 * Preview 함수들
 * 라이트 모드와 다크 모드 모두 확인 가능
 */
@Preview(name = "Light Mode", showBackground = true)
@Composable
fun MainScreenPreviewLight() {
    TeamnovaPersonalProjectProjectingKotlinTheme(darkTheme = false) {
        MainScreen()
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainScreenPreviewDark() {
    TeamnovaPersonalProjectProjectingKotlinTheme(darkTheme = true) {
        MainScreen()
    }
}