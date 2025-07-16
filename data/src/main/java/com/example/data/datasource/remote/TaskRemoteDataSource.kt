package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.TaskDTO
import com.example.domain.model.vo.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 태스크 정보에 접근하기 위한 인터페이스입니다.
 * 통합된 task_container collection에서 task 타입 문서만 필터링하여 처리합니다.
 */
interface TaskRemoteDataSource : DefaultDatasource

@Singleton
class TaskRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<TaskDTO>(firestore, TaskDTO::class.java), TaskRemoteDataSource {
    
}