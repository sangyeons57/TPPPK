package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.data.db.AppTypeConverters
import java.time.LocalDateTime

/**
 * Room 데이터베이스에 저장될 사용자 정보 엔티티 클래스입니다.
 *
 * @param id 사용자의 고유 ID (Primary Key, Firebase Auth UID 사용)
 * @param email 사용자 이메일
 * @param name 사용자 이름
 * @param profileImageUrl 사용자 프로필 이미지 URL (Nullable)
 * @param joinedProjects 참여 중인 프로젝트 ID 목록 (TypeConverter: List<String> <-> JSON String)
 * @param createdAt 사용자 계정 생성 시간 (TypeConverter: LocalDateTime <-> Long Timestamp)
 */
@Entity(tableName = "users") // 테이블 이름 지정
@TypeConverters(AppTypeConverters::class) // 타입 컨버터 적용
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val name: String,
    val profileImageUrl: String? = null,
    val joinedProjects: List<String> = emptyList(),
    val createdAt: LocalDateTime
) 