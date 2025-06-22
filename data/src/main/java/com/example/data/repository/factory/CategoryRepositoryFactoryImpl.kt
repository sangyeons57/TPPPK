package com.example.data.repository.factory

import com.example.data.datasource.remote.CategoryRemoteDataSource
import com.example.data.repository.base.CategoryRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.factory.context.CategoryRepositoryFactoryContext
import javax.inject.Inject

class CategoryRepositoryFactoryImpl @Inject constructor(
    private val categoryRemoteDataSource: CategoryRemoteDataSource
) : RepositoryFactory<CategoryRepositoryFactoryContext, CategoryRepository> {

    override fun create(input: CategoryRepositoryFactoryContext): CategoryRepository {
        return CategoryRepositoryImpl(
            factoryContext = input,
            categoryRemoteDataSource = categoryRemoteDataSource
        )
    }
}
