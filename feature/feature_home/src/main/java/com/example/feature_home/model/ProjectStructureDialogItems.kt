package com.example.feature_home.model

/**
 * SimpleReorderDialog에서 사용할 통합된 프로젝트 구조 아이템
 */
data class UnifiedStructureDialogItem(
    val id: String,
    val displayName: String,
    val type: UnifiedStructureItemType,
    val originalItem: ProjectStructureItem
)

enum class UnifiedStructureItemType {
    CATEGORY,
    DIRECT_CHANNEL
}

/**
 * ProjectStructureUiState를 SimpleReorderDialog에서 사용할 수 있는 형태로 변환하는 확장 함수
 */
fun ProjectStructureUiState.toUnifiedDialogItems(): List<UnifiedStructureDialogItem> {
    android.util.Log.d("ProjectStructureDialogItems", "Converting ${unifiedStructureItems.size} items to dialog items")
    unifiedStructureItems.forEachIndexed { index, item ->
        when (item) {
            is ProjectStructureItem.CategoryItem -> {
                android.util.Log.d("ProjectStructureDialogItems", "Item $index: Category '${item.category.name.value}' (order: ${item.globalOrder})")
            }
            is ProjectStructureItem.DirectChannelItem -> {
                android.util.Log.d("ProjectStructureDialogItems", "Item $index: Direct Channel '${item.channel.name.value}' (order: ${item.globalOrder})")
            }
        }
    }
    
    return unifiedStructureItems.map { item ->
        when (item) {
            is ProjectStructureItem.CategoryItem -> {
                UnifiedStructureDialogItem(
                    id = item.category.id.value,
                    displayName = "📁 ${item.category.name.value}",
                    type = UnifiedStructureItemType.CATEGORY,
                    originalItem = item
                )
            }
            is ProjectStructureItem.DirectChannelItem -> {
                val channelIcon = when (item.channel.mode) {
                    com.example.domain.model.enum.ProjectChannelType.MESSAGES -> "💬"
                    com.example.domain.model.enum.ProjectChannelType.TASKS -> "✅"
                    else -> "📄"
                }
                UnifiedStructureDialogItem(
                    id = item.channel.id.value,
                    displayName = "$channelIcon ${item.channel.name.value}",
                    type = UnifiedStructureItemType.DIRECT_CHANNEL,
                    originalItem = item
                )
            }
        }
    }
}

/**
 * 재정렬된 다이얼로그 아이템을 다시 ProjectStructureItem 리스트로 변환하는 확장 함수
 */
fun List<UnifiedStructureDialogItem>.toProjectStructureItems(): List<ProjectStructureItem> {
    return this.map { it.originalItem }
}