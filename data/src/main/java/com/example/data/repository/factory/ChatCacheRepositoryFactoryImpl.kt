package com.example.data.repository.factory

import com.example.data.cache.ChatCacheManager
import com.example.data.repository.ChatCacheRepositoryImpl
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