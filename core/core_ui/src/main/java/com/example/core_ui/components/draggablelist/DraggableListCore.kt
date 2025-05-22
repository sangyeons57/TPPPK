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
 * @param depth 현재 아이템의 깊이 수준 (0부터 시작).
 * @param parentId 부모 아이템의 ID (최상위 아이템의 경우 null).
 * @param canAcceptChildren 이 아이템이 자식 아이템을 가질 수 있는지 여부.
 *                          (예: 카테고리는 채널을 자식으로 가질 수 있지만, 채널은 자식을 가질 수 없음)
 * @param maxChildDepth 이 아이템 아래에 허용되는 자식 아이템의 최대 상대적 깊이.
 *                      예: depth 0인 카테고리가 maxChildDepth=1 이면, 자식은 depth 1까지만 허용.
 */
data class DraggableListItemData<T>(
    val id: String,
    val originalData: T,
    val depth: Int = 0,
    val parentId: String? = null,
    val canAcceptChildren: Boolean = false, // 기본적으로 자식 수용 불가
    val maxRelativeChildDepth: Int = 0 // 이 아이템 기준으로 하위 몇 단계까지 허용하는지
)

/**
 * 드래그 앤 드롭 리스트의 내부 상태를 관리하는 클래스.
 * UI 로직과 상태 관리를 분리하기 위해 사용됩니다.
 *
 * @param T 실제 아이템 데이터의 타입.
 * @param scope Composable의 CoroutineScope.
 * @param initialItems 초기 아이템 목록.
 * @param onItemMove 아이템 이동이 완료되었을 때 호출되는 콜백 (id, fromIndex, toIndex, newParentId, newDepth).
 */
@Stable
class DraggableListState<T>(
    private val scope: CoroutineScope,
    initialItems: List<DraggableListItemData<T>>, // 초기 아이템은 depth 정보가 포함된 flatten list라고 가정
    val lazyListState: LazyListState = LazyListState(),
    // onItemMove 콜백 시그니처 변경: id, fromIndex, toIndex, newParentId, newDepth
    private val onItemMove: (id: String, fromIndex: Int, toIndex: Int, newParentId: String?, newDepth: Int) -> Unit
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

    // 드래그 중인 아이템이 드롭될 예상 인덱스 및 새로운 부모/깊이 정보
    data class DropTargetInfo(
        val index: Int,
        val parentId: String? = null,
        val depth: Int
    )
    var currentDropTargetInfo by mutableStateOf<DropTargetInfo?>(null)
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
            currentDropTargetInfo = DropTargetInfo(index, draggedItem.parentId, draggedItem.depth)
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
        val draggedItemData = items[currentDraggedInitialIndex]
        val listLayoutInfo = lazyListState.layoutInfo
        val visibleItemsInfo = listLayoutInfo.visibleItemsInfo

        val draggedItemVisualInfo = visibleItemsInfo.find { it.index == currentDraggedInitialIndex } ?: return
        val draggedItemCurrentTopY = draggedItemVisualInfo.offset + draggedItemOffsetY
        val draggedItemCurrentBottomY = draggedItemCurrentTopY + draggedItemVisualInfo.size

        var newTargetIndex = currentDraggedInitialIndex
        var newTargetParentId: String? = draggedItemData.parentId
        var newTargetDepth: Int = draggedItemData.depth

        // 1. 다른 아이템 위/아래로의 순서 변경 (같은 depth 유지 또는 부모 변경)
        for (visibleItem in visibleItemsInfo) {
            if (visibleItem.index == currentDraggedInitialIndex) continue

            val otherItemData = items[visibleItem.index]
            val otherItemTop = visibleItem.offset.toFloat()
            val otherItemBottom = (visibleItem.offset + visibleItem.size).toFloat()
            val otherItemCenterY = otherItemTop + visibleItem.size / 2f

            // 시나리오 1: 다른 아이템의 자식으로 들어갈 수 있는지 (항상 아이템 하단으로)
            if (otherItemData.canAcceptChildren && 
                draggedItemData.depth <= otherItemData.depth + otherItemData.maxRelativeChildDepth) {
                // 아이템의 하단 절반 영역에 드롭 시 자식으로 편입 시도
                if (draggedItemCurrentTopY > otherItemCenterY && draggedItemCurrentTopY < otherItemBottom) {
                    newTargetIndex = visibleItem.index + 1 // 해당 아이템 바로 다음으로 삽입 (시각적으로)
                    newTargetParentId = otherItemData.id
                    newTargetDepth = otherItemData.depth + 1
                    // currentDropTargetInfo 업데이트 후 바로 break 할 수도 있음 (우선순위 높은 규칙)
                }
            }

            // 시나리오 2: 같은 depth에서 순서 변경 (다른 아이템의 중앙을 기준으로)
            // 이 로직은 시나리오 1 (자식으로 편입)보다 후순위로 고려될 수 있음
            // 또는, 자식 편입이 아닌 경우에만 이 로직 실행
            if (newTargetParentId == draggedItemData.parentId && newTargetDepth == draggedItemData.depth) { // 아직 부모/깊이 변경이 없는 경우
                 if (currentDraggedInitialIndex < visibleItem.index) { // 아래로 드래그
                    if (draggedItemCurrentBottomY > otherItemCenterY && draggedItemCurrentTopY < otherItemBottom) {
                        if (otherItemData.parentId == newTargetParentId) { // 같은 부모 내에서만
                             newTargetIndex = visibleItem.index
                        }
                    }
                } else { // 위로 드래그
                    if (draggedItemCurrentTopY < otherItemCenterY && draggedItemCurrentBottomY > otherItemTop) {
                        if (otherItemData.parentId == newTargetParentId) {
                            newTargetIndex = visibleItem.index
                        }
                    }
                }
            }
            // TODO: 루트 레벨로 이동하는 로직 (예: 리스트 최상단/최하단 특정 영역에 드롭)
        }
        currentDropTargetInfo = DropTargetInfo(newTargetIndex, newTargetParentId, newTargetDepth)
    }

    /**
     * 드래그를 종료하고, 필요한 경우 아이템 이동 콜백을 호출합니다.
     */
    fun endDrag() {
        if (isDragging) {
            val dItemId = draggedItemId
            val fromIdx = draggedItemIndex
            val targetInfo = currentDropTargetInfo

            if (dItemId != null && fromIdx != null && targetInfo != null) {
                val originalItem = items[fromIdx]
                if (originalItem.parentId != targetInfo.parentId || 
                    originalItem.depth != targetInfo.depth || 
                    fromIdx != targetInfo.index) { // 실제 변경이 있을 경우에만 호출
                    onItemMove(dItemId, fromIdx, targetInfo.index, targetInfo.parentId, targetInfo.depth)
                }
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
        currentDropTargetInfo = null
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
 * @param onItemMove 아이템 이동이 완료되었을 때 호출되는 콜백 (id, fromIndex, toIndex, newParentId, newDepth).
 * @return 기억된 DraggableListState 인덱스.
 */
@Composable
fun <T> rememberDraggableListState(
    initialItems: List<DraggableListItemData<T>>,
    onItemMove: (id: String, fromIndex: Int, toIndex: Int, newParentId: String?, newDepth: Int) -> Unit,
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