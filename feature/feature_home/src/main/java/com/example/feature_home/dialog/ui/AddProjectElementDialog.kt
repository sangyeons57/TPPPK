package com.example.feature_home.dialog.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.base.Category
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.category.CategoryName
import com.example.feature_home.dialog.viewmodel.AddProjectElementEvent
import com.example.feature_home.dialog.viewmodel.AddProjectElementViewModel

/**
 * 프로젝트에 새로운 요소(카테고리 또는 채널)를 추가하는 다이얼로그입니다.
 * 이 다이얼로그는 ViewModel을 통해 프로젝트 ID를 전달받고, 카테고리 및 채널 추가 로직을 처리합니다.
 * 스낵바 메시지는 ViewModel의 이벤트를 통해 내부적으로 처리됩니다.
 *
 * @param onDismissRequest 다이얼로그를 닫아야 할 때 호출됩니다.
 * @param viewModel [AddProjectElementViewModel] 인스턴스로, UI 로직 및 데이터 처리를 담당합니다.
 */
@Composable
fun AddProjectElementDialog(
    projectId: DocumentId,
    onDismissRequest: () -> Unit,
    viewModel: AddProjectElementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("카테고리", "채널")
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, projectId) {
        viewModel.initialize(projectId)
    }

    LaunchedEffect(key1 = viewModel) { // Key on viewModel to restart if it changes
        viewModel.eventFlow.collect {
            when (it) {
                is AddProjectElementEvent.DismissDialog -> onDismissRequest()
                is AddProjectElementEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = it.message)
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SnackbarHost(hostState = snackbarHostState) // Add SnackbarHost here
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    when (selectedTabIndex) {
                        0 -> CategoryInput(
                            onAdd = { name -> viewModel.onAddCategory(CategoryName.from(name)) },
                            onDismiss = onDismissRequest
                        )
                        1 -> ChannelInput(
                            categories = uiState.availableCategories,
                            onAdd = { name, category, type ->
                                viewModel.onAddChannel(Name.from(name), type, category)
                            },
                            onDismiss = onDismissRequest
                        )
                    }
                }
            }
        }
    }
}

/**
 * 카테고리 추가를 위한 입력 필드
 */
@Composable
private fun CategoryInput(
    onAdd: (name: String) -> Unit, // order 제거
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val isAddEnabled = name.isNotBlank()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("카테고리 이름") },
            singleLine = true
        )
        // 순서 입력 필드 제거
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onAdd(name) }, // order 제거
                enabled = isAddEnabled
            ) {
                Text("추가")
            }
        }
    }
}

/**
 * 채널 추가를 위한 입력 필드
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelInput(
    categories: List<Category>,
    onAdd: (name: String, parentCategory: Category?, type: ProjectChannelType) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    // The first category should be "No Category" as it's sorted by order 0.
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull()) }
    var channelTypeExpanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(ProjectChannelType.MESSAGES) }
    val channelTypes = ProjectChannelType.values()

    val isAddEnabled = name.isNotBlank()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("채널 이름") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Category Dropdown using ExposedDropdownMenuBox
        ExposedDropdownMenuBox(
            expanded = categoryDropdownExpanded,
            onExpandedChange = { newState ->
                Log.d("ChannelInput", "Category ExposedDropdownMenuBox onExpandedChange: $newState")
                categoryDropdownExpanded = newState
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory?.name?.value ?: "카테고리를 선택해주세요",
                onValueChange = {},
                label = { Text("카테고리") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.textFieldColors(
                    unfocusedContainerColor = Color.Transparent
                )
            )
            ExposedDropdownMenu(
                expanded = categoryDropdownExpanded,
                onDismissRequest = { categoryDropdownExpanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name.value) },
                        onClick = {
                            selectedCategory = category
                            categoryDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Channel Type Dropdown using ExposedDropdownMenuBox
        ExposedDropdownMenuBox(
            expanded = channelTypeExpanded,
            onExpandedChange = { newState ->
                Log.d("ChannelInput", "ChannelType ExposedDropdownMenuBox onExpandedChange: $newState")
                channelTypeExpanded = newState
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedType.value,
                onValueChange = {},
                label = { Text("채널 타입") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = channelTypeExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.textFieldColors(
                    unfocusedContainerColor = Color.Transparent
                )
            )
            ExposedDropdownMenu(
                expanded = channelTypeExpanded,
                onDismissRequest = { channelTypeExpanded = false }
            ) {
                channelTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.value) },
                        onClick = {
                            selectedType = type
                            channelTypeExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onAdd(name, selectedCategory, selectedType) },
                enabled = isAddEnabled
            ) {
                Text("추가")
            }
        }
    }
}
