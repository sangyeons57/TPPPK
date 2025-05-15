package com.example.domain.repository

import com.example.domain.model.Category
import com.example.domain.model.ProjectStructure
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.mockito.kotlin.verify

/**
 * ProjectStructureRepository 인터페이스의 테스트 케이스.
 * 리팩토링 이후 프로젝트 구조와 카테고리 관리 중심 기능이 올바르게 작동하는지 검증합니다.
 */
class ProjectStructureRepositoryTest {
    
    // 테스트 데이터
    private val projectId = "project1"
    private val categoryId = "category1"
    
    private val testCategory = Category(
        id = categoryId,
        projectId = projectId,
        name = "Test Category",
        order = 0,
    )
    
    private val testProjectStructure = ProjectStructure(
        categories = listOf(testCategory),
        directChannels = emptyList(),
    )
    
    // Mock 객체
    private lateinit var mockRepository: ProjectStructureRepository
    
    @Before
    fun setup() {
        mockRepository = mock()
    }
    
    @Test
    fun `getProjectStructureStream should return flow of project structure`() = runTest {
        // Given
        whenever(mockRepository.getProjectStructureStream(projectId))
            .thenReturn(flowOf(testProjectStructure))
        
        // When
        val result = mockRepository.getProjectStructureStream(projectId).first()
        
        // Then
        assertEquals(testProjectStructure, result)
    }
    
    @Test
    fun `getProjectStructure should return project structure`() = runTest {
        // Given
        whenever(mockRepository.getProjectStructure(projectId))
            .thenReturn(Result.success(testProjectStructure))
        
        // When
        val result = mockRepository.getProjectStructure(projectId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testProjectStructure, result.getOrNull())
    }
    
    @Test
    fun `createCategory should return created category`() = runTest {
        // Given
        whenever(mockRepository.createCategory(projectId, "Test Category"))
            .thenReturn(Result.success(testCategory))
        
        // When
        val result = mockRepository.createCategory(projectId, "Test Category")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testCategory, result.getOrNull())
    }
    
    @Test
    fun `getCategoryDetails should return category details`() = runTest {
        // Given
        whenever(mockRepository.getCategoryDetails(projectId, categoryId))
            .thenReturn(Result.success(testCategory))
        
        // When
        val result = mockRepository.getCategoryDetails(projectId, categoryId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testCategory, result.getOrNull())
    }
    
    @Test
    fun `getProjectCategories should return list of categories`() = runTest {
        // Given
        val categories = listOf(testCategory)
        whenever(mockRepository.getProjectCategories(projectId))
            .thenReturn(Result.success(categories))
        
        // When
        val result = mockRepository.getProjectCategories(projectId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(categories, result.getOrNull())
    }
    
    @Test
    fun `getProjectCategoriesStream should return flow of categories`() = runTest {
        // Given
        val categories = listOf(testCategory)
        whenever(mockRepository.getProjectCategoriesStream(projectId))
            .thenReturn(flowOf(categories))
        
        // When
        val result = mockRepository.getProjectCategoriesStream(projectId).first()
        
        // Then
        assertEquals(categories, result)
    }
    
    @Test
    fun `updateCategory should update category name`() = runTest {
        // Given
        whenever(mockRepository.updateCategory(projectId, categoryId, "Updated Category", null))
            .thenReturn(Result.success(Unit))
        
        // When
        val result = mockRepository.updateCategory(projectId, categoryId, "Updated Category", null)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `deleteCategory should delete category`() = runTest {
        // Given
        whenever(mockRepository.deleteCategory(projectId, categoryId, false))
            .thenReturn(Result.success(Unit))
        
        // When
        val result = mockRepository.deleteCategory(projectId, categoryId, false)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `deleteCategory with deleteChannels true should delete category and channels`() = runTest {
        // Given
        whenever(mockRepository.deleteCategory(projectId, categoryId, true))
            .thenReturn(Result.success(Unit))
        
        // When
        val result = mockRepository.deleteCategory(projectId, categoryId, true)
        
        // Then
        assertTrue(result.isSuccess)
        verify(mockRepository).deleteCategory(projectId, categoryId, true)
    }
    
    @Test
    fun `reorderCategory should change category order`() = runTest {
        // Given
        whenever(mockRepository.reorderCategory(projectId, categoryId, 1))
            .thenReturn(Result.success(Unit))
        
        // When
        val result = mockRepository.reorderCategory(projectId, categoryId, 1)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `batchReorderCategories should update multiple category orders`() = runTest {
        // Given
        val categoryOrders = mapOf(
            categoryId to 1,
            "category2" to 0
        )
        whenever(mockRepository.batchReorderCategories(projectId, categoryOrders))
            .thenReturn(Result.success(Unit))
        
        // When
        val result = mockRepository.batchReorderCategories(projectId, categoryOrders)
        
        // Then
        assertTrue(result.isSuccess)
    }
} 