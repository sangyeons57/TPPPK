package com.example.feature_settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.feature_settings.viewmodel.MigrationToolViewModel

/**
 * 채널 마이그레이션 도구 UI
 * 기존 DM과 프로젝트 채팅 데이터를 새 채널 구조로 마이그레이션하는 기능을 제공합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrationToolScreen(
    viewModel: MigrationToolViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("채널 마이그레이션 도구") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 경고 메시지
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "주의: 이 도구는 기존 DM과 프로젝트 채팅 데이터를 새로운 채널 구조로 이전합니다. 작업 전 데이터 백업을 권장합니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // DM 마이그레이션
            item {
                MigrationSection(
                    title = "DM 대화 마이그레이션",
                    description = "기존 DM 대화를 새 채널 시스템으로 이전합니다.",
                    isRunning = uiState.isDmMigrationRunning,
                    stats = uiState.dmMigrationStats,
                    onStartClick = { viewModel.startDmMigration() }
                )
            }
            
            // 프로젝트 마이그레이션
            item {
                MigrationSection(
                    title = "프로젝트 채널 마이그레이션",
                    description = "선택한 프로젝트의 채널을 새 시스템으로 이전합니다.",
                    isRunning = uiState.isProjectMigrationRunning,
                    stats = uiState.projectMigrationStats,
                    textFieldValue = uiState.projectIdInput,
                    textFieldLabel = "프로젝트 ID",
                    onTextFieldChange = { viewModel.updateProjectIdInput(it) },
                    onStartClick = { viewModel.startProjectMigration() }
                )
            }
            
            // 오류 목록
            if (uiState.dmMigrationStats?.errors?.isNotEmpty() == true || 
                uiState.projectMigrationStats?.errors?.isNotEmpty() == true) {
                
                item {
                    Text(
                        text = "발생한 오류",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                val errors = mutableListOf<String>().apply {
                    uiState.dmMigrationStats?.errors?.let { addAll(it) }
                    uiState.projectMigrationStats?.errors?.let { addAll(it) }
                }
                
                items(errors) { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 마이그레이션 섹션 UI 컴포넌트
 */
@Composable
fun MigrationSection(
    title: String,
    description: String,
    isRunning: Boolean,
    stats: MigrationStats?,
    textFieldValue: String? = null,
    textFieldLabel: String? = null,
    onTextFieldChange: ((String) -> Unit)? = null,
    onStartClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 제목
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // 설명
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            // 입력 필드 (옵션)
            if (textFieldValue != null && textFieldLabel != null && onTextFieldChange != null) {
                TextField(
                    value = textFieldValue,
                    onValueChange = onTextFieldChange,
                    label = { Text(textFieldLabel) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    enabled = !isRunning
                )
            }
            
            // 상태 표시
            if (stats != null) {
                Text(
                    text = "생성된 채널: ${stats.channelsCreated}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "이전된 메시지: ${stats.messagesTransferred}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (stats.failureCount > 0) {
                    Text(
                        text = "오류 발생: ${stats.failureCount}건",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // 진행 상태 표시
            if (isRunning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
            
            // 버튼
            Button(
                onClick = onStartClick,
                modifier = Modifier.align(Alignment.End),
                enabled = !isRunning
            ) {
                Text(if (stats == null) "시작" else "계속")
            }
        }
    }
}

/**
 * 마이그레이션 통계 데이터 클래스
 */
data class MigrationStats(
    val channelsCreated: Int,
    val messagesTransferred: Int,
    val failureCount: Int,
    val errors: List<String>
) 