package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.repository.CategoryCollectionRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.ProjectChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * CategoryCollectionRepository 인터페이스의 구현체
 * CategoryRepository와 ProjectChannelRepository를 조합하여 카테고리와 채널을 함께 관리합니다.
 */
class CategoryCollectionRepositoryImpl @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val projectChannelRepository: ProjectChannelRepository
) : CategoryCollectionRepository {

    /**
     * 특정 프로젝트의 모든 카테고리와 각 카테고리에 속한 채널 목록을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return Flow<CustomResult<List<CategoryCollection>, Exception>> 카테고리 컬렉션 목록을 포함한 결과
     */
    override suspend fun getCategoryCollections(projectId: String): Flow<CustomResult<List<CategoryCollection>, Exception>> {
        return flow {
            try {
                // 카테고리 목록을 가져옵니다
                val categoriesResult = categoryRepository.getCategoriesStream(projectId).first()
                
                when (categoriesResult) {
                    is CustomResult.Success -> {
                        val categoryList = categoriesResult.data
                        val categoryCollections = mutableListOf<CategoryCollection>()
                        
                        // 각 카테고리에 대해 채널 목록을 가져옵니다
                        for (category in categoryList) {
                            try {
                                val channelsResult = projectChannelRepository.getProjectChannelsByCategoryStream(
                                    projectId,
                                    category.id
                                ).first()
                                
                                val channels = when (channelsResult) {
                                    is CustomResult.Success -> channelsResult.data
                                    else -> emptyList()
                                }
                                
                                categoryCollections.add(
                                    CategoryCollection(
                                        category = category,
                                        channels = channels
                                    )
                                )
                            } catch (e: Exception) {
                                // 채널 가져오기 실패 시 빈 목록으로 처리
                                categoryCollections.add(
                                    CategoryCollection(
                                        category = category,
                                        channels = emptyList()
                                    )
                                )
                            }
                        }
                        
                        emit(CustomResult.Success(categoryCollections))
                    }
                    else -> {
                        emit(CustomResult.Failure(Exception("카테고리 목록을 가져오는데 실패했습니다.")))
                    }
                }
            } catch (e: Exception) {
                emit(CustomResult.Failure(e))
            }
        }
    }

    /**
     * 카테고리 컬렉션 목록을 업데이트합니다.
     * 이 메서드는 카테고리와 채널의 구조적 변경(순서 변경, 부모-자식 관계 변경 등)을 처리합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param collections 업데이트할 카테고리 컬렉션 목록
     * @return CustomResult<Unit, Exception> 업데이트 결과
     */
    override suspend fun updateCategoryCollections(
        projectId: String, 
        collections: List<CategoryCollection>
    ): CustomResult<Unit, Exception> {
        try {
            // 카테고리 목록 추출 및 업데이트
            val categories = collections.map { it.category }
            val updateCategoriesResult = categoryRepository.updateCategories(projectId, categories)
            
            if (updateCategoriesResult is CustomResult.Failure) {
                return CustomResult.Failure(updateCategoriesResult.error)
            }
            
            // 각 카테고리의 채널 목록 업데이트
            for (collection in collections) {
                for (channel in collection.channels) {
                    val updateChannelResult = projectChannelRepository.updateProjectChannel(
                        projectId, 
                        channel
                    )
                    
                    if (updateChannelResult is CustomResult.Failure) {
                        return CustomResult.Failure(updateChannelResult.error)
                    }
                }
            }
            
            return CustomResult.Success(Unit)
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }

    /**
     * 특정 카테고리에 새 채널을 추가합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channel 추가할 채널 정보
     * @return CustomResult<CategoryCollection, Exception> 업데이트된 카테고리 컬렉션
     */
    override suspend fun addChannelToCategory(
        projectId: String, 
        categoryId: String, 
        channel: ProjectChannel
    ): CustomResult<CategoryCollection, Exception> {
        try {
            // 채널 추가
            val addChannelResult = projectChannelRepository.addProjectChannel(projectId, channel)
            
            if (addChannelResult is CustomResult.Failure) {
                return CustomResult.Failure(addChannelResult.error)
            }
            
            // 업데이트된 카테고리 컬렉션 반환
            val categoryResult = categoryRepository.getCategory(projectId, categoryId)
            
            return when (categoryResult) {
                is CustomResult.Success -> {
                    val channelsResult = projectChannelRepository.getProjectChannelsByCategoryStream(
                        projectId, 
                        categoryId
                    ).first()
                    
                    when (channelsResult) {
                        is CustomResult.Success -> {
                            CustomResult.Success(
                                CategoryCollection(
                                    category = categoryResult.data,
                                    channels = channelsResult.data
                                )
                            )
                        }
                        else -> CustomResult.Failure(Exception("채널 목록을 가져오는데 실패했습니다."))
                    }
                }
                else -> CustomResult.Failure(Exception("카테고리 정보를 가져오는데 실패했습니다."))
            }
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }

    /**
     * 특정 카테고리에서 채널을 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 삭제할 채널 ID
     * @return CustomResult<CategoryCollection, Exception> 업데이트된 카테고리 컬렉션
     */
    override suspend fun removeChannelFromCategory(
        projectId: String, 
        categoryId: String, 
        channelId: String
    ): CustomResult<CategoryCollection, Exception> {
        try {
            // 채널 삭제
            val deleteChannelResult = projectChannelRepository.deleteProjectChannel(projectId, channelId)
            
            if (deleteChannelResult is CustomResult.Failure) {
                return CustomResult.Failure(deleteChannelResult.error)
            }
            
            // 업데이트된 카테고리 컬렉션 반환
            val categoryResult = categoryRepository.getCategory(projectId, categoryId)
            
            return when (categoryResult) {
                is CustomResult.Success -> {
                    val channelsResult = projectChannelRepository.getProjectChannelsByCategoryStream(
                        projectId, 
                        categoryId
                    ).first()
                    
                    when (channelsResult) {
                        is CustomResult.Success -> {
                            CustomResult.Success(
                                CategoryCollection(
                                    category = categoryResult.data,
                                    channels = channelsResult.data
                                )
                            )
                        }
                        else -> CustomResult.Failure(Exception("채널 목록을 가져오는데 실패했습니다."))
                    }
                }
                else -> CustomResult.Failure(Exception("카테고리 정보를 가져오는데 실패했습니다."))
            }
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }

    /**
     * 채널을 한 카테고리에서 다른 카테고리로 이동합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelId 이동할 채널 ID
     * @param sourceCategoryId 원본 카테고리 ID
     * @param targetCategoryId 대상 카테고리 ID
     * @param newOrder 새 순서 (대상 카테고리 내에서의 위치)
     * @return CustomResult<List<CategoryCollection>, Exception> 업데이트된 카테고리 컬렉션 목록
     */
    override suspend fun moveChannelBetweenCategories(
        projectId: String,
        channelId: String,
        sourceCategoryId: String,
        targetCategoryId: String,
        newOrder: Int
    ): CustomResult<List<CategoryCollection>, Exception> {
        try {
            // 채널 정보 가져오기
            val channelResult = projectChannelRepository.getProjectChannelStream(projectId, channelId).first()
            
            if (channelResult !is CustomResult.Success) {
                return CustomResult.Failure(Exception("채널 정보를 가져오는데 실패했습니다."))
            }
            
            val channel = channelResult.data
            
            // 채널 업데이트 로직 구현 (카테고리 변경 및 순서 변경)
            // 실제 구현은 프로젝트의 채널 모델 구조에 따라 달라질 수 있습니다
            
            // 업데이트된 카테고리 컬렉션 목록 반환
            return getCategoryCollections(projectId).first()
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }

    /**
     * 카테고리 순서를 변경합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 이동할 카테고리 ID
     * @param newOrder 새 순서
     * @return CustomResult<List<CategoryCollection>, Exception> 업데이트된 카테고리 컬렉션 목록
     */
    override suspend fun moveCategoryOrder(
        projectId: String,
        categoryId: String,
        newOrder: Int
    ): CustomResult<List<CategoryCollection>, Exception> {
        try {
            // 카테고리 정보 가져오기
            val categoryResult = categoryRepository.getCategory(projectId, categoryId)
            
            if (categoryResult !is CustomResult.Success) {
                return CustomResult.Failure(Exception("카테고리 정보를 가져오는데 실패했습니다."))
            }
            
            val category = categoryResult.data
            
            // 카테고리 순서 변경 로직 구현
            // 실제 구현은 프로젝트의 카테고리 모델 구조에 따라 달라질 수 있습니다
            
            // 업데이트된 카테고리 컬렉션 목록 반환
            return getCategoryCollections(projectId).first()
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }

    /**
     * 새 카테고리를 추가합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param category 추가할 카테고리 정보
     * @return CustomResult<CategoryCollection, Exception> 추가된 카테고리 컬렉션
     */
    override suspend fun addCategory(
        projectId: String, 
        category: Category
    ): CustomResult<CategoryCollection, Exception> {
        try {
            // 카테고리 추가
            val addCategoryResult = categoryRepository.createCategory(projectId, category)
            
            if (addCategoryResult !is CustomResult.Success) {
                return CustomResult.Failure(Exception("카테고리 추가에 실패했습니다."))
            }
            
            val categoryId = addCategoryResult.data
            
            // 추가된 카테고리 정보 가져오기
            val categoryResult = categoryRepository.getCategory(projectId, categoryId)
            
            return when (categoryResult) {
                is CustomResult.Success -> {
                    CustomResult.Success(
                        CategoryCollection(
                            category = categoryResult.data,
                            channels = emptyList()
                        )
                    )
                }
                else -> CustomResult.Failure(Exception("추가된 카테고리 정보를 가져오는데 실패했습니다."))
            }
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }

    /**
     * 카테고리를 삭제합니다. 카테고리에 속한 모든 채널도 함께 삭제됩니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 삭제할 카테고리 ID
     * @return CustomResult<Unit, Exception> 삭제 결과
     */
    override suspend fun removeCategory(
        projectId: String, 
        categoryId: String
    ): CustomResult<Unit, Exception> {
        try {
            // 카테고리에 속한 채널 목록 가져오기
            val channelsResult = projectChannelRepository.getProjectChannelsByCategoryStream(
                projectId, 
                categoryId
            ).first()
            
            // 채널 삭제
            if (channelsResult is CustomResult.Success) {
                for (channel in channelsResult.data) {
                    projectChannelRepository.deleteProjectChannel(projectId, channel.id)
                }
            }
            
            // 카테고리 삭제
            return categoryRepository.deleteCategory(projectId, categoryId)
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }

    /**
     * 카테고리 이름을 변경합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param newName 새 이름
     * @return CustomResult<CategoryCollection, Exception> 업데이트된 카테고리 컬렉션
     */
    override suspend fun renameCategory(
        projectId: String, 
        categoryId: String, 
        newName: String
    ): CustomResult<CategoryCollection, Exception> {
        try {
            // 카테고리 정보 가져오기
            val categoryResult = categoryRepository.getCategory(projectId, categoryId)
            
            if (categoryResult !is CustomResult.Success) {
                return CustomResult.Failure(Exception("카테고리 정보를 가져오는데 실패했습니다."))
            }
            
            // 카테고리 이름 변경
            val updatedCategory = categoryResult.data.copy(name = newName)
            val updateResult = categoryRepository.updateCategory(projectId, updatedCategory)
            
            if (updateResult !is CustomResult.Success) {
                return CustomResult.Failure(Exception("카테고리 이름 변경에 실패했습니다."))
            }
            
            // 업데이트된 카테고리 컬렉션 반환
            val channelsResult = projectChannelRepository.getProjectChannelsByCategoryStream(
                projectId, 
                categoryId
            ).first()
            
            return when (channelsResult) {
                is CustomResult.Success -> {
                    CustomResult.Success(
                        CategoryCollection(
                            category = updatedCategory,
                            channels = channelsResult.data
                        )
                    )
                }
                else -> CustomResult.Failure(Exception("채널 목록을 가져오는데 실패했습니다."))
            }
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }

    /**
     * 채널 이름을 변경합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @param newName 새 이름
     * @return CustomResult<CategoryCollection, Exception> 업데이트된 카테고리 컬렉션
     */
    override suspend fun renameChannel(
        projectId: String, 
        categoryId: String, 
        channelId: String, 
        newName: String
    ): CustomResult<CategoryCollection, Exception> {
        try {
            // 채널 정보 가져오기
            val channelResult = projectChannelRepository.getProjectChannelStream(projectId, channelId).first()
            
            if (channelResult !is CustomResult.Success) {
                return CustomResult.Failure(Exception("채널 정보를 가져오는데 실패했습니다."))
            }
            
            // 채널 이름 변경
            val updatedChannel = channelResult.data.copy(channelName = newName)
            val updateResult = projectChannelRepository.updateProjectChannel(projectId, updatedChannel)
            
            if (updateResult !is CustomResult.Success) {
                return CustomResult.Failure(Exception("채널 이름 변경에 실패했습니다."))
            }
            
            // 업데이트된 카테고리 컬렉션 반환
            val categoryResult = categoryRepository.getCategory(projectId, categoryId)
            val channelsResult = projectChannelRepository.getProjectChannelsByCategoryStream(
                projectId, 
                categoryId
            ).first()
            
            return when {
                categoryResult is CustomResult.Success && channelsResult is CustomResult.Success -> {
                    CustomResult.Success(
                        CategoryCollection(
                            category = categoryResult.data,
                            channels = channelsResult.data
                        )
                    )
                }
                else -> CustomResult.Failure(Exception("카테고리 또는 채널 목록을 가져오는데 실패했습니다."))
            }
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }
}
