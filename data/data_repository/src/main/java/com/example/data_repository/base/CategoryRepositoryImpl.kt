package com.example.data_repository.base

import com.example.data_converter.JsonConverter
import com.example.data_datasource.remote.CategoryRemoteDataSource
import com.example.data_model.remote.CategoryDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.base.Category
import com.example.domain_repository.base.CategoryRepository
import com.example.mapper.DtoMapper
import retrofit2.Converter
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    categoryRemoteDataSource: CategoryRemoteDataSource,
    private val categoryMapper: DtoMapper<Category, CategoryDTO>,
) : DefaultRepositoryImpl<Category, CategoryDTO>(categoryRemoteDataSource, categoryMapper),
    CategoryRepository {
    // 모든 기본 CRUD 메서드들은 부모 클래스에서 자동으로 처리됩니다!
}
