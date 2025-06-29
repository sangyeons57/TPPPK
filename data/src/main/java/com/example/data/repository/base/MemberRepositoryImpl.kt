package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.MemberRemoteDataSource
import com.example.data.model.remote.MemberDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
// import com.example.data.datasource.local.projectmember.ProjectMemberLocalDataSource // 필요시
import com.example.domain.model.base.Member
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.MemberRepository
import com.example.domain.repository.factory.context.MemberRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MemberRepositoryImpl @Inject constructor(
    private val memberRemoteDataSource: MemberRemoteDataSource,
    override val factoryContext: MemberRepositoryFactoryContext
) : DefaultRepositoryImpl(memberRemoteDataSource, factoryContext), MemberRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Member)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Member"))
        ensureCollection()
        return if (entity.isNew) {
            memberRemoteDataSource.create(entity.toDto())
        } else {
            memberRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
}
