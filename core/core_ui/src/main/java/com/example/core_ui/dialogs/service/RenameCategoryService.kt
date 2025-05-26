package com.example.core_ui.dialogs.service

import com.example.core_common.util.DateTimeUtil
import com.example.core_ui.dialogs.viewmodel.ContextMenuState
import com.example.domain.model.Category

class RenameCategoryService {
    var isInitialized: Boolean = false;
    private var currentCategories: MutableList<Category> = mutableListOf()
    private var projectId: String = ""

    fun initialize(currentCategories: List<Category>, projectId: String){
        this.currentCategories = currentCategories.toMutableList()
        this.projectId = projectId
        isInitialized = true;
    }

    operator fun invoke(categoryId: String, newName: String): Result<Category> {
        isInitialized.takeIf { !it }?.let {
            return Result.failure(Exception("Service not initialized"))
        }

        if (newName.isBlank()) return Result.failure(Exception("New name is blank"))
        var selectCategory: Category? = null;
        val now = DateTimeUtil.nowInstant()
        val updatedCategories = currentCategories.map { category ->
            if (category.id == categoryId) {
                selectCategory = category
                category.copy(name = newName, updatedAt = now)
            } else {
                category
            }
        }
        
        currentCategories = updatedCategories.toMutableList()
        if(selectCategory == null) return Result.failure(Exception("Category not found"))
        return Result.success(selectCategory)
    }
}