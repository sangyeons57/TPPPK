package com.example.data_core.repository.base

// import com.example.data.datasource.local.projectmember.ProjectMemberLocalDataSource // 필요시
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.remote.MemberRemoteDataSource
import com.example.data_core.repository.DefaultRepositoryImpl
import com.example.data_model.remote.toDto
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Member
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.MemberRepository
import com.example.domain.repository.factory.context.MemberRepositoryFactoryContext
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
