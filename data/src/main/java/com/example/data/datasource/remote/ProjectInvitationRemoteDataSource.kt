package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.data.model.remote.ProjectInvitationDTO
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProjectInvitation 전용 데이터소스 인터페이스
 * DefaultDatasource를 확장하여 Firebase Functions 통합을 제공합니다.
 */
interface ProjectInvitationRemoteDataSource : DefaultDatasource {
    
    /**
     * Firebase Functions를 통해 초대 코드를 검증합니다.
     * 
     * @param inviteCode 초대 코드
     * @return 검증 결과가 포함된 데이터
     */
    suspend fun validateInviteCodeViaFunction(inviteCode: String): CustomResult<Map<String, Any?>, Exception>
    
    /**
     * Firebase Functions를 통해 초대 링크를 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param expiresInHours 만료 시간
     * @return 생성된 초대 링크 데이터
     */
    suspend fun generateInviteLinkViaFunction(
        projectId: String, 
        expiresInHours: Int
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * Firebase Functions를 통해 초대 코드를 사용해 프로젝트에 참여합니다.
     */
    suspend fun joinProjectWithInviteViaFunction(inviteCode: String): CustomResult<Map<String, Any?>, Exception>
    
    /**
     * Firebase Functions를 통해 초대 링크를 무효화합니다.
     * 
     * @param inviteCode 초대 코드
     * @return 무효화 결과
     */
    suspend fun revokeInviteLinkViaFunction(inviteCode: String): CustomResult<Map<String, Any?>, Exception>
}

/**
 * ProjectInvitation 데이터소스 구현체
 * DefaultDatasourceImpl을 확장하여 기본 CRUD 기능을 제공하고,
 * Firebase Functions 통합을 추가로 제공합니다.
 */
@Singleton
class ProjectInvitationRemoteDataSourceImpl @Inject constructor(
    firestore: FirebaseFirestore,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource
) : DefaultDatasourceImpl<ProjectInvitationDTO>( firestore, ProjectInvitationDTO::class.java), ProjectInvitationRemoteDataSource {

    override suspend fun validateInviteCodeViaFunction(inviteCode: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.validateInviteCode(inviteCode)
    }

    override suspend fun generateInviteLinkViaFunction(
        projectId: String, 
        expiresInHours: Int
    ): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.generateInviteLink(projectId, expiresInHours)
    }

    override suspend fun joinProjectWithInviteViaFunction(inviteCode: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.joinProjectWithInvite(inviteCode)
    }

    override suspend fun revokeInviteLinkViaFunction(inviteCode: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.revokeInviteLink(inviteCode)
    }
}