
package com.example.data.datasource.remote

import com.example.data.model._remote.ProjectsWrapperDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.dataObjects
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectsWrapperRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ProjectsWrapperRemoteDataSource {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val PROJECTS_WRAPPER_COLLECTION = "projects_wrapper"
    }

    override fun observeProjectsWrappers(): Flow<List<ProjectsWrapperDTO>> {
        val uid = auth.currentUser?.uid
            ?: return kotlinx.coroutines.flow.flow { throw Exception("User not logged in.") }
        
        // ProjectsWrapper 데이터의 생성/삭제는 사용자가 프로젝트에 참여하거나 나갈 때
        // 서버(Cloud Functions)에서 처리하는 것이 데이터 정합성에 안전합니다.
        // 따라서 클라이언트에서는 이 목록을 관찰하는 기능만 구현합니다.
        return firestore.collection(USERS_COLLECTION).document(uid)
            .collection(PROJECTS_WRAPPER_COLLECTION)
            .dataObjects()
    }
}

