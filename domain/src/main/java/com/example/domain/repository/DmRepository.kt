// 경로: domain/repository/DmRepository.kt (신규 생성)
package com.example.domain.repository

import com.example.domain.model.DmConversation
import kotlinx.coroutines.flow.Flow
import kotlin.Result

interface DmRepository {
    fun getDmListStream(): Flow<List<DmConversation>>
    suspend fun fetchDmList(): Result<Unit>
    // 필요시 DM 생성, 삭제 등 함수 추가
}