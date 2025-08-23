package com.example.data_repository.base

import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.MemberRemoteDataSource
import com.example.data_model.remote.MemberDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.base.Member
import com.example.domain_repository.base.MemberRepository
import com.example.mapper.DtoMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MemberRepositoryImpl @Inject constructor(
    private val memberRemoteDataSource: MemberRemoteDataSource,
    private val memberMapper: DtoMapper<Member, MemberDTO>,
) : DefaultRepositoryImpl<Member, MemberDTO>(memberRemoteDataSource, memberMapper),
    MemberRepository {

    override suspend fun findAllActiveMembers(): CustomResult<List<Member>, Exception> {
        return when (val result = memberRemoteDataSource.findAllActiveMembers()) {
            is CustomResult.Success -> {
                val members = result.data.map { dto -> memberMapper.dtoToDomain(dto) }
                CustomResult.Success(members)
            }

            is CustomResult.Failure -> CustomResult.Failure(result.error)
            else -> result as CustomResult<List<Member>, Exception>
        }
    }

    override fun observeActiveMembers(): Flow<CustomResult<List<Member>, Exception>> {
        return memberRemoteDataSource.observeActiveMembers().map { result ->
            when (result) {
                is CustomResult.Success -> {
                    val members = result.data.map { dto -> memberMapper.dtoToDomain(dto) }
                    CustomResult.Success(members)
                }

                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> result as CustomResult<List<Member>, Exception>
            }
        }
    }

    override suspend fun findAllBlockedMembers(): CustomResult<List<Member>, Exception> {
        return when (val result = memberRemoteDataSource.findAllBlockedMembers()) {
            is CustomResult.Success -> {
                val members = result.data.map { dto -> memberMapper.dtoToDomain(dto) }
                CustomResult.Success(members)
            }

            is CustomResult.Failure -> CustomResult.Failure(result.error)
            else -> result as CustomResult<List<Member>, Exception>
        }
    }

    override fun observeBlockedMembers(): Flow<CustomResult<List<Member>, Exception>> {
        return memberRemoteDataSource.observeBlockedMembers().map { result ->
            when (result) {
                is CustomResult.Success -> {
                    val members = result.data.map { dto -> memberMapper.dtoToDomain(dto) }
                    CustomResult.Success(members)
                }

                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> result as CustomResult<List<Member>, Exception>
            }
        }
    }
}
