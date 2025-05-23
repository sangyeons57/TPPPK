package com.example.data.datasource.remote.schedule

import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.ScheduleFields
import com.example.data.model.remote.schedule.ScheduleDto
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore // ktx extension
import com.google.firebase.ktx.Firebase // ktx extension
import kotlinx.coroutines.tasks.await // kotlinx-coroutines-play-services for Task.await()
import java.util.NoSuchElementException // 예외 추가
import javax.inject.Inject
import javax.inject.Singleton
import com.example.core_common.util.DateTimeUtil // Import DateTimeUtil
import android.util.Log // Log import 추가
import com.example.core_common.constants.FirestoreConstants
import com.example.data.datasource.remote.user.UserRemoteDataSource
import kotlinx.coroutines.flow.first

/**
 * Firestore 'schedules' 컬렉션과 상호작용하는 ScheduleRemoteDataSource의 구현체입니다.
 * Hilt를 통해 FirebaseFirestore 인스턴스를 주입받습니다.
 * 모든 Firestore 작업은 비동기적으로 처리되며, 실패 시 FirebaseFirestoreException을 발생시킬 수 있습니다.
 */
@Singleton // 애플리케이션 전역에서 싱글톤으로 관리
class ScheduleRemoteDataSourceImpl @Inject constructor(
    // Hilt를 통해 FirebaseFirestore 인스턴스 주입
    private val firestore: FirebaseFirestore,
    private val userRemoteDataSource: UserRemoteDataSource
) : ScheduleRemoteDataSource {

    // 'schedules' 컬렉션 참조
    private val scheduleCollection = firestore.collection(Collections.SCHEDULES)

    /**
     * Firestore에서 특정 날짜 범위에 해당하는 일정 목록을 가져옵니다.
     * 'startTime' 필드를 기준으로 쿼리합니다.
     */
    override suspend fun getSchedulesForDate(startOfDay: Timestamp, endOfDay: Timestamp): List<ScheduleDto> {
        Log.d("ScheduleRemoteDS", "getSchedulesForDate 호출됨: startOfDay=${DateTimeUtil.firebaseTimestampToLocalDateTime(startOfDay)}, endOfDay=${DateTimeUtil.firebaseTimestampToLocalDateTime(endOfDay)}")
        // startTime이 startOfDay 이후이고 endOfDay 이전인 문서를 쿼리
        return userRemoteDataSource.getCurrentUserStream().first().fold(
            onSuccess = { userDto ->
                val querySnapshot = scheduleCollection
                    //.whereEqualTo(ScheduleFields.CREATOR_ID, userDto?.id)
                    .whereGreaterThanOrEqualTo(ScheduleFields.START_TIME, startOfDay)
                    .whereLessThanOrEqualTo(ScheduleFields.START_TIME, endOfDay)
                    // .orderBy("startTime") // 필요 시 시작 시간 순으로 정렬
                    .get()
                    .await() // Task<QuerySnapshot> -> QuerySnapshot

                // QuerySnapshot에서 각 DocumentSnapshot을 ScheduleDto로 변환
                // 실패 시 toObject가 예외를 던질 수 있음 (호출 스택으로 전파됨)
                val schedules = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(ScheduleDto::class.java)
                    // @DocumentId가 자동으로 id 필드를 채워줌
                }
                Log.d("ScheduleRemoteDS", "getSchedulesForDate: ${schedules.size}개의 일정을 가져옴. 데이터: $schedules")
                return@fold schedules

            },
            onFailure = { error ->
                Log.e("ScheduleRemoteDS", "getSchedulesForDate - getCurrentUserStream() 실패: $error")
                return@fold emptyList()
            }
        )
    }

    /**
     * Firestore에서 특정 월 범위에 해당하는 일정 목록을 가져옵니다.
     * 'startTime' 필드를 기준으로 쿼리합니다.
     */
    override suspend fun getSchedulesForMonth(startOfMonth: Timestamp, endOfMonth: Timestamp): List<ScheduleDto> {
        Log.d("ScheduleRemoteDS", "getSchedulesForMonth 호출됨: startOfMonth=${DateTimeUtil.firebaseTimestampToLocalDateTime(startOfMonth)}, endOfMonth=${DateTimeUtil.firebaseTimestampToLocalDateTime(endOfMonth)}")
         // startTime이 startOfMonth 이후이고 endOfMonth 이전인 문서를 쿼리
        return userRemoteDataSource.getCurrentUserStream().first().fold(
            onSuccess = { userDto ->
                val querySnapshot = scheduleCollection
                    //.whereEqualTo(ScheduleFields.CREATOR_ID, userDto?.id)
                    .whereGreaterThanOrEqualTo(ScheduleFields.START_TIME, startOfMonth)
                    .whereLessThanOrEqualTo(ScheduleFields.START_TIME, endOfMonth)
                    .get()
                    .await()

                val schedules = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(ScheduleDto::class.java)
                }
                Log.d("ScheduleRemoteDS", "getSchedulesForMonth: ${schedules.size}개의 일정을 가져옴. 데이터: $schedules")
                return@fold schedules
            },
            onFailure = { error ->
                Log.e("ScheduleRemoteDS", "getSchedulesForMonth - getCurrentUserStream() 실패: $error")
                return@fold emptyList<ScheduleDto>()
            }
        )

    }

    /**
     * Firestore에서 특정 ID를 가진 일정 문서를 가져옵니다.
     */
    override suspend fun getScheduleDetail(scheduleId: String): ScheduleDto {
        Log.d("ScheduleRemoteDS", "getScheduleDetail 호출됨: scheduleId=$scheduleId")
        val documentSnapshot = scheduleCollection.document(scheduleId)
            .get()
            .await() // Task<DocumentSnapshot> -> DocumentSnapshot

        // 문서를 ScheduleDto로 변환, 문서가 없거나 변환 실패 시 예외 발생 가능
        val schedule = documentSnapshot.toObject(ScheduleDto::class.java)
            ?: throw NoSuchElementException("Schedule document with id $scheduleId not found or could not be parsed.")
        Log.d("ScheduleRemoteDS", "getScheduleDetail: 가져온 일정 데이터: $schedule")
        return schedule
    }

    /**
     * 새로운 일정을 Firestore 'schedules' 컬렉션에 추가합니다.
     * Firestore가 자동으로 문서 ID를 생성합니다.
     */
    override suspend fun addSchedule(scheduleDto: ScheduleDto): String {
        val now = DateTimeUtil.nowFirebaseTimestamp()
        val dtoToSave = scheduleDto.copy(
            createdAt = scheduleDto.createdAt ?: now, // Use existing if somehow set, else now
            updatedAt = now // Always set updatedAt to now for new schedule
        )
        val documentReference = scheduleCollection
            .add(dtoToSave) 
            .await()
        return documentReference.id
    }

    /**
     * Firestore에서 특정 ID를 가진 일정 문서를 삭제합니다.
     */
    override suspend fun deleteSchedule(scheduleId: String) {
        scheduleCollection.document(scheduleId)
            .delete()
            .await() // Task<Void> -> Unit (성공 시), 실패 시 예외 발생
    }

    /**
     * Firestore에서 특정 ID를 가진 일정 문서를 업데이트합니다.
     * DTO 객체 전체를 덮어쓰는 방식(set) 대신 특정 필드만 업데이트(update) 할 수도 있습니다.
     * 여기서는 DTO 객체 전체를 사용하여 문서를 업데이트합니다 (set 또는 merge set 권장).
     */
    override suspend fun updateSchedule(scheduleId: String, scheduleDto: ScheduleDto) {
        val dtoToUpdate = scheduleDto.copy(
            updatedAt = DateTimeUtil.nowFirebaseTimestamp() // Always update 'updatedAt'
            // createdAt should not be changed on update, merge will handle this if scheduleDto.createdAt is null or original
        )
        scheduleCollection.document(scheduleId)
             .set(dtoToUpdate, com.google.firebase.firestore.SetOptions.merge()) 
            .await() // Task<Void> -> Unit (성공 시), 실패 시 예외 발생
    }
} 