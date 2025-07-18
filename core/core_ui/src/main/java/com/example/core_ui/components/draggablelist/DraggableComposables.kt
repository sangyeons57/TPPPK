package com.example.core_ui.components.draggablelist

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 드래그 앤 드롭으로 순서 변경이 가능한 아이템들을 표시하는 LazyColumn 기반 리스트입니다.
 *
 * @param T 리스트 아이템의 원본 데이터 타입.
 * @param state `rememberDraggableListState`로 생성된 상태 객체.
 * @param modifier 이 컴포저블에 적용할 Modifier.
 * @param key 각 아이템의 고유 키를 생성하는 람다.
 * @param itemContent 각 아이템의 전체 UI를 정의하는 Composable 람다.
 */
@Composable
fun <T> DraggableList(
    state: DraggableListState<T>,
    modifier: Modifier = Modifier,
    key: (index: Int, item: DraggableListItemData<T>) -> Any = { _, item -> item.id },
    itemContent: @Composable (index: Int, itemData: DraggableListItemData<T>, isCurrentlyDragging: Boolean, listState: DraggableListState<T>) -> Unit
) {
    LazyColumn(
        state = state.lazyListState,
        modifier = modifier
    ) {
        itemsIndexed(
            items = state.items,
            key = key
        ) { index, itemData ->
            val isCurrentlyDraggingItem = state.isDragging && state.draggedItemId == itemData.id
            
            itemContent(index, itemData, isCurrentlyDraggingItem, state)
        }
    }
}

/**
 * DraggableList 내에서 사용될 수 있는 표준 드래그 가능 아이템 Composable 입니다.
 * 왼쪽에 드래그 핸들이 있고 오른쪽에 사용자 정의 컨텐츠가 오는 일반적인 레이아웃을 제공합니다.
 */
@Composable
fun <T> DraggableListItem(
    itemData: DraggableListItemData<T>,
    index: Int,
    isCurrentlyDragging: Boolean,
    draggableListState: DraggableListState<T>,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit = {
        Icon(imageVector = Icons.Filled.DragIndicator, contentDescription = "Drag to reorder")
    },
    content: @Composable BoxScope.() -> Unit
) {
    // 드래그 중일 때 전체 Row가 함께 이동하도록 수정
    val itemDisplayModifier = if (isCurrentlyDragging) {
        modifier.offset { IntOffset(0, draggableListState.draggedItemOffsetY.roundToInt()) }
    } else {
        modifier
    }

    Row(
        modifier = itemDisplayModifier
            .fillMaxWidth()
            .pointerInput(itemData.id) { 
                detectDragGestures(
                    onDragStart = { draggableListState.startDrag(index) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        draggableListState.onDrag(dragAmount.y)
                    },
                    onDragEnd = { draggableListState.endDrag() },
                    onDragCancel = { draggableListState.cancelDrag() }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            dragHandle()
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
} 