
package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.remote.ScheduleDTO
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.dataObjects
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
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
            .snapshots()
            .map { snapshot -> snapshot.toObjects(ScheduleDTO::class.java) }
    }

    override suspend fun getSchedule(scheduleId: String): CustomResult<ScheduleDTO, Exception> = resultTry {
        if (scheduleId.isBlank()) {
            throw IllegalArgumentException("Schedule ID cannot be empty.")
        }
        val document = schedulesCollection.document(scheduleId).get().await()
        document.toObject(ScheduleDTO::class.java) as ScheduleDTO
    }

    override suspend fun createSchedule(schedule: ScheduleDTO): CustomResult<String, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
            // 생성자 ID와 생성 시간을 주입 (DTO에 ServerTimestamp가 있으므로 Firestore에서 자동 설정됨)
            val newScheduleWithCreator = schedule.copy(creatorId = uid)
            val docRef = schedulesCollection.add(newScheduleWithCreator).await()
            docRef.id
        }
    }

    override suspend fun updateSchedule(schedule: ScheduleDTO): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
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

    override suspend fun deleteSchedule(scheduleId: String): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            schedulesCollection.document(scheduleId).delete().await()
            Unit
        }
    }

    override suspend fun getSchedulesForMonth(userId : String, yearMonth: YearMonth): Flow<CustomResult<List<ScheduleDTO>, Exception>> = callbackFlow {

        val startOfMonthTimestamp = DateTimeUtil.yearMonthToStartOfMonthTimestamp(yearMonth)
        val endOfMonthTimestamp = DateTimeUtil.yearMonthToEndOfMonthExclusiveTimestamp(yearMonth)

        val query = firestore.collection(FirestoreConstants.Collections.SCHEDULES) // agés FirestoreConstants.Collections.SCHEDULES 사용
            .whereEqualTo(FirestoreConstants.Schedule.CREATOR_ID, userId) // agés FirestoreConstants.Schedule.CREATOR_ID 사용
            .whereGreaterThanOrEqualTo(FirestoreConstants.Schedule.START_TIME, startOfMonthTimestamp) // agés FirestoreConstants.Schedule.START_TIME 사용
            .whereLessThan(FirestoreConstants.Schedule.START_TIME, endOfMonthTimestamp)
            .orderBy(FirestoreConstants.Schedule.START_TIME, Query.Direction.ASCENDING) // agés FirestoreConstants.Schedule.START_TIME 사용

        val listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                trySend(CustomResult.Failure(Exception("$error")))
                close() // 오류 발생 시 Flow 종료
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val schedules = snapshots.toObjects<ScheduleDTO>(ScheduleDTO::class.java)
                trySend(CustomResult.Success(schedules))
            } else {
                // 스냅샷이 null인 경우는 Firestore 문서가 없을 때도 발생 가능 (Listener 초기 호출 시)
                trySend(CustomResult.Success(emptyList()))
            }
        }
        // Flow가 취소될 때 Firestore 리스너를 제거합니다.
        awaitClose { listenerRegistration.remove() }
    }


    override suspend fun getSchedulesOnDate(userId: String, date: LocalDate): Flow<CustomResult<List<ScheduleDTO>, Exception>> = callbackFlow {

        // DateTimeUtil을 사용하여 해당 날짜의 시작과 끝 Timestamp를 가져옵니다. (UTC 기준)
        val startOfDayTimestamp = DateTimeUtil.localDateToStartOfDayInstant(date)
        val endOfDayExclusiveTimestamp = DateTimeUtil.localDateToEndOfDayInstant(date)

        val query = firestore.collection(FirestoreConstants.Collections.SCHEDULES)
            .whereEqualTo(FirestoreConstants.Schedule.CREATOR_ID, userId)
            .whereGreaterThanOrEqualTo(FirestoreConstants.Schedule.START_TIME, startOfDayTimestamp)
            .whereLessThan(FirestoreConstants.Schedule.START_TIME, endOfDayExclusiveTimestamp)
            .orderBy(FirestoreConstants.Schedule.START_TIME, Query.Direction.ASCENDING)

        val listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (isClosedForSend) { // 채널이 닫혔는지 확인
                return@addSnapshotListener
            }
            if (error != null) {
                trySend(CustomResult.Failure(error))
                // 필요하다면 여기서 close(error) 호출하여 Flow를 종료
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val schedules = snapshots.toObjects<ScheduleDTO>(ScheduleDTO::class.java) // 여기에서 오류가 발생했었음
                trySend(CustomResult.Success(schedules))
            } else {
                trySend(CustomResult.Success(emptyList()))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }
}

