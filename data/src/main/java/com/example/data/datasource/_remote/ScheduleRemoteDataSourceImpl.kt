
package com.example.data.datasource._remote

import com.example.data.model._remote.ScheduleDTO
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.dataObjects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ScheduleRemoteDataSource {

    companion object {
        private const val SCHEDULES_COLLECTION = "schedules"
    }

    private val schedulesCollection = firestore.collection(SCHEDULES_COLLECTION)

    override fun getSchedulesForProject(
        projectId: String,
        startAt: Timestamp,
        endAt: Timestamp
    ): Flow<List<ScheduleDTO>> {
        // 참고: 이 쿼리를 사용하려면 Firestore에서 (projectId, startTime)에 대한
        // 복합 색인(composite index) 생성이 필요합니다.
        return schedulesCollection
            .whereEqualTo("projectId", projectId)
            .whereGreaterThanOrEqualTo("startTime", startAt)
            .whereLessThanOrEqualTo("startTime", endAt) // endTime으로 필터링하는 것이 더 정확할 수 있으나, startTime 기준으로 조회
            .dataObjects()
    }

    override suspend fun getSchedule(scheduleId: String): Result<ScheduleDTO?> = withContext(Dispatchers.IO) {
        resultTry {
            if (scheduleId.isBlank()) {
                throw IllegalArgumentException("Schedule ID cannot be empty.")
            }
            val document = schedulesCollection.document(scheduleId).get().await()
            document.toObject(ScheduleDTO::class.java)
        }
    }

    override suspend fun createSchedule(schedule: ScheduleDTO): Result<String> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
            // 생성자 ID와 생성 시간을 주입 (DTO에 ServerTimestamp가 있으므로 Firestore에서 자동 설정됨)
            val newScheduleWithCreator = schedule.copy(creatorId = uid)
            val docRef = schedulesCollection.add(newScheduleWithCreator).await()
            docRef.id
        }
    }

    override suspend fun updateSchedule(schedule: ScheduleDTO): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            if (schedule.id.isBlank()) {
                throw IllegalArgumentException("Schedule ID cannot be empty for an update.")
            }
            // 업데이트할 필드만 Map으로 만들어 전달하여 createdAt 같은 불변 필드를 보호하고
            // updatedAt 타임스탬프를 확실하게 찍습니다.
            val updateData = mutableMapOf<String, Any?>()
            updateData["title"] = schedule.title
            updateData["content"] = schedule.content
            updateData["startTime"] = schedule.startTime
            updateData["endTime"] = schedule.endTime
            updateData["projectId"] = schedule.projectId // projectId도 업데이트 가능하도록 유지 (선택사항)
            updateData["status"] = schedule.status
            updateData["color"] = schedule.color
            updateData["updatedAt"] = FieldValue.serverTimestamp()
            
            schedulesCollection.document(schedule.id).update(updateData).await()
            Unit
        }
    }

    override suspend fun deleteSchedule(scheduleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            schedulesCollection.document(scheduleId).delete().await()
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

