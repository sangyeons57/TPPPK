package com.example.orchestrator

import com.example.domain.AggregateRoot
import com.example.domain.model.sync.SyncCoordinator
import com.example.domain.model.sync.SyncCursorStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory to assemble SyncCoordinator instances for a given channel context.
 * It registers ports (messages/tasks) as needed.
 */
@Singleton
class SyncManagerFactory @Inject constructor(
    private val cursorStore: SyncCursorStore,
    private val messagePortFactory: MessageSyncPortFactory,
    private val taskPortFactory: TaskSyncPortFactory,
) {
    fun forChannel(
        channelId: String,
        includeMessages: Boolean = false,
        includeTasks: Boolean = false
    ): SyncCoordinator {
        val ports = mutableListOf<com.example.domain.model.sync.SyncPort<out AggregateRoot>>()
        if (includeMessages) ports += messagePortFactory.create(channelId)
        if (includeTasks) ports += taskPortFactory.create(channelId)
        return DefaultSyncManager(
            ports = ports,
            cursorStore = cursorStore,
            pageSize = 200
        )
    }
}

