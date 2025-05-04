package com.example.data.model.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId // Firestore 문서 ID 자동 매핑
import com.google.firebase.firestore.ServerTimestamp // 서버 시간 자동 입력

/**
 * Firestore 'schedules' 컬렉션의 문서 구조와 직접 매핑되는 데이터 전송 객체(DTO)입니다.
 * Firestore SDK를 통해 데이터를 읽고 쓸 때 사용됩니다.
 * 도메인 모델(`Schedule`)과 달리 Firestore 특정 타입(`Timestamp`)을 직접 포함할 수 있습니다.
 *
 * @property id Firestore 문서의 고유 ID. `@DocumentId` 어노테이션을 사용하여 자동으로 매핑됩니다. null일 수 있음(쓰기 시).
 * @property projectId 일정이 속한 프로젝트 ID. null일 경우 개인 일정.
 * @property title 일정 제목.
 * @property content 일정 내용 (선택 사항).
 * @property startTime 일정 시작 시간 (Firestore Timestamp 타입).
 * @property endTime 일정 종료 시간 (Firestore Timestamp 타입).
 * @property participants 참여자 ID 리스트.
 * @property isAllDay 하루 종일 일정 여부. (도메인 모델에는 있지만 스키마엔 없음 - 매핑 시 주의)
 * @property createdAt 문서 생성 시간. `@ServerTimestamp` 어노테이션으로 서버에서 자동 설정. (선택적 추가 필드)
 */
data class ScheduleDto(
    @DocumentId val id: String? = null, // 읽기 시에는 Firestore 문서 ID가 자동 매핑, 쓰기 시에는 null 또는 무시됨
    val projectId: String? = null,
    val title: String = "",
    val content: String? = null,
    val startTime: Timestamp? = null, // Firestore Timestamp 사용
    val endTime: Timestamp? = null,   // Firestore Timestamp 사용
    val participants: List<String>? = null, // Firestore는 필드 부재 시 null을 반환할 수 있음
    val isAllDay: Boolean? = null, // Firestore 필드 부재 시 null 처리
    // @ServerTimestamp val createdAt: Timestamp? = null // 필요 시 생성 시간 자동 기록
) {
    // Firestore Data Class 매핑을 위한 기본 생성자 (필수)
    constructor() : this(null, null, "", null, null, null, null, null)
} 