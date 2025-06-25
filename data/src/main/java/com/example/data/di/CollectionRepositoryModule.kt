package com.example.data.di

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.repository.collection.CategoryCollectionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Singleton

/**
 * DI module for collection repositories
 * 
 * TODO: This is a temporary implementation. CategoryCollectionRepository should
 * be properly implemented in the data layer or refactored to use the factory pattern.
 */
@Module
@InstallIn(SingletonComponent::class)
object CollectionRepositoryModule {

    /**
     * Provides a temporary stub implementation of CategoryCollectionRepository.
     * 
     * TODO: Replace with proper implementation or refactor to use factory pattern.
     */
    @Provides
    @Singleton
    fun provideCategoryCollectionRepository(): CategoryCollectionRepository {
        return object : CategoryCollectionRepository {
            override suspend fun getCategoryCollections(projectId: String): Flow<CustomResult<List<CategoryCollection>, Exception>> {
                return flowOf(CustomResult.Success(emptyList()))
            }

            override suspend fun updateCategoryCollections(
                projectId: String,
                collections: List<CategoryCollection>
            ): CustomResult<Unit, Exception> {
                return CustomResult.Success(Unit)
            }

            override suspend fun addChannelToCategory(
                projectId: String,
                categoryId: String,
                channel: ProjectChannel
            ): CustomResult<CategoryCollection, Exception> {
                return CustomResult.Failure(Exception("Stub implementation"))
            }

            override suspend fun removeChannelFromCategory(
                projectId: String,
                categoryId: String,
                channelId: String
            ): CustomResult<CategoryCollection, Exception> {
                return CustomResult.Failure(Exception("Stub implementation"))
            }

            override suspend fun moveChannelBetweenCategories(
                projectId: String,
                channelId: String,
                sourceCategoryId: String,
                targetCategoryId: String,
                newOrder: Int
            ): CustomResult<List<CategoryCollection>, Exception> {
                return CustomResult.Failure(Exception("Stub implementation"))
            }

            override suspend fun moveCategoryOrder(
                projectId: String,
                categoryId: String,
                newOrder: Int
            ): CustomResult<List<CategoryCollection>, Exception> {
                return CustomResult.Failure(Exception("Stub implementation"))
            }

            override suspend fun addCategory(
                projectId: String,
                category: Category
            ): CustomResult<CategoryCollection, Exception> {
                return CustomResult.Failure(Exception("Stub implementation"))
            }

            override suspend fun removeCategory(
                projectId: String,
                categoryId: String
            ): CustomResult<Unit, Exception> {
                return CustomResult.Success(Unit)
            }

            override suspend fun renameCategory(
                projectId: String,
                categoryId: String,
                newName: String
            ): CustomResult<CategoryCollection, Exception> {
                return CustomResult.Failure(Exception("Stub implementation"))
            }

            override suspend fun renameChannel(
                projectId: String,
                categoryId: String,
                channelId: String,
                newName: String
            ): CustomResult<CategoryCollection, Exception> {
                return CustomResult.Failure(Exception("Stub implementation"))
            }
        }
    }
}