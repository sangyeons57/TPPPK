package com.example.core_ui.dialogs.service

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.Category
import kotlinx.coroutines.flow.update

class MoveCategoryService {
    var isInitialized: Boolean = false;
    private var currentCategories: MutableList<Category> = mutableListOf()

    fun initialize(initialCategories: List<Category>) {
        currentCategories = initialCategories.toMutableList()
    }

    suspend operator fun invoke(fromIndex : Int, toIndex : Int) : Result<Boolean>{
        isInitialized.takeIf { !it }?.let {
            return Result.failure(Exception("Service not initialized"))
        }

        if (fromIndex == toIndex) return Result.failure(Exception("fromIndex and toIndex are the same"))

        val movedCategory = currentCategories.removeAt(fromIndex)
        currentCategories.add(toIndex, movedCategory)

        // 순서 변경에 따른 order 필드 업데이트
        val now = DateTimeUtil.nowInstant()
        currentCategories = currentCategories.mapIndexed { index, category ->
            category.copy(order = index, updatedAt = now)
        }.toMutableList()

        return Result.success(true)
    }

    fun getCategoriesForUi(): List<Category> = currentCategories.toList()
}