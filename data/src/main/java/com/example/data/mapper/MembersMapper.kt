package com.example.data.mapper

import com.example.data.model.local.MembersEntity
import com.example.domain.model.base.Member
import com.example.domain.model.vo.DocumentId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * MembersEntity와 Member 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object MembersMapper {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Member 도메인 모델을 MembersEntity로 변환
     * @param member 변환할 Member 도메인 모델
     * @param projectId 멤버가 속한 프로젝트 ID
     * @return MembersEntity
     */
    fun toEntity(member: Member, projectId: String): MembersEntity {
        return MembersEntity(
            id = member.id.value,
            projectId = projectId,
            roleIds = json.encodeToString(member.roleIds.map { it.value }),
            createdAt = member.createdAt,
            updatedAt = member.updatedAt
        )
    }

    /**
     * MembersEntity를 Member 도메인 모델로 변환
     * @param entity 변환할 MembersEntity
     * @return Member 도메인 모델
     */
    fun toDomain(entity: MembersEntity): Member {
        val roleIdsList = json.decodeFromString<List<String>>(entity.roleIds)
            .map { DocumentId(it) }

        return Member.fromDataSource(
            id = DocumentId(entity.id),
            roleIds = roleIdsList,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Member 도메인 모델 리스트를 MembersEntity 리스트로 변환
     * @param members 변환할 Member 도메인 모델 리스트
     * @param projectId 멤버들이 속한 프로젝트 ID
     * @return MembersEntity 리스트
     */
    fun toEntityList(members: List<Member>, projectId: String): List<MembersEntity> {
        return members.map { toEntity(it, projectId) }
    }

    /**
     * MembersEntity 리스트를 Member 도메인 모델 리스트로 변환
     * @param entities 변환할 MembersEntity 리스트
     * @return Member 도메인 모델 리스트
     */
    fun toDomainList(entities: List<MembersEntity>): List<Member> {
        return entities.map { toDomain(it) }
    }
}