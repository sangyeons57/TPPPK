package com.example.teamnovapersonalprojectprojectingkotlin.feature_search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear // 검색어 지우기
import androidx.compose.material.icons.filled.Search // 검색 아이콘
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.teamnovapersonalprojectprojectingkotlin.R // 기본 이미지
// Domain 모델 및 ViewModel 관련 요소 Import
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.MessageResult
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.SearchResultItem
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.SearchScope
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.UserResult
import com.example.teamnovapersonalprojectprojectingkotlin.feature_search.viewmodel.SearchEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_search.viewmodel.SearchUiState
import com.example.teamnovapersonalprojectprojectingkotlin.feature_search.viewmodel.SearchViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * SearchScreen: 검색 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToChat: (channelId: String, messageId: Int) -> Unit,
    onNavigateToUserProfile: (userId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is SearchEvent.NavigateToMessage -> onNavigateToChat(event.channelId, event.messageId)
                is SearchEvent.NavigateToUserProfile -> onNavigateToUserProfile(event.userId)
                is SearchEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
        // 화면 진입 시 포커스 요청 (선택적)
        // focusRequester.requestFocus()
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("검색") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
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
                SearchScope.values().forEach { scope ->
                    Tab(
                        selected = uiState.selectedScope == scope,
                        onClick = { viewModel.onScopeChange(scope) },
                        text = { Text(scope.displayName) }
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
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("오류: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    // 검색 결과 없음
                    uiState.searchPerformed && uiState.searchResults.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("'${uiState.query}'에 대한 검색 결과가 없습니다.")
                        }
                    }
                    // 초기 상태 (검색 전)
                    !uiState.searchPerformed -> {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("검색어를 입력하여 검색을 시작하세요.")
                        }
                    }
                    // 검색 결과 표시
                    else -> {
                        SearchResultList(
                            results = uiState.searchResults, // ★ Domain 모델 리스트 전달
                            onResultClick = viewModel::onResultClick // ViewModel 함수 호출
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
                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp) // 공간 확보
            )
            Text( // 시간
                text = messageResult.timestamp.format(DateTimeFormatter.ofPattern("yyyy.MM.dd a h:mm")), // 시간 포맷팅
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
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(userResult.profileImageUrl ?: R.drawable.ic_account_circle_24)
                .error(R.drawable.ic_account_circle_24)
                .placeholder(R.drawable.ic_account_circle_24)
                .build(),
            contentDescription = "${userResult.userName} 프로필",
            modifier = Modifier.size(40.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
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
                    text = userResult.status,
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
        MessageResult("m1", "ch1", "일반 채널", 101, "김개발", "이전 프로젝트 검색 결과입니다.", LocalDateTime.now().minusDays(1)),
        UserResult("u1", "u1", "박기획", "url...", "회의 중"),
        MessageResult("m2", "ch2", "중요 공지", 102, "최관리", "검색 테스트 메시지 내용 미리보기", LocalDateTime.now().minusHours(3)),
        UserResult("u2", "u2", "정디자인", null, null)
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
                OutlinedTextField(value = "검색", onValueChange = {}, modifier = Modifier.fillMaxWidth().padding(16.dp))
                PrimaryTabRow(selectedTabIndex = 0) {
                    SearchScope.values().forEach { Tab(selected = it == SearchScope.ALL, onClick = {}, text = { Text(it.displayName) }) }
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
    val previewUiState = SearchUiState(query = "없는단어", searchPerformed = true, searchResults = emptyList())
    // ... (Scaffold 구조는 위와 유사하게) ...
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("검색") }) }
        ) { padding ->
            Column(Modifier.padding(padding)) {
                OutlinedTextField(value = "없는단어", onValueChange = {}, modifier = Modifier.fillMaxWidth().padding(16.dp))
                PrimaryTabRow(selectedTabIndex = 0) {
                    SearchScope.values().forEach { Tab(selected = it == SearchScope.ALL, onClick = {}, text = { Text(it.displayName) }) }
                }
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
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
    val previewUiState = SearchUiState(query = "검색중", isLoading = true)
    // ... (Scaffold 구조는 위와 유사하게) ...
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("검색") }) }
        ) { padding ->
            Column(Modifier.padding(padding)) {
                OutlinedTextField(value = "검색중", onValueChange = {}, modifier = Modifier.fillMaxWidth().padding(16.dp))
                PrimaryTabRow(selectedTabIndex = 0) {
                    SearchScope.values().forEach { Tab(selected = it == SearchScope.ALL, onClick = {}, text = { Text(it.displayName) }) }
                }
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}