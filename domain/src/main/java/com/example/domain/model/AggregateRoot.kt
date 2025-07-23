package com.example.domain.model

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.google.firebase.firestore.FieldValue
import java.time.Instant
import kotlin.collections.component1
import kotlin.collections.component2


abstract class AggregateRoot {
    /** Collects domain events raised by this aggregate until they are dispatched. */
    abstract val id: DocumentId
    abstract val isNew : Boolean
    
    /** Standard timestamp fields for all domain entities */
    companion object {
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"
    }
    abstract val createdAt: Instant
    abstract val updatedAt: Instant
    private lateinit var originalState: Map<String, Any?>

    private val _domainEvents: MutableList<DomainEvent> = mutableListOf()

    protected fun setOriginalState() {
        originalState = this.getCurrentStateMap()
    }

    abstract fun getCurrentStateMap(): Map<String, Any?>

    fun pullDomainEvents(): List<DomainEvent> {
        val copy = _domainEvents.toList()
        _domainEvents.clear()
        return copy
    }
    fun clearDomainEvents() {
        _domainEvents.clear()
    }
    fun pushDomainEvent(event: DomainEvent) {
        _domainEvents.add(event)
    }
    
    // MessageAttachment 호환성을 위한 별칭 메소드
    fun addEvent(event: DomainEvent) {
        pushDomainEvent(event)
    }
    
    // 상태 변경을 표시하는 메소드 (업데이트된 시간 등을 갱신할 때 사용)
    protected fun markAsChanged() {
        // 구현체에서 필요한 경우 updatedAt을 갱신하도록 처리
        // 이 메소드는 상태 변경을 표시하는 목적으로 사용됨
    }
    fun getChangedFields(): Map<String, Any?> {
        val newState = this.getCurrentStateMap()
        val changedFields = mutableMapOf<String, Any?>()
        if(isNew){
            return newState
        }
        if (!::originalState.isInitialized) {
            return changedFields
        }
        newState.forEach { (key, newValue) ->
            val oldValue = originalState[key]
            if (newValue != oldValue) {
                changedFields[key] = newValue
            }
        }
        return changedFields
    }
}