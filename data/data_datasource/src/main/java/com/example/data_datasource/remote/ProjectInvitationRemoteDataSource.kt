package com.example.data_datasource.remote

import com.example.data_datasource.remote.special.DefaultDatasource
import com.example.data_datasource.remote.special.DefaultDatasourceImpl
import com.example.data_datasource.remote.special.FunctionsRemoteDataSource
import com.example.data_model.remote.ProjectInvitationDTO
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProjectInvitation 전용 데이터소스 인터페이스
 * DefaultDatasource를 확장하여 Firebase Functions 통합을 제공합니다.
 */
interface ProjectInvitationRemoteDataSource : DefaultDatasource<ProjectInvitationDTO>

/**
 * ProjectInvitation 데이터소스 구현체
 * DefaultDatasourceImpl을 확장하여 기본 CRUD 기능을 제공하고,
 * Firebase Functions 통합을 추가로 제공합니다.
 */
@Singleton
class ProjectInvitationRemoteDataSourceImpl @Inject constructor(
    firestore: FirebaseFirestore,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource
) : DefaultDatasourceImpl<ProjectInvitationDTO>(firestore), ProjectInvitationRemoteDataSource {

    override val dtoClass = ProjectInvitationDTO::class.java

    // No additional functions; invites via code are removed
}
