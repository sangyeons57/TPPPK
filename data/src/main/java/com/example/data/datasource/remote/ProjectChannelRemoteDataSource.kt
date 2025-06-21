package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.ProjectChannelDTO
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

interface ProjectChannelRemoteDataSource : DefaultDatasource {
    // 현재 ProjectChannel은 기본 CRUD 외 특수 메서드가 없습니다.
}

@Singleton
class ProjectChannelRemoteDataSourceImpl @Inject constructor(
    val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<ProjectChannelDTO>(firestore, ProjectChannelDTO::class.java),
    ProjectChannelRemoteDataSource {
}


