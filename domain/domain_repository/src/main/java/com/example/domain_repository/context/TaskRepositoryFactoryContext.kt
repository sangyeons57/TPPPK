package com.example.domain_repository.context

import com.example.domain.vo.CollectionPath

class TaskRepositoryFactoryContext(
    override val collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext