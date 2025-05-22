package com.example.data.model.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.ProjectMember

/**
 * 프로젝트 멤버 데이터를 로컬 데이터베이스에 저장하기 위한 엔티티
 */
@Entity(
    tableName = "project_members",
    primaryKeys = ["projectId", "userId"],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["userId"])
    ]
)
data class ProjectMemberEntity(
    /**
     * 프로젝트 ID
     */
    val projectId: String,
    
    /**
     * 사용자 ID
     */
    val userId: String,
    
    /**
     * 사용자 이름 (닉네임)
     */
    val userName: String,
    
    /**
     * 프로필 이미지 URL (nullable)
     */
    val profileImageUrl: String?,
    
    /**
     * 역할 ID 목록 (JSON 문자열로 저장)
     */
    val roleIdsJson: String,
    
    /**
     * 로컬 캐시 저장 시간
     */
    val cachedAt: Long = System.currentTimeMillis()
) {
    /**
     * ProjectMemberEntity를 도메인 모델 ProjectMember로 변환
     * 
     * @return ProjectMember 도메인 모델
     */
    fun toDomain(): ProjectMember {
        // JSON 문자열에서 역할 ID 목록으로 변환
        val roleIds = roleIdsJson.split(",").filter { it.isNotEmpty() }
        
        return ProjectMember(
            userId = userId,
            userName = userName,
            profileImageUrl = profileImageUrl,
            roleIds = roleIds
        )
    }

    companion object {
        /**
         * 도메인 모델 ProjectMember를 ProjectMemberEntity로 변환
         * 
         * @param projectId 프로젝트 ID
         * @param member 변환할 ProjectMember 객체
         * @return ProjectMemberEntity
         */
        fun fromDomain(projectId: String, member: ProjectMember): ProjectMemberEntity {
            // 역할 ID 목록을 JSON 문자열로 변환
            val roleIdsJsonValue = member.roleIds.joinToString(",")
            
            return ProjectMemberEntity(
                projectId = projectId,
                userId = member.userId,
                userName = member.userName,
                profileImageUrl = member.profileImageUrl,
                roleIdsJson = roleIdsJsonValue
            )
        }
    }
} 