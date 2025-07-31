package com.example.data_repository.base

import com.example.data_datasource.remote.MessageRemoteDataSource
import com.example.data_model.remote.MessageDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.base.Message
import com.example.domain_repository.base.MessageRepository
import com.example.mapper.DtoMapper
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    messageRemoteDataSource: MessageRemoteDataSource,
    private val messageMapper: DtoMapper<Message, MessageDTO>,
) : DefaultRepositoryImpl<Message, MessageDTO>(messageRemoteDataSource, messageMapper),
    MessageRepository {
    // 모든 기본 CRUD 메서드들은 부모 클래스에서 자동으로 처리됩니다!
}
