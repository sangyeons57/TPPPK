
package com.example.data.datasource._remote

import com.example.data.model._remote.InviteDTO
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.dataObjects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InviteRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : InviteRemoteDataSource {

    companion object {
        private const val PROJECTS_COLLECTION = "projects"
        private const val INVITES_COLLECTION = "invites"
    }

    private fun getInvitesCollection(projectId: String) =
        firestore.collection(PROJECTS_COLLECTION).document(projectId)
            .collection(INVITES_COLLECTION)

    override fun observeInvites(projectId: String): Flow<List<InviteDTO>> {
        return getInvitesCollection(projectId).dataObjects()
    }

    override suspend fun createInvite(
        projectId: String,
        expirationDate: Timestamp?
    ): Result<InviteDTO> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")

            // Firestore 문서 ID를 미리 생성하여 inviteId와 inviteCode로 활용
            val newDocumentRef = getInvitesCollection(projectId).document()
            
            val newInvite = InviteDTO(
                id = newDocumentRef.id,
                // 간단한 랜덤 코드를 생성, 실제로는 더 복잡한 코드 생성 로직이 필요할 수 있음
                inviteCode = UUID.randomUUID().toString().substring(0, 8),
                createdBy = uid,
                expiresAt = expirationDate
            )
            
            newDocumentRef.set(newInvite).await()
            newInvite
        }
    }

    override suspend fun updateInviteStatus(
        projectId: String,
        inviteId: String,
        newStatus: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            getInvitesCollection(projectId).document(inviteId)
                .update("status", newStatus).await()
            Unit
        }
    }

    private inline fun <T> resultTry(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Throwable) {
            if (e is java.util.concurrent.CancellationException) throw e
            Result.failure(e)
        }
    }
}

