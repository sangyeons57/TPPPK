package com.example.feature_search.ui

// Removed direct Coil imports
// Domain 모델 및 ViewModel 관련 요소 Import
// 네비게이션 관련 임포트 업데이트
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_common.util.DateTimeUtil
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.components.user.UserProfileImage
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.ui.search.MessageResult
import com.example.domain.model.ui.search.SearchResultItem
import com.example.domain.model.ui.search.SearchScope
import com.example.domain.model.ui.search.UserResult
import com.example.feature_search.viewmodel.SearchEvent
import com.example.feature_search.viewmodel.SearchUiState
import com.example.feature_search.viewmodel.SearchViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant

/**
 * SearchScreen: 검색 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    navigationManger: NavigationManger,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val focusRequester = remember { FocusRequester() }
    
    // 이벤트 수집 (상태가 아닌 일회성 이벤트)
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(key1 = viewModel) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is SearchEvent.NavigateToMessageDetail -> {
                    navigationManger.navigateToMessageDetail(event.channelId, event.messageId)
                }
                is SearchEvent.NavigateToUserProfile -> {
                    navigationManger.navigateToUserProfile(event.userId)
                }
                is SearchEvent.NavigateToMessage -> {
                    navigationManger.navigateToMessageDetail(event.channelId, event.messageId)
                }
                // 다른 이벤트 추가 가능
                is SearchEvent.ShowSnackbar -> {
                    // 에러 토스트 또는 스낵바 표시
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("검색") },
                navigationIcon = {
                    DebouncedBackButton(onClick = { navigationManger.navigateBack() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            // 검색 입력 필드
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text("검색어를 입력하세요...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색 아이콘") },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) { // 검색어 지우기
                            Icon(Icons.Default.Clear, contentDescription = "지우기")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.performSearch() // 키보드에서 검색 실행
                    keyboardController?.hide() // 키보드 숨기기
                })
            )

            // 검색 범위 선택 탭
            PrimaryTabRow(selectedTabIndex = uiState.selectedScope.ordinal) {
                SearchScope.entries.forEach { scope ->
                    Tab(
                        selected = uiState.selectedScope == scope,
                        onClick = { viewModel.onScopeChange(scope) },
                        text = { Text(scope.getDisplayName()) }
                    )
                }
            }

            // 검색 결과 영역
            Box(modifier = Modifier.weight(1f)) {
                when {
                    // 로딩 중
                    uiState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    // 에러 발생
                    uiState.error != null -> {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("오류: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    // 검색 결과 없음
                    uiState.searchPerformed && uiState.searchResults.isEmpty() -> {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("'${uiState.query}'에 대한 검색 결과가 없습니다.")
                        }
                    }
                    // 초기 상태 (검색 전)
                    !uiState.searchPerformed -> {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("검색어를 입력하여 검색을 시작하세요.")
                        }
                    }
                    // 검색 결과 표시
                    else -> {
                        SearchResultList(
                            results = uiState.searchResults, // ★ Domain 모델 리스트 전달
                            onResultClick = viewModel::onResultItemClick // ViewModel 함수 호출
                        )
                    }
                }
            }
        }
    }
}

/**
 * SearchResultList: 검색 결과 목록 UI (Stateless)
 */
@Composable
fun SearchResultList(
    modifier: Modifier = Modifier,
    results: List<SearchResultItem>, // ★ Domain 모델 타입 사용
    onResultClick: (SearchResultItem) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = results,
            key = { it.id } // ★ 각 아이템의 고유 ID 사용
        ) { item ->
            // 결과 타입에 따라 다른 Composable 호출
            when (item) {
                is MessageResult -> MessageResultItem(
                    messageResult = item,
                    onClick = { onResultClick(item) }
                )
                is UserResult -> UserResultItem(
                    userResult = item,
                    onClick = { onResultClick(item) }
                )
                // 다른 타입 추가 시 여기에 추가
            }
            HorizontalDivider()
        }
    }
}

/**
 * MessageResultItem: 메시지 검색 결과 아이템 UI (Stateless)
 */
@Composable
fun MessageResultItem(
    messageResult: MessageResult, // ★ Domain 모델 사용
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text( // 채널명 + 보낸사람
                text = "${messageResult.channelName} - ${messageResult.senderName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 8.dp) // 공간 확보
            )
            Text( // 시간
                text = DateTimeUtil.formatDateTime2(DateTimeUtil.toLocalDateTime(messageResult.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text( // 메시지 내용 (TODO: 검색어 하이라이팅 추가)
            text = messageResult.messageContent,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * UserResultItem: 사용자 검색 결과 아이템 UI (Stateless)
 */
@Composable
fun UserResultItem(
    userResult: UserResult, // ★ Domain 모델 사용
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserProfileImage(
            profileImageUrl = userResult.profileImageUrl,
            contentDescription = "${userResult.userName} 프로필",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = userResult.userName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (userResult.status != null) { // 상태 메시지 표시 (선택적)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = userResult.status!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


// --- Preview ---
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview_Results() {
    val previewResults = listOf(
        MessageResult(
            id = "m1",
            channelId = "ch1",
            channelName = "일반 채널",
            messageId = "msg1",
            messageContent = "이전 프로젝트 검색 결과입니다.",
            timestamp = Instant.now(),
            senderId = "user123",
            senderName = "김개발",
            highlightedContent = "이전 프로젝트 검색 결과"
        ),
        UserResult(
            id = "u1", 
            userId = "u1", 
            userName = "박기획",
            displayName = "박기획", 
            profileImageUrl = "url...", 
            status = "회의 중",
            isOnline = true,
            matchReason = "닉네임 일치"
        ),
        MessageResult(
            id = "m2",
            channelId = "ch2",
            channelName = "중요 공지",
            messageId = "msg2",
            messageContent = "검색 테스트 메시지 내용 미리보기",
            timestamp = Instant.now().minusSeconds(3600),
            senderId = "user456",
            senderName = "최관리",
            highlightedContent = "검색 테스트 메시지"
        ),
        UserResult(
            id = "u2", 
            userId = "u2", 
            userName = "정디자인",
            displayName = "정디자인",
            profileImageUrl = null,
            status = null,
            isOnline = false,
            matchReason = "이메일 일치"
        )
    )
    val previewUiState = SearchUiState(
        query = "검색",
        selectedScope = SearchScope.ALL,
        searchResults = previewResults,
        searchPerformed = true
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        // Scaffold 구조 포함하여 프리뷰
        Scaffold(
            topBar = { TopAppBar(title = { Text("검색") }) }
        ) { padding ->
            Column(Modifier.padding(padding)) {
                OutlinedTextField(value = "검색", onValueChange = {}, modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp))
                PrimaryTabRow(selectedTabIndex = 0) {
                    SearchScope.values().forEach { Tab(selected = it == SearchScope.ALL, onClick = {}, text = { Text(it.name) }) }
                }
                SearchResultList(results = previewUiState.searchResults, onResultClick = {})
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Search Screen Empty")
@Composable
private fun SearchScreenPreview_Empty() {
    val previewUiState = SearchUiState(query = "없는단어", searchPerformed = true)
    // ... (Scaffold 구조는 위와 유사하게) ...
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("검색") }) }
        ) { padding ->
            Column(Modifier.padding(padding)) {
                OutlinedTextField(value = "없는단어", onValueChange = {}, modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp))
                PrimaryTabRow(selectedTabIndex = 0) {
                    SearchScope.values().forEach { Tab(selected = it == SearchScope.ALL, onClick = {}, text = { Text(it.name) }) }
                }
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("'${previewUiState.query}'에 대한 검색 결과가 없습니다.")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Search Screen Loading")
@Composable
private fun SearchScreenPreview_Loading() {
    SearchUiState(query = "검색중", isLoading = true)
    // ... (Scaffold 구조는 위와 유사하게) ...
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("검색") }) }
        ) { padding ->
            Column(Modifier.padding(padding)) {
                OutlinedTextField(value = "검색중", onValueChange = {}, modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp))
                PrimaryTabRow(selectedTabIndex = 0) {
                    SearchScope.values().forEach { Tab(selected = it == SearchScope.ALL, onClick = {}, text = { Text(it.name) }) }
                }
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}