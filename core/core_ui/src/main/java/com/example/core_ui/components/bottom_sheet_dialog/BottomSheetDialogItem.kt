package com.example.core_ui.components.bottom_sheet_dialog

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 코어 바텀 시트에 표시될 수 있는 모든 아이템의 종류를 정의하는 sealed interface.
 */
sealed interface BottomSheetDialogItem {

    /**
     * 클릭 가능한 액션 아이템. (기존의 버튼)
     * @param label 아이템에 표시될 텍스트.
     * @param icon (선택 사항) 텍스트 앞에 표시될 아이콘.
     * @param onClick 아이템이 클릭되었을 때 실행될 람다 함수.
     */
    data class Button(
        val label: String,
        val icon: ImageVector? = null,
        val onClick: () -> Unit
    ) : BottomSheetDialogItem

    /**
     * 상호작용이 불가능한 단순 텍스트 아이템. (예: 제목, 설명)
     * @param text 표시할 문자열.
     * @param style 적용할 텍스트 스타일. 기본값은 MaterialTheme.typography.titleMedium.
     */
    data class Text(
        val text: String,
        val style: @Composable () -> TextStyle = { MaterialTheme.typography.titleMedium }
    ) : BottomSheetDialogItem

    /**
     * 상호작용이 불가능한 공백 아이템. 아이템들 사이에 간격을 줄 때 사용.
     * @param height 공백의 높이. 기본값은 16.dp.
     */
    data class Spacer(
        val height: Dp = 16.dp
    ) : BottomSheetDialogItem
}