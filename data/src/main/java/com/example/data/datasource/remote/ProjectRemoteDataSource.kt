
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.data.model.remote.ProjectDTO
import kotlinx.coroutines.tasks.await

interface ProjectRemoteDataSource : DefaultDatasource {

}

@Singleton
class ProjectRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<ProjectDTO>(firestore, ProjectDTO::class.java),
    ProjectRemoteDataSource {

}

