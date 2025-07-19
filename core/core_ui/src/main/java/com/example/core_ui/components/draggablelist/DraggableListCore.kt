package com.example.core_ui.components.draggablelist

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
 * @param onTargetIndexChange 드래그 중 목표 인덱스가 변경될 때 호출되는 콜백 (fromIndex, toIndex).
 * @param onRealtimeReorder 실시간 순서가 변경될 때 호출되는 콜백 (realtimeOrderedItems).
 */
@Stable
class DraggableListState<T>(
    private val scope: CoroutineScope,
    initialItems: List<DraggableListItemData<T>>,
    val lazyListState: LazyListState = LazyListState(),
    private val onItemMove: (id: String, fromIndex: Int, toIndex: Int) -> Unit,
    private val onTargetIndexChange: ((fromIndex: Int, toIndex: Int) -> Unit)? = null,
    private val onRealtimeReorder: ((realtimeOrderedItems: List<T>) -> Unit)? = null
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
    
    // 드래그 중 실시간 아이템 순서 (드래그 완료 시 실제 순서가 됨)
    var realtimeItems by mutableStateOf<List<DraggableListItemData<T>>>(initialItems)
        private set

    /**
     * 특정 아이템에 대한 드래그를 시작합니다.
     * @param itemId 드래그를 시작할 아이템의 ID.
     * @param index realtimeItems 배열에서의 인덱스.
     */
    fun startDrag(itemId: String, index: Int) {
        // 이미 드래그 중이면 무시
        if (isDragging) return
        
        if (realtimeItems.indices.contains(index)) {
            val draggedItem = realtimeItems[index]
            
            // itemId가 실제로 해당 인덱스의 아이템과 일치하는지 확인
            if (draggedItem.id == itemId) {
                draggedItemIndex = index
                draggedItemId = itemId
                currentDropTargetIndex = index
                isDragging = true
                draggedItemOffsetY = 0f
                
                // 드래그 시작 시 items와 realtimeItems 동기화 보장
                if (realtimeItems != items) {
                    realtimeItems = items.toList()
                    // 동기화 후 인덱스 재검증
                    val revalidatedIndex = realtimeItems.indexOfFirst { it.id == itemId }
                    if (revalidatedIndex != -1) {
                        draggedItemIndex = revalidatedIndex
                        currentDropTargetIndex = revalidatedIndex
                    }
                }
            }
        }
    }
    
    /**
     * 아이템 ID만으로 드래그를 시작합니다. 인덱스는 내부적으로 찾습니다.
     * @param itemId 드래그를 시작할 아이템의 ID.
     */
    fun startDragByItemId(itemId: String) {
        // 이미 드래그 중이면 무시
        if (isDragging) return
        
        // 드래그 시작 시 items와 realtimeItems 동기화 보장
        if (realtimeItems != items) {
            realtimeItems = items.toList()
        }
        
        // realtimeItems에서 해당 ID의 인덱스 찾기
        val index = realtimeItems.indexOfFirst { it.id == itemId }
        if (index != -1) {
            draggedItemIndex = index
            draggedItemId = itemId
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

        val currentDraggedIndex = draggedItemIndex ?: return
        val listLayoutInfo = lazyListState.layoutInfo
        val visibleItemsInfo = listLayoutInfo.visibleItemsInfo

        val draggedItemVisualInfo = visibleItemsInfo.find { it.index == currentDraggedIndex } ?: return
        val draggedItemCenterY = draggedItemVisualInfo.offset + draggedItemOffsetY + draggedItemVisualInfo.size / 2f

        var newTargetIndex = currentDraggedIndex

        // 개선된 방향성 감지 로직 - 드래그된 아이템의 중심점 기준으로 대칭적 처리
        newTargetIndex = calculateTargetIndex(
            draggedItemCenterY = draggedItemCenterY,
            currentIndex = currentDraggedIndex,
            visibleItems = visibleItemsInfo
        )
        
        // 자동 스크롤 처리
        handleAutoScroll(draggedItemCenterY, listLayoutInfo)
        
        // 실시간 콜백 호출: targetIndex가 변경될 때마다 호출
        val previousTarget = currentDropTargetIndex
        currentDropTargetIndex = newTargetIndex
        
        // 목표 인덱스가 변경되었을 때 실시간 재정렬 수행
        if (previousTarget != newTargetIndex) {
            updateRealtimeItems(currentDraggedIndex, newTargetIndex)
            
            // 안정적인 인덱스 업데이트 및 오프셋 보정
            updateDraggedItemIndex(
                previousIndex = currentDraggedIndex,
                newIndex = newTargetIndex,
                visibleItems = visibleItemsInfo
            )
            
            // 외부 콜백이 있으면 호출 (애니메이션 정보 전달용)
            if (onTargetIndexChange != null) {
                onTargetIndexChange.invoke(currentDraggedIndex, newTargetIndex)
            }
        }
    }

    /**
     * 드래그를 종료하고, 필요한 경우 아이템 이동 콜백을 호출합니다.
     */
    fun endDrag() {
        if (isDragging) {
            val dItemId = draggedItemId
            val fromIdx = draggedItemIndex
            val toIdx = currentDropTargetIndex

            // 실시간 순서 콜백 (실제 데이터 전달) - 상태 리셋 전에 호출
            if (realtimeItems.isNotEmpty() && onRealtimeReorder != null) {
                val realtimeOrderedData = realtimeItems.map { it.originalData }
                onRealtimeReorder.invoke(realtimeOrderedData)
            }
            
            // 드래그 완료 시 realtimeItems의 순서를 items에 반영 (점멸 방지)
            items = realtimeItems
            
            // 드래그 상태 해제
            resetDragState()
            
            if (dItemId != null && fromIdx != null && toIdx != null && fromIdx != toIdx) {
                // 기존 콜백 (호환성을 위해 유지)
                onItemMove(dItemId, fromIdx, toIdx)
            }
        } else {
            resetDragState()
        }
    }
    
    /**
     * 드래그가 취소되었을 때 상태를 리셋합니다.
     */
    fun cancelDrag() {
        resetDragState(restoreOriginalOrder = true)
    }

    private fun resetDragState(restoreOriginalOrder: Boolean = false) {
        isDragging = false
        draggedItemIndex = null
        draggedItemId = null
        draggedItemOffsetY = 0f
        currentDropTargetIndex = null
        
        // 드래그 취소 시에만 원래 순서로 복원
        if (restoreOriginalOrder) {
            realtimeItems = items
        }
        // 드래그 완료 시에는 realtimeItems 순서를 유지 (점멸 방지)
    }
    
    /**
     * 자동 스크롤 처리 - 화면 경계 근처에서 드래그 시
     */
    private fun handleAutoScroll(
        draggedItemCenterY: Float,
        listLayoutInfo: androidx.compose.foundation.lazy.LazyListLayoutInfo
    ) {
        val scrollThreshold = 100f // 스크롤 트리거 영역 (픽셀)
        val scrollSpeed = 15f // 스크롤 속도
        
        val viewportStartOffset = listLayoutInfo.viewportStartOffset
        val viewportEndOffset = listLayoutInfo.viewportEndOffset
        
        when {
            // 상단 경계 근처에서 드래그 시 위로 스크롤
            draggedItemCenterY - viewportStartOffset < scrollThreshold -> {
                scope.launch {
                    lazyListState.animateScrollToItem(
                        maxOf(0, lazyListState.firstVisibleItemIndex - 1)
                    )
                }
            }
            // 하단 경계 근처에서 드래그 시 아래로 스크롤
            viewportEndOffset - draggedItemCenterY < scrollThreshold -> {
                scope.launch {
                    lazyListState.animateScrollToItem(
                        lazyListState.firstVisibleItemIndex + 1
                    )
                }
            }
        }
    }

    /**
     * 드래그된 아이템의 인덱스를 안정적으로 업데이트하고 오프셋 보정
     */
    private fun updateDraggedItemIndex(
        previousIndex: Int,
        newIndex: Int,
        visibleItems: List<LazyListItemInfo>
    ) {
        if (previousIndex == newIndex) return
        
        // 이전 위치와 새로운 위치의 아이템 정보 찾기
        val oldItemInfo = visibleItems.find { it.index == previousIndex }
        val newItemInfo = visibleItems.find { it.index == newIndex }
        
        // 오프셋 보정 - 인덱스 변경으로 인한 위치 차이 보정
        if (oldItemInfo != null && newItemInfo != null) {
            // 드래그 방향을 고려한 더 정확한 오프셋 계산
            val isMovingDown = newIndex > previousIndex
            val positionDiff = if (isMovingDown) {
                // 아래로 이동 시: 새 위치까지의 거리만큼 오프셋 감소
                newItemInfo.offset - oldItemInfo.offset
            } else {
                // 위로 이동 시: 이전 위치와 새 위치 차이만큼 오프셋 증가
                newItemInfo.offset - oldItemInfo.offset
            }
            
            // 부드러운 전환을 위한 오프셋 보정
            draggedItemOffsetY -= positionDiff
        }
        
        // 인덱스 업데이트는 오프셋 보정 후에 수행
        draggedItemIndex = newIndex
    }

    /**
     * 개선된 타겟 인덱스 계산 - 대칭적 방향성 감지
     */
    private fun calculateTargetIndex(
        draggedItemCenterY: Float,
        currentIndex: Int,
        visibleItems: List<LazyListItemInfo>
    ): Int {
        var targetIndex = currentIndex
        val threshold = 0.5f // 50% 오버랩 시 이동
        
        for (item in visibleItems) {
            if (item.index == currentIndex) continue
            
            val itemCenterY = item.offset + item.size / 2f
            val itemThreshold = item.size * threshold
            
            when {
                // 아래로 드래그: 다음 아이템의 중심점을 충분히 통과했을 때
                currentIndex < item.index && draggedItemCenterY > itemCenterY + itemThreshold -> {
                    targetIndex = item.index
                }
                // 위로 드래그: 이전 아이템의 중심점을 충분히 통과했을 때
                currentIndex > item.index && draggedItemCenterY < itemCenterY - itemThreshold -> {
                    targetIndex = item.index
                }
            }
        }
        
        return targetIndex
    }

    /**
     * 실시간 아이템 순서 업데이트 - 간단한 shift-based 알고리즘
     */
    private fun updateRealtimeItems(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        
        val newItems = realtimeItems.toMutableList()
        
        // 드래그된 아이템을 제거하고 목표 위치에 삽입
        val draggedItem = newItems.removeAt(fromIndex)
        newItems.add(toIndex, draggedItem)
        
        realtimeItems = newItems
    }

    /**
     * 외부에서 아이템 목록을 업데이트할 수 있도록 하는 함수.
     * onItemMove 콜백 처리 후 호출될 것을 기대합니다.
     */
    fun updateItems(newItems: List<DraggableListItemData<T>>) {
        // 드래그 중이거나 items와 realtimeItems가 이미 동기화되어 있으면 업데이트 안함
        if (isDragging || items == realtimeItems) {
            return
        }
        
        items = newItems
        realtimeItems = newItems
    }
}

/**
 * DraggableListState 인스턴스를 생성하고 기억하는 Composable 함수.
 *
 * @param T 실제 아이템 데이터의 타입.
 * @param initialItems 초기 아이템 목록.
 * @param onItemMove 아이템 이동이 완료되었을 때 호출되는 콜백 (id, fromIndex, toIndex).
 * @param onTargetIndexChange 드래그 중 목표 인덱스가 변경될 때 호출되는 콜백 (fromIndex, toIndex).
 * @param onRealtimeReorder 실시간 순서가 변경될 때 호출되는 콜백 (realtimeOrderedItems).
 * @return 기억된 DraggableListState 인스턴스.
 */
@Composable
fun <T> rememberDraggableListState(
    initialItems: List<DraggableListItemData<T>>,
    onItemMove: (id: String, fromIndex: Int, toIndex: Int) -> Unit,
    onTargetIndexChange: ((fromIndex: Int, toIndex: Int) -> Unit)? = null,
    onRealtimeReorder: ((realtimeOrderedItems: List<T>) -> Unit)? = null
): DraggableListState<T> {
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    // initialItems가 변경되어도 DraggableListState가 재 생성되지 않도록 key 사용 고려
    // 또는 DraggableListState 내부에서 initialItems 변경에 대응하는 로직 추가 필요
    val state = remember(scope, lazyListState) { // onItemMove는 recomposition 시 변경될 수 있으므로 key에서 제외하거나 LaunchedEffect로 전달
        DraggableListState(scope, initialItems, lazyListState, onItemMove, onTargetIndexChange, onRealtimeReorder)
    }
    // initialItems가 외부에서 변경될 경우 state의 items도 업데이트
    LaunchedEffect(initialItems) {
        state.updateItems(initialItems)
    }
    LaunchedEffect(state.isDragging) {
        if (!state.isDragging) {
            withFrameNanos {  }
            // state.clearOffset()
        }
    }
    return state
} 