
package com.example.data.datasource.remote

import com.example.data.model._remote.ProjectsWrapperDTO
import kotlinx.coroutines.flow.Flow

interface ProjectsWrapperRemoteDataSource {

    /**
     * 현재 로그인한 사용자가 속한 프로젝트 요약 정보 목록을 실시간으로 관찰합니다.
     */
    fun observeProjectsWrappers(): Flow<List<ProjectsWrapperDTO>>

}

