
package com.example.data.datasource._remote

import com.example.data.model._remote.DMWrapperDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.dataObjects
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DMWrapperRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : DMWrapperRemoteDataSource {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val DM_WRAPPER_COLLECTION = "dm_wrapper"
        private const val LAST_MESSAGE_TIMESTAMP_FIELD = "lastMessageTimestamp"
    }
    
    override fun observeDmWrappers(): Flow<List<DMWrapperDTO>> {
        val uid = auth.currentUser?.uid
            ?: return kotlinx.coroutines.flow.flow { throw Exception("User not logged in.") }

        // DMWrapper는 주로 목록을 보여주기 위한 읽기 전용 데이터입니다.
        // 이 데이터의 생성/수정은 보통 메시지가 전송될 때 서버(Cloud Functions)에서
        // 트랜잭션을 통해 안전하게 처리하는 것이 일반적입니다.
        // 따라서 클라이언트에서는 이 목록을 관찰하는 기능만 구현합니다.
        return firestore.collection(USERS_COLLECTION).document(uid)
            .collection(DM_WRAPPER_COLLECTION)
            .orderBy(LAST_MESSAGE_TIMESTAMP_FIELD, Query.Direction.DESCENDING) // 최신 메시지 순으로 정렬
            .dataObjects() // Flow<List<DMWrapperDTO>> 반환
    }
}

