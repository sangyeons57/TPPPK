
package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.model.remote.MemberDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.dataObjects
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MemberRemoteDataSource {

    companion object {
        private const val PROJECTS_COLLECTION = "projects"
        private const val MEMBERS_COLLECTION = "members"
    }
    
    private fun getMembersCollection(projectId: String) =
        firestore.collection(PROJECTS_COLLECTION).document(projectId)
            .collection(MEMBERS_COLLECTION)

    override fun observeMembers(projectId: String): Flow<List<MemberDTO>> {
        return getMembersCollection(projectId).snapshots()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toObject(MemberDTO::class.java) } }
    }
    
    override suspend fun getProjectMember(
        projectId: String,
        userId: String
    ): CustomResult<MemberDTO?, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            if (projectId.isBlank() || userId.isBlank()) {
                throw IllegalArgumentException("Project ID and User ID cannot be empty.")
            }
            val document = getMembersCollection(projectId).document(userId).get().await()
            document.toObject(MemberDTO::class.java)
        }
    }

    override suspend fun addMember(
        projectId: String,
        userId: String,
        roleIds: String
    ): CustomResult<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun addMember(
        projectId: String,
        userId: String,
        roleIds: List<String>
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            // MemberDTO에는 joinedAt (ServerTimestamp)과 roleId만 저장됩니다.
            // Class Diagram에 따르면 Members 엔티티의 문서 ID는 userId 입니다.
            val newMember = MemberDTO(roleIds = roleIds) // joinedAt은 DTO의 @ServerTimestamp로 자동 설정
            getMembersCollection(projectId).document(userId)
                .set(newMember).await()
            Unit
        }
    }

    override suspend fun updateMemberRole(
        projectId: String,
        userId: String,
        newRoleIds: List<String>
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val updateData = mapOf(
                FirestoreConstants.Project.Members.ROLE_ID to newRoleIds,
                // 역할 변경 시 joinedAt은 변경하지 않으므로, 업데이트 맵에 포함하지 않습니다.
                // 만약 joinedAt도 업데이트해야 한다면 FieldValue.serverTimestamp()를 사용할 수 있으나,
                // 보통 역할 변경 시 가입 시간은 유지됩니다.
            )
            getMembersCollection(projectId).document(userId)
                .update(updateData).await()
            Unit
        }
    }

    override suspend fun removeMember(
        projectId: String,
        userId: String
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            getMembersCollection(projectId).document(userId)
                .delete().await()
            Unit
        }
    }
}

