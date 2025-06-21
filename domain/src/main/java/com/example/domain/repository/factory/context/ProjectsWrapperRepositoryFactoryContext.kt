package com.example.domain.repository.factory.context

import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.DefaultRepositoryFactoryContext

class ProjectsWrapperRepositoryFactoryContext(
    override val collectionPath: CollectionPath
) : DefaultRepositoryFactoryContext {

}
