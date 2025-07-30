package com.example.domain_repository.context

import com.example.domain.vo.CollectionPath

class ChatCacheRepositoryFactoryContext(
    override val collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext