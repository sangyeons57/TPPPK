
package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.data.model.remote.ProjectsWrapperDTO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

interface ProjectsWrapperRemoteDataSource {

    /**
     * 현재 로그인한 사용자가 참여하고 있는 프로젝트의 ID 목록을 실시간으로 관찰합니다.
     * @param uid 사용자 ID
     * @return 프로젝트 ID 목록을 담은 Flow
     */
    fun observeProjectsWrappers(uid: String): Flow<List<String>>

    /**
     *
     */

    // 사용자의 프로젝트 목록에 새 프로젝트를 추가합니다. (ProjectWrapper 생성)
    // projectWrapper DTO는 이제 projectId 필드만 가집니다.
    suspend fun addProjectToUser(uid: String, projectId: String, projectWrapper: ProjectsWrapperDTO) : CustomResult<Unit, Exception>

    // 특정 프로젝트 래퍼를 삭제하는 함수
    suspend fun removeProjectFromUser(uid: String, projectId: String) : CustomResult<Unit, Exception>
}

