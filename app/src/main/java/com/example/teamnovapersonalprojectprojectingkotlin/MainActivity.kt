package com.example.teamnovapersonalprojectprojectingkotlin

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

@AndroidEntryPoint // Hilt 사용 시 Activity에 추가
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}