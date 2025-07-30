package com.example.domain_repository.context

import com.example.domain.vo.CollectionPath

class DMWrapperRepositoryFactoryContext(
    override val collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext
