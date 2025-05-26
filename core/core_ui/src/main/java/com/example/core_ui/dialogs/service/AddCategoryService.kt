package com.example.core_ui.dialogs.service

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.Category
import kotlinx.coroutines.flow.update
import java.util.UUID

class AddCategoryService {
    var isInitialized: Boolean = false;
    private var currentCategories: MutableList<Category> = mutableListOf()
    private var projectId: String = ""

    fun initialize(currentCategories: List<Category>, projectId: String){
        this.currentCategories = currentCategories.toMutableList()
        this.projectId = projectId
        isInitialized = true;
    }

    operator fun invoke () : Result<List<Category>>{
        isInitialized.takeIf { !it }?.let {
            return Result.failure(Exception("Service not initialized"))
        }

        val newCategoryId = UUID.randomUUID().toString()
        val now = DateTimeUtil.nowInstant()
        val newCategory = Category(
            id = newCategoryId,
            projectId = projectId,
            name = "새 카테고리",
            order = currentCategories.size, // 새 카테고리는 마지막 순서
            channels = emptyList(),
            createdAt = now,
            updatedAt = now,
            // createdBy, updatedBy는 현재 ViewModel에서 알 수 없으므로 null 또는 기본값 처리
            createdBy = null,
            updatedBy = null
        )

        val updatedCategories = currentCategories.toMutableList().apply {
            add(newCategory)
        }

        return Result.success(updatedCategories)
    }
}
