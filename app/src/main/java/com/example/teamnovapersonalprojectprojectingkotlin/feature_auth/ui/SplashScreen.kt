package com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.R // 로고 리소스 ID
import com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.viewmodel.SplashEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_auth.viewmodel.SplashViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * SplashScreen: 앱 시작 시 표시되는 스플래시 화면 (Stateful)
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    // 이벤트 처리 (네비게이션)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                SplashEvent.NavigateToLogin -> onNavigateToLogin()
                SplashEvent.NavigateToMain -> onNavigateToMain()
            }
        }
    }

    SplashScreenContent(modifier = modifier)
}

/**
 * SplashScreenContent: 스플래시 화면 UI (Stateless)
 */
@Composable
fun SplashScreenContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background), // 앱 테마 배경색 사용
        contentAlignment = Alignment.Center // 콘텐츠 중앙 정렬
    ) {
        // TODO: 로고 이미지 리소스를 프로젝트에 맞게 수정하세요. (ic_launcher_foreground는 예시)
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground), // 앱 로고 표시
            contentDescription = "앱 로고",
            modifier = Modifier.size(120.dp) // 로고 크기 조절
        )

        // TODO: 필요 시 로딩 인디케이터 표시 (ViewModel 상태에 따라)
        // CircularProgressIndicator(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp))
    }
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun SplashScreenContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        SplashScreenContent()
    }
}