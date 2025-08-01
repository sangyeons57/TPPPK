package com.example.mapper.sync

import com.example.data_model.local.OutBoxEntity
import com.example.domain.enum.EntityType
import com.example.domain.model.AggregateRoot
import com.example.domain.model.enum.OutBoxStatus
import com.example.domain.model.sync.OutBox
import com.example.domain.model.sync.OutBoxPayload
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OutBox 관련 Entity와 Domain 간의 매핑을 담당하는 Mapper
 * 제네릭을 사용하여 다양한 Domain Model을 처리
 *
 * 주의: OutBoxEntity는 여전히 payloadJson: String을 사용하므로
 * 실제 JSON 변환은 JsonConverter를 통해 Data 계층에서 처리됨
 */
@Singleton
class OutBoxMapper @Inject constructor() {

    /**
     * OutBoxEntity를 Domain OutBox로 변환
     * 제네릭 T는 실제 payload 데이터 타입
     * JsonConverter를 통해 payloadJson이 T 타입으로 변환됨
     */
    fun <T> entityToDomain(entity: OutBoxEntity, payload: T): OutBox<T> where T : AggregateRoot {
        val entityType = EntityType.valueOf(entity.entityType)
        val operation = OutBox.OutBoxOperation.valueOf(entity.operation)
        val outBoxPayload = OutBoxPayload.create(payload)
        val status = OutBoxStatus.valueOf(entity.status)

        return OutBox.fromDataSource(
            id = entity.id,
            entityType = entityType,
            entityId = entity.entityId,
            operation = operation,
            payload = outBoxPayload,
            baseVersion = entity.baseVersion,
            priority = entity.priority,
            maxRetries = entity.maxRetries,
            retryDelayMs = entity.retryDelayMs,
            timeoutMs = entity.timeoutMs,
            status = status,
            attempts = entity.attempts,
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            lastAttemptAt = entity.lastAttemptAt?.let { Instant.ofEpochMilli(it) },
            errorMessage = entity.errorMessage
        )
    }

    /**
     * Domain OutBox를 OutBoxEntity로 변환
     * payload의 JSON 직렬화는 JsonConverter에서 처리됨
     */
    fun <T> domainToEntity(
        outBox: OutBox<T>,
        payloadJson: String
    ): OutBoxEntity where T : AggregateRoot {
        return OutBoxEntity(
            id = outBox.id,
            entityType = outBox.entityType.name,
            entityId = outBox.entityId,
            operation = outBox.operation.name,
            payloadJson = payloadJson, // JsonConverter에서 변환된 JSON
            baseVersion = outBox.baseVersion,
            priority = outBox.priority,
            maxRetries = outBox.maxRetries,
            retryDelayMs = outBox.retryDelayMs,
            timeoutMs = outBox.timeoutMs,
            status = outBox.getStatus().name,
            attempts = outBox.getAttempts(),
            createdAt = outBox.createdAt.toEpochMilli(),
            lastAttemptAt = outBox.getLastAttemptAt()?.toEpochMilli(),
            errorMessage = outBox.getErrorMessage()
        )
    }

    /**
     * 새로운 OutBox 생성을 위한 헬퍼 메서드
     * payload를 받아서 OutBoxEntity로 변환
     */
    fun <T> createEntityFromPayload(
        entityType: EntityType,
        entityId: String,
        operation: OutBox.OutBoxOperation,
        payload: T,
        payloadJson: String,
        baseVersion: Int = 1,
        priority: Int = OutBox.DEFAULT_PRIORITY,
        maxRetries: Int = OutBox.DEFAULT_MAX_RETRIES,
        retryDelayMs: Long = OutBox.DEFAULT_RETRY_DELAY_MS,
        timeoutMs: Long = OutBox.DEFAULT_TIMEOUT_MS
    ): OutBoxEntity where T : AggregateRoot {
        val outBoxPayload = OutBoxPayload.create(payload)
        val outBox = OutBox.create(
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payload = outBoxPayload,
            baseVersion = baseVersion,
            priority = priority,
            maxRetries = maxRetries,
            retryDelayMs = retryDelayMs,
            timeoutMs = timeoutMs
        )

        return domainToEntity(outBox, payloadJson)
    }
}