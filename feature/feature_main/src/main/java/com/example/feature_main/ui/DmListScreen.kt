package com.example.feature_main.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.domain.model.ui.DmUiModel

/**
 * DM 대화 목록을 표시하는 화면입니다.
 *
 * @param dms 표시할 DM 목록
 * @param onDmClick DM 아이템 클릭 시 호출될 콜백 (DM ID 전달)
 * @param modifier Modifier
 */
@Composable
fun DmListScreen(
    dms: List<DmUiModel>,
    onDmClick: (dmId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: 실제 DM 목록 UI 구현 (LazyColumn 등 사용)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("DM List Screen Placeholder\n(${dms.size} conversations)")
    }
} 