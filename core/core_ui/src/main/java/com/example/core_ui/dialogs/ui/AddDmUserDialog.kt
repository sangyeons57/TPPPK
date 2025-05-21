package com.example.core_ui.dialogs.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 사용자 이름을 입력하여 DM 대화를 시작할 사용자를 추가하는 다이얼로그
 *
 * @param onDismiss 다이얼로그가 닫힐 때 호출되는 콜백
 * @param onSearch 검색 버튼 클릭 시 호출되는 콜백
 * @param username 사용자 이름 입력 상태
 * @param onUsernameChange 사용자 이름 변경 시 호출되는 콜백
 * @param isLoading 로딩 상태
 * @param errorMessage 오류 메시지
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDmUserDialog(
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    
    // 키보드 자동 포커스
    LaunchedEffect(Unit) {
        // 약간의 딜레이 후 포커스 요청 (UI가 그려진 후)
        delay(100)
        focusRequester.requestFocus()
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 제목 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "DM 추가하기",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // 설명 텍스트
            Text(
                text = "사용자 이름을 입력하여 DM 대화를 시작하세요.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 입력 필드
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("사용자 이름") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                enabled = !isLoading,
                isError = errorMessage != null
            )
            
            // 오류 메시지 표시
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp)
                )
            }
            
            // 버튼 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp)
            ) {
                Button(
                    onClick = onSearch,
                    enabled = !isLoading && username.isNotBlank(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("찾기")
                    }
                }
            }
        }
    }
} 