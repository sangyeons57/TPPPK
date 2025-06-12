package com.example.core_ui.components.bottom_sheet_dialog

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 메서드 체이닝을 지원하는 바텀 시트 빌더 클래스.
 * 사용법: BottomSheetBuilder().action(...).spacer(...).build()
 */
class BottomSheetDialogBuilder {
    // 내부적으로 아이템들을 저장할 리스트
    private val items = mutableListOf<BottomSheetDialogItem>()

    /**
     * 클릭 가능한 액션 아이템을 추가합니다.
     * @return 계속해서 메서드를 연결할 수 있도록 빌더 자신(this)을 반환합니다.
     */
    fun button(label: String, icon: ImageVector? = null, onClick: () -> Unit): BottomSheetDialogBuilder {
        items.add(BottomSheetDialogItem.Button(label, icon, onClick))
        return this
    }

    /**
     * 단순 텍스트 아이템을 추가합니다.
     * @return 계속해서 메서드를 연결할 수 있도록 빌더 자신(this)을 반환합니다.
     */
    fun text(text: String, style: @Composable () -> TextStyle = { MaterialTheme.typography.titleMedium }): BottomSheetDialogBuilder {
        items.add(BottomSheetDialogItem.Text(text, style))
        return this
    }

    /**
     * 공백 아이템을 추가합니다.
     * @return 계속해서 메서드를 연결할 수 있도록 빌더 자신(this)을 반환합니다.
     */
    fun spacer(height: Dp = 16.dp): BottomSheetDialogBuilder {
        items.add(BottomSheetDialogItem.Spacer(height))
        return this
    }

    /**
     * 지금까지 추가된 모든 아이템을 바탕으로 최종적인 리스트를 생성하여 반환합니다.
     * @return 생성된 불변(immutable) 리스트.
     */
    fun build(): List<BottomSheetDialogItem> {
        return items.toList()
    }
}