package com.example.feature_main.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.domain.model.ui.ProjectUiModel

/**
 * 프로젝트 목록을 표시하는 화면입니다.
 *
 * @param projects 표시할 프로젝트 목록
 * @param onProjectClick 프로젝트 아이템 클릭 시 호출될 콜백 (Project ID 전달)
 * @param modifier Modifier
 */
@Composable
fun ProjectListScreen(
    projects: List<ProjectUiModel>,
    onProjectClick: (projectId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: 실제 프로젝트 목록 UI 구현 (LazyColumn 등 사용)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Project List Screen Placeholder\n(${projects.size} projects)")
    }
} 