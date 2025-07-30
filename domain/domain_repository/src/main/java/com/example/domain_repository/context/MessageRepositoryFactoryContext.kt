package com.example.domain_repository.context

import com.example.domain.vo.CollectionPath

class MessageRepositoryFactoryContext(
    override val collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext
