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

/**
 * Firestore 'schedules' 컬렉션과 상호작용하는 ScheduleRemoteDataSource의 구현체입니다.
 * Hilt를 통해 FirebaseFirestore 인스턴스를 주입받습니다.
 * 모든 Firestore 작업은 비동기적으로 처리되며, 실패 시 FirebaseFirestoreException을 발생시킬 수 있습니다.
 */
@Singleton // 애플리케이션 전역에서 싱글톤으로 관리
class ScheduleRemoteDataSourceImpl @Inject constructor(
    // Hilt를 통해 FirebaseFirestore 인스턴스 주입
    private val firestore: FirebaseFirestore
) : ScheduleRemoteDataSource {

    // 'schedules' 컬렉션 참조
    private val scheduleCollection = firestore.collection(Collections.SCHEDULES)

    /**
     * Firestore에서 특정 날짜 범위에 해당하는 일정 목록을 가져옵니다.
     * 'startTime' 필드를 기준으로 쿼리합니다.
     */
    override suspend fun getSchedulesForDate(startOfDay: Timestamp, endOfDay: Timestamp): List<ScheduleDto> {
        // startTime이 startOfDay 이후이고 endOfDay 이전인 문서를 쿼리
        val querySnapshot = scheduleCollection
            .whereGreaterThanOrEqualTo(ScheduleFields.START_TIME, startOfDay)
            .whereLessThanOrEqualTo(ScheduleFields.START_TIME, endOfDay)
            // .orderBy("startTime") // 필요 시 시작 시간 순으로 정렬
            .get()
            .await() // Task<QuerySnapshot> -> QuerySnapshot

        // QuerySnapshot에서 각 DocumentSnapshot을 ScheduleDto로 변환
        // 실패 시 toObject가 예외를 던질 수 있음 (호출 스택으로 전파됨)
        return querySnapshot.documents.mapNotNull { document ->
            document.toObject(ScheduleDto::class.java)
            // @DocumentId가 자동으로 id 필드를 채워줌
        }
    }

    /**
     * Firestore에서 특정 월 범위에 해당하는 일정 목록을 가져옵니다.
     * 'startTime' 필드를 기준으로 쿼리합니다.
     */
    override suspend fun getSchedulesForMonth(startOfMonth: Timestamp, endOfMonth: Timestamp): List<ScheduleDto> {
         // startTime이 startOfMonth 이후이고 endOfMonth 이전인 문서를 쿼리
        val querySnapshot = scheduleCollection
            .whereGreaterThanOrEqualTo(ScheduleFields.START_TIME, startOfMonth)
            .whereLessThanOrEqualTo(ScheduleFields.START_TIME, endOfMonth)
            .get()
            .await()

        return querySnapshot.documents.mapNotNull { document ->
            document.toObject(ScheduleDto::class.java)
        }
    }

    /**
     * Firestore에서 특정 ID를 가진 일정 문서를 가져옵니다.
     */
    override suspend fun getScheduleDetail(scheduleId: String): ScheduleDto {
        val documentSnapshot = scheduleCollection.document(scheduleId)
            .get()
            .await() // Task<DocumentSnapshot> -> DocumentSnapshot

        // 문서를 ScheduleDto로 변환, 문서가 없거나 변환 실패 시 예외 발생 가능
        return documentSnapshot.toObject(ScheduleDto::class.java)
            ?: throw NoSuchElementException("Schedule document with id $scheduleId not found or could not be parsed.")
    }

    /**
     * 새로운 일정을 Firestore 'schedules' 컬렉션에 추가합니다.
     * Firestore가 자동으로 문서 ID를 생성합니다.
     */
    override suspend fun addSchedule(scheduleDto: ScheduleDto): String {
        // DTO 객체를 Firestore에 추가, await()은 DocumentReference를 반환
        val documentReference = scheduleCollection
            .add(scheduleDto) // DTO 객체를 직접 전달 (필드명과 일치해야 함)
            .await()
        // 생성된 문서의 ID 반환
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
        // scheduleDto에 id 필드가 있다면 Firestore 쓰기 시 문제를 일으킬 수 있으므로 주의
        // 여기서는 set(dto, SetOptions.merge())를 사용해 명시된 필드만 업데이트하거나,
        // 또는 dto에서 id를 제외한 Map을 만들어 update()를 사용하는 것이 더 안전할 수 있습니다.
        // 가장 간단한 방법은 DTO 전체를 set으로 덮어쓰는 것입니다. (단, createdAt 같은 필드 관리 주의)
        scheduleCollection.document(scheduleId)
            //.set(scheduleDto) // 문서 전체 덮어쓰기
             .set(scheduleDto, com.google.firebase.firestore.SetOptions.merge()) // 병합: DTO 내 non-null 필드만 업데이트/추가
            .await() // Task<Void> -> Unit (성공 시), 실패 시 예외 발생
    }
} 