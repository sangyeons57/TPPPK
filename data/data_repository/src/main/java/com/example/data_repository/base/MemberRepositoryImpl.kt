package com.example.data_repository.base

// import com.example.data.datasource.local.projectmember.ProjectMemberLocalDataSource // 필요시
import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.MemberRemoteDataSource
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Member
import com.example.domain.model.vo.DocumentId
import com.example.domain_repository.base.MemberRepository
import com.example.mapper.member.MemberMapper
import javax.inject.Inject

class MemberRepositoryImpl @Inject constructor(
    private val memberRemoteDataSource: MemberRemoteDataSource,
    private val memberMapper: MemberMapper,
) : DefaultRepositoryImpl(memberRemoteDataSource), MemberRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Member)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Member"))
        ensureCollection()
        return if (entity.isNew) {
            memberRemoteDataSource.create(memberMapper.domainToDto(entity))
        } else {
            memberRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
}
