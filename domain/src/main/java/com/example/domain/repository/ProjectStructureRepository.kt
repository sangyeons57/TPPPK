// 경로: domain/repository/ProjectStructureRepository.kt (신규 생성)
package com.example.domain.repository

import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import kotlin.Result

interface ProjectStructureRepository {
    /** 카테고리 생성 */
    suspend fun createCategory(projectId: String, name: String): Result<Category>
    /** 채널 생성 */
    suspend fun createChannel(categoryId: String, name: String, type: ChannelType): Result<Channel>
    /** 카테고리 상세 정보 (필요시) */
    suspend fun getCategoryDetails(categoryId: String): Result<Category>
    /** 카테고리 수정 */
    suspend fun updateCategory(categoryId: String, newName: String): Result<Unit>
    /** 카테고리 삭제 (ProjectSettingRepo와 중복될 수 있으므로 역할 분담 필요) */
    // suspend fun deleteCategory(categoryId: String): Result<Unit>
    /** 채널 상세 정보 (필요시) */
    suspend fun getChannelDetails(channelId: String): Result<Channel>
    /** 채널 수정 */
    suspend fun updateChannel(channelId: String, newName: String, newType: ChannelType): Result<Unit>
    /** 채널 삭제 (ProjectSettingRepo와 중복될 수 있으므로 역할 분담 필요) */
    // suspend fun deleteChannel(channelId: String): Result<Unit>
}