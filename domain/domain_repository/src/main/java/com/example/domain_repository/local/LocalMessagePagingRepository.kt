package com.example.domain_repository.local

import androidx.paging.PagingSource
import com.example.domain.model.base.Message

/**
 * Paging3 지원을 위한 Message Repository 확장 인터페이스
 * Clean Architecture를 유지하면서 데이터 계층에서만 Paging3 의존성 사용
 */
interface LocalMessagePagingRepository : LocalMessageRepository {

    /**
     * 메시지용 PagingSource 제공 (시간 역순)
     * @return 타임스탬프 키를 사용하는 PagingSource
     */
    fun getMessagesPagingSource(): PagingSource<Long, Message>
}