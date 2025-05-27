
package com.example.data.datasource.remote

import com.example.data.model._remote.InviteDTO
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.dataObjects
import com.google.firebase.firestore.ktx.toObject
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
            val newDocumentRef = getInvitesCollection(projectId).document()
            
            val newInvite = InviteDTO(
                id = newDocumentRef.id,
                inviteCode = UUID.randomUUID().toString().substring(0, 8).uppercase(),
                createdBy = uid,
                expiresAt = expirationDate,
                status = "ACTIVE" // 생성 시 기본 상태는 ACTIVE
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

    override suspend fun getInviteByCode(
        projectId: String,
        inviteCode: String
    ): Result<InviteDTO?> = withContext(Dispatchers.IO) {
        resultTry {
            val querySnapshot = getInvitesCollection(projectId)
                .whereEqualTo("inviteCode", inviteCode)
                .limit(1)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                querySnapshot.documents.firstOrNull()?.toObject<InviteDTO>()
            } else {
                null
            }
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

