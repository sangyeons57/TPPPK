package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.InviteDTO
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 초대 정보에 접근하기 위한 인터페이스입니다.
 * DefaultDatasource를 확장하여 특정 프로젝트의 초대 문서에 대한 CRUD 및 관찰 기능을 제공합니다.
 * 초대 데이터는 `projects/{projectId}/invites/{inviteId}` 경로에 저장되므로,
 * 모든 작업 전에 `setCollection(projectId)`를 호출하여 프로젝트 컨텍스트를 설정해야 합니다.
 */
interface InviteRemoteDataSource : DefaultDatasource<InviteDTO> {

    /**
     * 프로젝트 ID와 초대 코드로 특정 초대 정보를 가져옵니다.
     * **중요:** 이 메서드를 호출하기 전에 `setCollection(projectId)`를 통해 프로젝트 컨텍스트를 설정해야 합니다.
     * @param projectId 대상 프로젝트의 ID. 이 ID는 `setCollection`에 전달된 ID와 일치해야 합니다.
     * @param inviteCode 조회할 초대 코드.
     * @return 조회된 초대 정보 DTO 또는 null(초대 코드가 없거나 오류 발생 시)을 포함한 CustomResult.
     * @throws IllegalStateException `setCollection(projectId)`가 호출되지 않았거나 `projectId`가 일치하지 않는 경우.
     */
    suspend fun findByCode(projectId: String, inviteCode: String): CustomResult<InviteDTO?, Exception>
}

@Singleton
class InviteRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<InviteDTO>(firestore, InviteDTO::class.java), InviteRemoteDataSource {

    private var currentProjectId: String? = null


    override suspend fun findByCode(projectId: String, inviteCode: String): CustomResult<InviteDTO?, Exception> {
        setCollection(projectId)
        return resultTry {
            val querySnapshot = collection
                .whereEqualTo(InviteDTO.INVITE_LINK, inviteCode)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                querySnapshot.documents.firstOrNull()?.toObject(InviteDTO::class.java)
            } else {
                null
            }
        }
    }
}
