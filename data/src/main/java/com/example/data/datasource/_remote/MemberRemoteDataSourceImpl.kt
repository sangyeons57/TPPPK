
package com.example.data.datasource._remote

import com.example.data.model._remote.MemberDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.dataObjects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
        return getMembersCollection(projectId).dataObjects()
    }
    
    override suspend fun getProjectMember(
        projectId: String,
        userId: String
    ): Result<MemberDTO?> = withContext(Dispatchers.IO) {
        resultTry {
            val document = getMembersCollection(projectId).document(userId).get().await()
            document.toObject(MemberDTO::class.java)
        }
    }

    override suspend fun addMember(
        projectId: String,
        userId: String,
        roleId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val newMember = MemberDTO(roleId = roleId)
            getMembersCollection(projectId).document(userId)
                .set(newMember).await()
            Unit
        }
    }

    override suspend fun updateMemberRole(
        projectId: String,
        userId: String,
        newRoleId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            getMembersCollection(projectId).document(userId)
                .update("roleId", newRoleId).await()
            Unit
        }
    }

    override suspend fun removeMember(
        projectId: String,
        userId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            getMembersCollection(projectId).document(userId)
                .delete().await()
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

