package com.example.data_datasource.remote

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data_datasource.remote.special.DefaultDatasource
import com.example.data_datasource.remote.special.DefaultDatasourceImpl
import com.example.data_model.remote.MemberDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 멤버 정보에 접근하기 위한 인터페이스입니다.
 * DefaultDatasource를 확장하여 특정 프로젝트의 멤버 문서에 대한 CRUD 및 관찰 기능을 제공합니다.
 * 멤버 데이터는 `projects/{projectId}/members/{userId}` 경로에 저장되며, `userId`가 문서 ID가 됩니다.
 * 모든 작업 전에 `setCollection(projectId)`를 호출하여 프로젝트 컨텍스트를 설정해야 합니다.
 */
interface MemberRemoteDataSource : DefaultDatasource<MemberDTO> {
    /**
     * ACTIVE 상태의 멤버들만 조회합니다.
     */
    suspend fun findAllActiveMembers(source: Source = Source.DEFAULT): CustomResult<List<MemberDTO>, Exception>

    /**
     * ACTIVE 상태의 멤버들을 실시간으로 관찰합니다.
     */
    fun observeActiveMembers(): Flow<CustomResult<List<MemberDTO>, Exception>>

    /**
     * BLOCKED 상태의 멤버들만 조회합니다.
     */
    suspend fun findAllBlockedMembers(source: Source = Source.DEFAULT): CustomResult<List<MemberDTO>, Exception>

    /**
     * BLOCKED 상태의 멤버들을 실시간으로 관찰합니다.
     */
    fun observeBlockedMembers(): Flow<CustomResult<List<MemberDTO>, Exception>>
}

@Singleton
class MemberRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<MemberDTO>(firestore), MemberRemoteDataSource {
    override val dtoClass = MemberDTO::class.java

    override suspend fun findAllActiveMembers(source: Source): CustomResult<List<MemberDTO>, Exception> =
        withContext(Dispatchers.IO) {
            checkCollectionInitialized("findAllActiveMembers")
            resultTry {
                // status가 없거나 "active"인 모든 멤버 조회
                val snapshot = collection.get(source).await()
                snapshot.documents.mapNotNull { document ->
                    document.toDtoSafely()?.let { dto ->
                        // status가 없거나 "active"인 경우만 포함
                        val status = document.getString("status")
                        when (status?.lowercase()) {
                            null, "", "active" -> dto // status가 없거나 active인 경우
                            "banned" -> dto // 기존 banned 데이터도 active로 처리
                            else -> null // blocked, leave는 제외
                        }
                    }
                }
            }
        }

    override fun observeActiveMembers(): Flow<CustomResult<List<MemberDTO>, Exception>> =
        callbackFlow {
            checkCollectionInitialized("observeActiveMembers")

            // 초기 로딩 상태 전송
            trySend(CustomResult.Loading)

            val listener = collection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(CustomResult.Failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        val dtos = snapshot.documents.mapNotNull { document ->
                            document.toDtoSafely()?.let { dto ->
                                // status가 없거나 "active"인 경우만 포함
                                val status = document.getString("status")
                                when (status?.lowercase()) {
                                    null, "", "active" -> dto // status가 없거나 active인 경우
                                    "banned" -> dto // 기존 banned 데이터도 active로 처리
                                    else -> null // blocked, leave는 제외
                                }
                            }
                        }
                        trySend(CustomResult.Success(dtos))
                    } catch (e: Exception) {
                        trySend(CustomResult.Failure(e))
                    }
                } else {
                    trySend(CustomResult.Success(emptyList()))
                }
            }

            awaitClose { listener.remove() }
        }

    override suspend fun findAllBlockedMembers(source: Source): CustomResult<List<MemberDTO>, Exception> =
        withContext(Dispatchers.IO) {
            checkCollectionInitialized("findAllBlockedMembers")
            resultTry {
                // blocked 상태의 멤버들만 조회
                val snapshot = collection.get(source).await()
                snapshot.documents.mapNotNull { document ->
                    document.toDtoSafely()?.let { dto ->
                        // blocked 상태만 포함
                        val status = document.getString("status")
                        when (status?.lowercase()) {
                            "blocked" -> dto // blocked인 경우만 포함
                            else -> null // 다른 상태는 제외
                        }
                    }
                }
            }
        }

    override fun observeBlockedMembers(): Flow<CustomResult<List<MemberDTO>, Exception>> =
        callbackFlow {
            checkCollectionInitialized("observeBlockedMembers")

            // 초기 로딩 상태 전송
            trySend(CustomResult.Loading)

            val listener = collection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(CustomResult.Failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        val dtos = snapshot.documents.mapNotNull { document ->
                            document.toDtoSafely()?.let { dto ->
                                // blocked 상태만 포함
                                val status = document.getString("status")
                                when (status?.lowercase()) {
                                    "blocked" -> dto // blocked인 경우만 포함
                                    else -> null // 다른 상태는 제외
                                }
                            }
                        }
                        trySend(CustomResult.Success(dtos))
                    } catch (e: Exception) {
                        trySend(CustomResult.Failure(e))
                    }
                } else {
                    trySend(CustomResult.Success(emptyList()))
                }
            }

            awaitClose { listener.remove() }
        }
}
