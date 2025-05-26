package com.example.core_ui.dialogs.service

import com.example.core_ui.components.draggablelist.DraggableListItemData
import com.example.core_ui.dialogs.viewmodel.ProjectStructureDraggableItem
import com.example.domain.model.ProjectStructure
import javax.inject.Inject

class ConvertProjectStructureToDraggableItems @Inject constructor() {
    operator fun invoke(structure: ProjectStructure): Result<List<DraggableListItemData<ProjectStructureDraggableItem>>> {
        val draggableItems = mutableListOf<DraggableListItemData<ProjectStructureDraggableItem>>()
        var orderInList = 0

        // Add categories and their channels
        structure.categories.sortedBy { it.order }.forEach { category ->
            draggableItems.add(
                DraggableListItemData(
                    id = category.id,
                    originalData = ProjectStructureDraggableItem.CategoryDraggable(category),
                    depth = 0,
                    parentId = null,
                    canAcceptChildren = true,
                    maxRelativeChildDepth = 1 // Categories can have channels (depth 1 relative to category)
                )
            )
            orderInList++
            category.channels.sortedBy { it.projectSpecificData?.order ?: 0 }.forEach { channel ->
                draggableItems.add(
                    DraggableListItemData(
                        id = channel.id,
                        originalData = ProjectStructureDraggableItem.ChannelDraggable(channel, category.id),
                        depth = 1,
                        parentId = category.id,
                        canAcceptChildren = false,
                        maxRelativeChildDepth = 0
                    )
                )
                orderInList++
            }
        }

        // Add direct channels (as depth 0 items, distinct from categories)
        structure.directChannels.forEach { channel -> // Assuming direct channels don't have an explicit order field, use list order
            draggableItems.add(
                DraggableListItemData(
                    id = channel.id,
                    originalData = ProjectStructureDraggableItem.ChannelDraggable(channel, null),
                    depth = 0, // Direct channels are at root level
                    parentId = null, // Or a special root ID if needed to distinguish from categories
                    canAcceptChildren = false,
                    maxRelativeChildDepth = 0
                )
            )
            orderInList++
        }
        return Result.success(draggableItems)
    }
}
