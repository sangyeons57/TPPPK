package com.example.data_model.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Local Entity for Categories collection
 * 1:1 correspondence with Firestore 'categories' collection
 * Pure domain/data model - sync logic handled by separate Outbox pattern
 */
@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["projectId", "order"]), // For project-based ordering
        Index(value = ["createdBy"]), // For creator-based queries
        Index(value = ["isCategory"]), // For category type filtering
        Index(value = ["name"]), // For category search
        Index(value = ["updatedAt"]), // For incremental sync
    ]
)
data class CategoriesEntity(
    @PrimaryKey
    val id: String,

    /**
     * Project ID this category belongs to
     */
    val projectId: String,

    /**
     * Category name
     */
    val name: String,

    /**
     * Display order within project
     */
    val order: Int = 0,

    /**
     * User ID who created this category
     */
    val createdBy: String,

    /**
     * Whether this is actually a category (true) or special "NoCategory" (false)
     */
    val isCategory: Boolean = true,

    // === Base Firestore Fields ===

    /**
     * Record creation time (UTC)
     */
    val createdAt: Instant,

    /**
     * Last modification time in Firestore (UTC)
     * 🔑 Key field for incremental synchronization
     */
    val updatedAt: Instant
) {
    companion object {
        /**
         * Special category ID for channels that don't belong to any specific category
         */
        const val NO_CATEGORY_ID = "NoCategory"
    }
}