package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.data.db.AppTypeConverters
import java.time.Instant
import java.time.LocalDateTime

/**
 * Room 데이터베이스에 저장될 프로젝트 정보 엔티티 클래스입니다.
 *
 * @param id 프로젝트 고유 ID (Primary Key, Firestore 문서 ID 사용)
 * @param name 프로젝트 이름
 * @param description 프로젝트 설명
 * @param ownerId 프로젝트 생성자 ID
 * @param participantIds 참여자 ID 목록 (TypeConverter: List<String> <-> JSON String)
 * @param createdAt 프로젝트 생성 시간 (TypeConverter: LocalDateTime <-> Long Timestamp)
 * @param lastUpdatedAt 마지막 업데이트 시간 (TypeConverter: LocalDateTime <-> Long Timestamp)
 */
@Entity(tableName = "projects") // 테이블 이름 지정
@TypeConverters(AppTypeConverters::class) // 타입 컨버터 적용
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val ownerId: String,
    val participantIds: List<String> = emptyList(),
    val createdAt: Instant,
    val lastUpdatedAt: Instant
) 