package com.example.core_ui.dialogs.service

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.Category


class DeleteCategoryService {
    var isInitialized: Boolean = false;
    private var currentCategories: MutableList<Category> = mutableListOf()
    private var projectId: String = ""

    fun initialize(currentCategories: List<Category>, projectId: String){
        this.currentCategories = currentCategories.toMutableList()
        this.projectId = projectId
        isInitialized = true;
    }

    operator fun invoke(categoryId: String): Result<Boolean>{
        isInitialized.takeIf { !it }?.let {
            return Result.failure(Exception("Service not initialized"))
        }

        val now = DateTimeUtil.nowInstant()
        // 삭제 후 남은 카테고리들의 순서를 재정렬
        val updatedCategories = currentCategories
            .filter { it.id != categoryId }
            .mapIndexed { index, category ->
                category.copy(order = index, updatedAt = now)
            }
        
        currentCategories = updatedCategories.toMutableList()

        return Result.success(true)
    }

    fun getCategories(): List<Category> = currentCategories.toList()
}