// 경로: data/repository/ProjectStructureRepositoryImpl.kt
package com.example.data.repository

import com.example.data.datasource.local.projectstructure.ProjectStructureLocalDataSource
import com.example.data.datasource.remote.projectstructure.ProjectStructureRemoteDataSource
import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import com.example.domain.repository.ProjectStructureRepository
import com.example.domain.util.NetworkConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result

/**
 * ProjectStructureRepository 인터페이스의 구현체
 * 로컬 및 원격 데이터 소스를 조율하여 프로젝트 구조 데이터를 관리합니다.
 * 
 * @param remoteDataSource 프로젝트 구조 원격 데이터 소스
 * @param localDataSource 프로젝트 구조 로컬 데이터 소스
 * @param networkMonitor 네트워크 연결 상태 모니터
 */
@Singleton
class ProjectStructureRepositoryImpl @Inject constructor(
    private val remoteDataSource: ProjectStructureRemoteDataSource,
    private val localDataSource: ProjectStructureLocalDataSource,
    private val networkMonitor: NetworkConnectivityMonitor
) : ProjectStructureRepository {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * 새 카테고리를 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param name 카테고리 이름
     * @return 생성된 카테고리 결과
     */
    override suspend fun createCategory(projectId: String, name: String): Result<Category> {
        // 네트워크 연결 확인
        val isConnected = networkMonitor.isNetworkAvailable.first()
        
        if (!isConnected) {
            return Result.failure(Exception("네트워크 연결이 필요합니다."))
        }
        
        return try {
            // 원격에 카테고리 생성
            val result = remoteDataSource.createCategory(projectId, name)
            
            if (result.isSuccess) {
                val category = result.getOrThrow()
                // 로컬 캐시 업데이트
                localDataSource.saveCategory(category)
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 새 채널을 생성합니다.
     * 
     * @param categoryId 카테고리 ID
     * @param name 채널 이름
     * @param type 채널 타입
     * @return 생성된 채널 결과
     */
    override suspend fun createChannel(categoryId: String, name: String, type: ChannelType): Result<Channel> {
        // 카테고리 정보 조회하여 projectId 가져오기
        val category = localDataSource.getCategoryById(categoryId)
            ?: return Result.failure(Exception("카테고리 정보를 찾을 수 없습니다."))
        
        // 네트워크 연결 확인
        val isConnected = networkMonitor.isNetworkAvailable.first()
        
        if (!isConnected) {
            return Result.failure(Exception("네트워크 연결이 필요합니다."))
        }
        
        return try {
            // 원격에 채널 생성
            val result = remoteDataSource.createChannel(
                projectId = category.projectId,
                categoryId = categoryId,
                name = name,
                type = type
            )
            
            if (result.isSuccess) {
                val channel = result.getOrThrow()
                // 로컬 캐시 업데이트
                localDataSource.saveChannel(channel)
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 카테고리 상세 정보를 가져옵니다.
     * 
     * @param categoryId 카테고리 ID
     * @return 카테고리 상세 정보 결과
     */
    override suspend fun getCategoryDetails(categoryId: String): Result<Category> {
        // 먼저 로컬에서 카테고리 정보 확인
        val localCategory = localDataSource.getCategoryById(categoryId)
        
        // 로컬에 없는 경우 원격에서 가져오기 시도
        if (localCategory == null) {
            // 네트워크 연결 확인
            val isConnected = networkMonitor.isNetworkAvailable.first()
            
            if (!isConnected) {
                return Result.failure(Exception("카테고리 정보를 찾을 수 없고 네트워크 연결이 없습니다."))
            }
            
            return try {
                // projectId를 모르기 때문에 실제 구현에서는 카테고리 ID로 직접 조회 가능한 별도 API 필요
                // 여기서는 간단히 하기 위해 로컬에서 projectId를 알 수 없으면 실패로 처리
                Result.failure(Exception("카테고리 정보를 찾을 수 없습니다."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        // 로컬에서 찾은 경우 그대로 반환
        return Result.success(localCategory)
    }

    /**
     * 카테고리 정보를 수정합니다.
     * 
     * @param categoryId 카테고리 ID
     * @param newName 새 카테고리 이름
     * @return 작업 성공 여부
     */
    override suspend fun updateCategory(categoryId: String, newName: String): Result<Unit> {
        // 카테고리 정보 조회하여 projectId 가져오기
        val category = localDataSource.getCategoryById(categoryId)
            ?: return Result.failure(Exception("카테고리 정보를 찾을 수 없습니다."))
        
        // 네트워크 연결 확인
        val isConnected = networkMonitor.isNetworkAvailable.first()
        
        if (!isConnected) {
            return Result.failure(Exception("네트워크 연결이 필요합니다."))
        }
        
        return try {
            // 원격에서 카테고리 정보 업데이트
            val result = remoteDataSource.updateCategory(
                projectId = category.projectId,
                categoryId = categoryId,
                newName = newName
            )
            
            if (result.isSuccess) {
                // 로컬 캐시 업데이트
                val updatedCategory = category.copy(name = newName)
                localDataSource.saveCategory(updatedCategory)
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 채널 상세 정보를 가져옵니다.
     * 
     * @param channelId 채널 ID
     * @return 채널 상세 정보 결과
     */
    override suspend fun getChannelDetails(channelId: String): Result<Channel> {
        // 먼저 로컬에서 채널 정보 확인
        val localChannel = localDataSource.getChannelById(channelId)
        
        // 로컬에 없는 경우 원격에서 가져오기 시도
        if (localChannel == null) {
            // 네트워크 연결 확인
            val isConnected = networkMonitor.isNetworkAvailable.first()
            
            if (!isConnected) {
                return Result.failure(Exception("채널 정보를 찾을 수 없고 네트워크 연결이 없습니다."))
            }
            
            return try {
                // projectId와 categoryId를 모르기 때문에 실제 구현에서는 
                // 채널 ID로 직접 조회 가능한 별도 API 필요
                // 여기서는 간단히 하기 위해 로컬에서 정보를 알 수 없으면 실패로 처리
                Result.failure(Exception("채널 정보를 찾을 수 없습니다."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        // 로컬에서 찾은 경우 그대로 반환
        return Result.success(localChannel)
    }

    /**
     * 채널 정보를 수정합니다.
     * 
     * @param channelId 채널 ID
     * @param newName 새 채널 이름
     * @param newType 새 채널 타입
     * @return 작업 성공 여부
     */
    override suspend fun updateChannel(channelId: String, newName: String, newType: ChannelType): Result<Unit> {
        // 채널 정보 조회하여 projectId와 categoryId 가져오기
        val channel = localDataSource.getChannelById(channelId)
            ?: return Result.failure(Exception("채널 정보를 찾을 수 없습니다."))
        
        // 네트워크 연결 확인
        val isConnected = networkMonitor.isNetworkAvailable.first()
        
        if (!isConnected) {
            return Result.failure(Exception("네트워크 연결이 필요합니다."))
        }
        
        return try {
            // 원격에서 채널 정보 업데이트
            val result = remoteDataSource.updateChannel(
                projectId = channel.projectId,
                categoryId = channel.categoryId,
                channelId = channelId,
                newName = newName,
                newType = newType
            )
            
            if (result.isSuccess) {
                // 로컬 캐시 업데이트
                val updatedChannel = channel.copy(name = newName, type = newType)
                localDataSource.saveChannel(updatedChannel)
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}