// 경로: data/repository/DmRepositoryImpl.kt
package com.example.data.repository

import com.example.domain.model.DmConversation
import com.example.domain.repository.DmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import kotlin.Result

class DmRepositoryImpl @Inject constructor() : DmRepository {
    override fun getDmListStream(): Flow<List<DmConversation>> {
        println("DmRepositoryImpl: getDmListStream called (returning empty flow)")
        return flowOf(emptyList())
    }
    override suspend fun fetchDmList(): Result<Unit> {
        println("DmRepositoryImpl: fetchDmList called (returning success)")
        return Result.success(Unit)
    }
}