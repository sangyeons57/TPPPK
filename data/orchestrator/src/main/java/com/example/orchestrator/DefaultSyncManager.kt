package com.example.orchestrator

import com.example.domain.AggregateRoot
import com.example.domain.model.sync.ConflictResolver
import com.example.domain.model.sync.PushResult
import com.example.domain.model.sync.SyncCoordinator
import com.example.domain.model.sync.SyncCursorStore
import com.example.domain.model.sync.SyncPort
import com.example.domain.model.sync.SyncScope

class DefaultSyncManager(
    private val ports: List<SyncPort<AggregateRoot>>,
    private val cursorStore: SyncCursorStore,
    private val pageSize: Int = 200,
    private val defaultResolver: ConflictResolver<Any> = NoopResolver,
) : SyncCoordinator {

    override suspend fun syncAll() {
        ports.forEach { syncInternal(it) }
    }

    override suspend fun sync(scope: SyncScope) {
        when (scope) {
            is SyncScope.All -> syncAll()
            is SyncScope.Stream -> ports.find { it.name == scope.name }?.let { syncInternal(it) }
        }
    }

    private suspend fun <T : AggregateRoot> syncInternal(port: SyncPort<T>) {
        val out = port.readOutboxBatch(pageSize)
        if (out.isNotEmpty()) {
            val result: PushResult = port.pushToRemote(out)
            if (result.successIds.isNotEmpty()) port.ackOutbox(result.successIds)
            if (result.failIds.isNotEmpty()) port.retryOutBox(result.failIds)
        }

        var cursor = cursorStore.getCursor(port.name)
        do {
            val remoteBatch = port.pullSince(cursor, pageSize)
            val outcome = port.applyRemote(remoteBatch, defaultResolver as ConflictResolver<T>)

            if (!outcome.success) break

            remoteBatch.nextCursor?.let {
                port.commitCursor(it)
                cursor = it
                cursorStore.saveCursor(port.name, cursor)
            }

        } while (remoteBatch.hasMore)

    }


    private object NoopResolver : ConflictResolver<Any> {
        override fun resolve(local: Any, remote: Any): Any = remote
    }
}






