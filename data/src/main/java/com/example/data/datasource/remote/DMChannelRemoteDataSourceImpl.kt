
package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.model.remote.DMChannelDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
class DMChannelRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : DMChannelRemoteDataSource {

    // FirestoreConstants에서 정의된 상수 사용

    private val channelsCollection = firestore.collection(FirestoreConstants.Collections.DM_CHANNELS)

    override fun observeDMChannel(channelId: String): Flow<DMChannelDTO?> {
        return channelsCollection.document(channelId)
            .snapshots()
            .map { snapshot -> snapshot.toObject(DMChannelDTO::class.java) }
    }

    override suspend fun findOrCreateDMChannel(otherUserId: String): CustomResult<String, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val myUid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
            
            // 두 사용자 ID를 모두 포함하는 채널이 있는지 먼저 검색
            val existingChannel = channelsCollection
                .whereArrayContains("participants", myUid)
                .get().await()
                .documents
                .find { doc -> doc.toObject(DMChannelDTO::class.java)?.participants?.contains(otherUserId) == true }

            if (existingChannel != null) {
                // 채널이 이미 존재하면 해당 ID 반환
                existingChannel.id
            } else {
                // 채널이 없으면 새로 생성
                val newChannel = DMChannelDTO(
                    participants = listOf(myUid, otherUserId).sorted() // 항상 정렬하여 저장하면 중복 방지에 용이
                )
                val docRef = channelsCollection.add(newChannel).await()
                docRef.id
            }
        }
    }

    override suspend fun updateLastMessage(
        channelId: String,
        messagePreview: String
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val updateData = mapOf(
                "lastMessagePreview" to messagePreview,
                "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            channelsCollection.document(channelId).update(updateData).await()
            Unit
        }
    }


}

