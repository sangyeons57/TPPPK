package com.example.feature_schedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save // 저장 아이콘
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.feature_schedule.viewmodel.EditScheduleEvent
import com.example.feature_schedule.viewmodel.EditScheduleUiState
import com.example.feature_schedule.viewmodel.EditScheduleViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * 기존 일정을 수정하는 화면입니다. (Placeholder)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleScreen(
    navigationHandler: ComposeNavigationHandler,
    modifier: Modifier = Modifier,
    viewModel: EditScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditScheduleEvent.NavigateBack -> {
                    navigationHandler.navigateBack()
                }
                is EditScheduleEvent.SaveSuccessAndRequestBackNavigation -> {
                    navigationHandler.setResult("schedule_added_or_updated", true)
                    navigationHandler.navigateBack()
                }
                is EditScheduleEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("일정 수정") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로 가기")
                    }
                },
                actions = {
                    if (!uiState.isLoading) { // 로딩 중 아닐 때만 저장 버튼 표시
                        IconButton(onClick = { viewModel.onSaveClicked() }, enabled = !uiState.isSaving) {
                            Icon(Icons.Filled.Save, "저장")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        EditScheduleContent(
            paddingValues = paddingValues,
            uiState = uiState,
            onTitleChanged = viewModel::onTitleChanged,
            onContentChanged = viewModel::onContentChanged,
            onSaveClicked = viewModel::onSaveClicked
        )
    }
}

/**
 * 일정 수정 화면의 내용을 구성합니다.
 */
@Composable
fun EditScheduleContent(
    paddingValues: PaddingValues,
    uiState: EditScheduleUiState,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onSaveClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
                Text(
                    text = "오류: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.isSaving) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = onTitleChanged,
                        label = { Text("일정 제목") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = !uiState.isSaving
                    )

                    OutlinedTextField(
                        value = uiState.content ?: "",
                        onValueChange = onContentChanged,
                        label = { Text("상세 내용 (선택 사항)") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        enabled = !uiState.isSaving
                    )

                    // TODO: 날짜, 시간 선택 UI 추가 (DatePicker, TimePicker)
                    Text("날짜: ${uiState.date?.toString() ?: "선택 안 함"}")
                    Text("시작 시간: ${uiState.startTime?.toString() ?: "선택 안 함"}")
                    Text("종료 시간: ${uiState.endTime?.toString() ?: "선택 안 함"}")

                    Spacer(modifier = Modifier.weight(1f)) // 버튼을 하단으로 밀기

                    Button(
                        onClick = onSaveClicked,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving && uiState.title.isNotBlank()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("저장 중...")
                        } else {
                            Text("일정 저장")
                        }
                    }
                }
            }
        }
    }
} 