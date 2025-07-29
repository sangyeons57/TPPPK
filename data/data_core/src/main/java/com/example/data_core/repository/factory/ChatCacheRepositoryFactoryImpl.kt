package com.example.data_core.repository.factory

import com.example.data_core.cache.ChatCacheManager
import com.example.data_core.repository.ChatCacheRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.ChatCacheRepository
import com.example.domain.repository.factory.context.ChatCacheRepositoryFactoryContext
import javax.inject.Inject

class ChatCacheRepositoryFactoryImpl @Inject constructor(
    private val chatCacheManager: ChatCacheManager
) : RepositoryFactory<ChatCacheRepositoryFactoryContext, ChatCacheRepository> {

    override fun create(input: ChatCacheRepositoryFactoryContext): ChatCacheRepository {
        return ChatCacheRepositoryImpl(
            chatCacheManager = chatCacheManager,
            factoryContext = input
        )
    }
}