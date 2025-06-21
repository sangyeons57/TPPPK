package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.MemberRemoteDataSource
import com.example.data.model.remote.MemberDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.event.AggregateRoot
// import com.example.data.datasource.local.projectmember.ProjectMemberLocalDataSource // 필요시
import com.example.domain.model.base.Member
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepositoryFactoryContext
import com.example.domain.repository.base.MemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MemberRepositoryImpl @Inject constructor(
    private val memberRemoteDataSource: MemberRemoteDataSource,
    override val factoryContext: DefaultRepositoryFactoryContext
) : DefaultRepositoryImpl(memberRemoteDataSource, factoryContext.collectionPath), MemberRepository {

    override fun getProjectMembersStream(projectId: String): Flow<CustomResult<List<Member>, Exception>> {
        // Using observeMembers from the data source, which returns Flow<List<MemberDTO>>
        // Wrap it in CustomResult and map to domain models
        return memberRemoteDataSource.observeMembers(projectId)
            .map { dtoList -> 
                CustomResult.Success(dtoList.map { it.toDomain() })
            }
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Member)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Member"))
        return if (entity.id.isAssigned()) {
            memberRemoteDataSource.update(entity.id, entity.getChangedFields())
        } else {
            memberRemoteDataSource.create(entity.toDto())
        }
    }

}
