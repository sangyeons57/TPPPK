package com.example.teamnovapersonalprojectprojectingkotlin

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.teamnovapersonalprojectprojectingkotlin.navigation.AppNavigation
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import dagger.hilt.android.AndroidEntryPoint
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import io.sentry.android.replay.maskAllImages
import io.sentry.android.replay.maskAllText

@AndroidEntryPoint // Hilt 사용 시 Activity에 추가
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SentryInit()

        enableEdgeToEdge() // Edge-to-edge 디스플레이 활성화 (선택적)
        setContent {
            TeamnovaPersonalProjectProjectingKotlinTheme {
                // 앱 전체 배경색을 가진 Surface (선택적)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    // 앱 전체 네비게이션 시작
                    AppNavigation(navController);
                }
            }
        }
    }

    private fun SentryInit(context: Context = applicationContext) {
        SentryAndroid.init(context) { options ->
            options.dsn = "https://0a3d0d1fe57deb2e7baebd1f244a04de@o4509194335223808.ingest.us.sentry.io/4509194511974400"
            options.isDebug = true

            options.sessionReplay.onErrorSampleRate = 1.0
            options.sessionReplay.sessionSampleRate = 0.1

            options.sessionReplay.maskAllText = true
            options.sessionReplay.maskAllImages = true
        }
    }
}