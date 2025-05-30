
package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.data.model.remote.ProjectsWrapperDTO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

interface ProjectsWrapperRemoteDataSource {

    /**
     * 현재 로그인한 사용자가 속한 프로젝트 요약 정보 목록을 실시간으로 관찰합니다.
     */
    fun observeProjectsWrappers(uid: String): Flow<List<ProjectsWrapperDTO>>

    /**
     *
     */

    // 새로운 프로젝트 래퍼를 추가하는 함수
    suspend fun addProjectToUser(uid: String, projectWrapper: ProjectsWrapperDTO, projectId: String) : CustomResult<Unit, Exception>

    // 특정 프로젝트 래퍼를 삭제하는 함수
    suspend fun removeProjectFromUser(uid: String, projectId: String) : CustomResult<Unit, Exception>
}

