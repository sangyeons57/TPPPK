package com.example.data.repository.factory

import com.example.data.repository.base.SearchRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.SearchRepository
import com.example.domain.repository.factory.context.SearchRepositoryFactoryContext
import javax.inject.Inject

class SearchRepositoryFactoryImpl @Inject constructor(
) : RepositoryFactory<SearchRepositoryFactoryContext, SearchRepository> {

    override fun create(input: SearchRepositoryFactoryContext): SearchRepository {
        return SearchRepositoryImpl()
    }
}
