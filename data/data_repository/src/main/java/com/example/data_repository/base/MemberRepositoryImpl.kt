package com.example.data_repository.base

import com.example.data_datasource.remote.MemberRemoteDataSource
import com.example.data_model.remote.MemberDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.base.Member
import com.example.domain_repository.base.MemberRepository
import com.example.mapper.DtoMapper
import javax.inject.Inject

class MemberRepositoryImpl @Inject constructor(
    memberRemoteDataSource: MemberRemoteDataSource,
    private val memberMapper: DtoMapper<Member, MemberDTO>,
) : DefaultRepositoryImpl<Member, MemberDTO>(memberRemoteDataSource, memberMapper),
    MemberRepository {
    // 모든 기본 CRUD 메서드들은 부모 클래스에서 자동으로 처리됩니다!
}
