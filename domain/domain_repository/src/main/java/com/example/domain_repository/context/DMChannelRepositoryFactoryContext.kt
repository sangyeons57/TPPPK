package com.example.domain_repository.context

import com.example.domain.vo.CollectionPath

class DMChannelRepositoryFactoryContext(
    override val collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext
