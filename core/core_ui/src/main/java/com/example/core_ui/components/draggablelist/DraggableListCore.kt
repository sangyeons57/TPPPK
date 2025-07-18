package com.example.core_ui.components.draggablelist

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope

/**
 * 드래그 가능한 아이템의 기본 데이터 구조.
 *
 * @param T 실제 아이템 데이터의 타입.
 * @param id 아이템의 고유 식별자.
 * @param originalData 원본 아이템 데이터.
 */
data class DraggableListItemData<T>(
    val id: String,
    val originalData: T
)

/**
 * 드래그 앤 드롭 리스트의 내부 상태를 관리하는 클래스.
 * UI 로직과 상태 관리를 분리하기 위해 사용됩니다.
 *
 * @param T 실제 아이템 데이터의 타입.
 * @param scope Composable의 CoroutineScope.
 * @param initialItems 초기 아이템 목록.
 * @param onItemMove 아이템 이동이 완료되었을 때 호출되는 콜백 (id, fromIndex, toIndex).
 */
@Stable
class DraggableListState<T>(
    private val scope: CoroutineScope,
    initialItems: List<DraggableListItemData<T>>,
    val lazyListState: LazyListState = LazyListState(),
    private val onItemMove: (id: String, fromIndex: Int, toIndex: Int) -> Unit
) {
    var items by mutableStateOf(initialItems)
        private set

    var isDragging by mutableStateOf(false)
        private set

    var draggedItemIndex by mutableStateOf<Int?>(null)
        private set
    var draggedItemId by mutableStateOf<String?>(null) // 드래그 중인 아이템의 ID
        private set
    
    var draggedItemOffsetY by mutableStateOf(0f)
        private set

    // 드래그 중인 아이템이 드롭될 예상 인덱스
    var currentDropTargetIndex by mutableStateOf<Int?>(null)
        private set

    /**
     * 특정 인덱스의 아이템에 대한 드래그를 시작합니다.
     * @param index 드래그를 시작할 아이템의 인덱스.
     */
    fun startDrag(index: Int) {
        if (items.indices.contains(index)) {
            val draggedItem = items[index]
            draggedItemIndex = index
            draggedItemId = draggedItem.id
            currentDropTargetIndex = index
            isDragging = true
            draggedItemOffsetY = 0f
        }
    }

    /**
     * 드래그 제스처 중 아이템의 위치를 업데이트합니다.
     * @param dragAmount Y축 이동량.
     */
    fun onDrag(dragAmount: Float) {
        if (!isDragging || draggedItemIndex == null) return

        draggedItemOffsetY += dragAmount

        val currentDraggedInitialIndex = draggedItemIndex ?: return
        val listLayoutInfo = lazyListState.layoutInfo
        val visibleItemsInfo = listLayoutInfo.visibleItemsInfo

        val draggedItemVisualInfo = visibleItemsInfo.find { it.index == currentDraggedInitialIndex } ?: return
        val draggedItemCurrentTopY = draggedItemVisualInfo.offset + draggedItemOffsetY
        val draggedItemCurrentBottomY = draggedItemCurrentTopY + draggedItemVisualInfo.size

        var newTargetIndex = currentDraggedInitialIndex

        // 다른 아이템과의 위치 비교로 새로운 인덱스 결정
        for (visibleItem in visibleItemsInfo) {
            if (visibleItem.index == currentDraggedInitialIndex) continue

            val otherItemTop = visibleItem.offset.toFloat()
            val otherItemBottom = (visibleItem.offset + visibleItem.size).toFloat()
            val otherItemCenterY = otherItemTop + visibleItem.size / 2f

            // 아래로 드래그 - 다른 아이템의 중앙점을 넘어서면 그 아이템의 인덱스로 이동
            if (currentDraggedInitialIndex < visibleItem.index) {
                if (draggedItemCurrentBottomY > otherItemCenterY && draggedItemCurrentTopY < otherItemBottom) {
                    newTargetIndex = visibleItem.index
                }
            }
            // 위로 드래그 - 다른 아이템의 중앙점을 넘어서면 그 아이템의 인덱스로 이동
            else if (currentDraggedInitialIndex > visibleItem.index) {
                if (draggedItemCurrentTopY < otherItemCenterY && draggedItemCurrentBottomY > otherItemTop) {
                    newTargetIndex = visibleItem.index
                }
            }
        }
        
        currentDropTargetIndex = newTargetIndex
    }

    /**
     * 드래그를 종료하고, 필요한 경우 아이템 이동 콜백을 호출합니다.
     */
    fun endDrag() {
        if (isDragging) {
            val dItemId = draggedItemId
            val fromIdx = draggedItemIndex
            val toIdx = currentDropTargetIndex

            if (dItemId != null && fromIdx != null && toIdx != null && fromIdx != toIdx) {
                onItemMove(dItemId, fromIdx, toIdx)
            }
        }
        resetDragState()
    }
    
    /**
     * 드래그가 취소되었을 때 상태를 리셋합니다.
     */
    fun cancelDrag() {
        resetDragState()
    }

    private fun resetDragState() {
        isDragging = false
        draggedItemIndex = null
        draggedItemId = null
        draggedItemOffsetY = 0f
        currentDropTargetIndex = null
    }

    /**
     * 외부에서 아이템 목록을 업데이트할 수 있도록 하는 함수.
     * onItemMove 콜백 처리 후 호출될 것을 기대합니다.
     */
    fun updateItems(newItems: List<DraggableListItemData<T>>) {
        items = newItems
    }
}

/**
 * DraggableListState 인스턴스를 생성하고 기억하는 Composable 함수.
 *
 * @param T 실제 아이템 데이터의 타입.
 * @param initialItems 초기 아이템 목록.
 * @param onItemMove 아이템 이동이 완료되었을 때 호출되는 콜백 (id, fromIndex, toIndex).
 * @return 기억된 DraggableListState 인스턴스.
 */
@Composable
fun <T> rememberDraggableListState(
    initialItems: List<DraggableListItemData<T>>,
    onItemMove: (id: String, fromIndex: Int, toIndex: Int) -> Unit,
): DraggableListState<T> {
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    // initialItems가 변경되어도 DraggableListState가 재 생성되지 않도록 key 사용 고려
    // 또는 DraggableListState 내부에서 initialItems 변경에 대응하는 로직 추가 필요
    val state = remember(scope, lazyListState) { // onItemMove는 recomposition 시 변경될 수 있으므로 key에서 제외하거나 LaunchedEffect로 전달
        DraggableListState(scope, initialItems, lazyListState, onItemMove)
    }
    // initialItems가 외부에서 변경될 경우 state의 items도 업데이트
    LaunchedEffect(initialItems) {
        state.updateItems(initialItems)
    }
    return state
} 