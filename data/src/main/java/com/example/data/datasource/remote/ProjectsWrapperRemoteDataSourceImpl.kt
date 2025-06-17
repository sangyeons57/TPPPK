
package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.data.model.remote.ProjectsWrapperDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectsWrapperRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ProjectsWrapperRemoteDataSource {

    private fun getCollection(userId: String) : CollectionReference {
        return firestore.collection(FirestoreConstants.Collections.USERS).document(userId).collection(FirestoreConstants.Users.ProjectsWrappers.COLLECTION_NAME)
    }

    override fun observeProjectsWrappers(uid: String): Flow<List<String>> {
        // ProjectsWrapper 데이터의 생성/삭제는 사용자가 프로젝트에 참여하거나 나갈 때
        // 서버(Cloud Functions)에서 처리하는 것이 데이터 정합성에 안전합니다.
        // 따라서 클라이언트에서는 이 목록을 관찰하는 기능만 구현합니다.
        return getCollection(uid)
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.id } } // 문서 ID (projectId) 목록을 반환
    }

    // 새로운 프로젝트 래퍼를 추가하는 함수
    override suspend fun addProjectToUser(uid: String, projectId: String, projectWrapper: ProjectsWrapperDTO) : CustomResult<Unit, Exception> {
        // projectId를 문서 ID로 사용하여 추가
        return try {
            getCollection(uid)
                .document(projectId) // 특정 프로젝트 ID를 문서 이름으로 지정
                .set(projectWrapper) // 데이터 추가 또는 덮어쓰기
                .await() // 작업이 완료될 때까지 기다림
            CustomResult.Success(Unit)
        } catch (e : Exception){
            CustomResult.Failure(e)
        }
    }

    // 특정 프로젝트 래퍼를 삭제하는 함수
    override suspend fun removeProjectFromUser(uid: String, projectId: String) : CustomResult<Unit, Exception>{
        return try {
            getCollection(uid)
                .document(projectId) // 삭제할 문서(프로젝트) ID 지정
                .delete() // 문서 삭제
                .await() // 작업이 완료될 때까지 기다림
            CustomResult.Success(Unit)
        } catch (e : Exception) {
            CustomResult.Failure(e)
        }
    }
}

