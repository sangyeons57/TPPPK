package com.example.orchestrator

import com.example.domain.AggregateRoot
import com.example.domain.model.sync.ConflictResolver
import com.example.domain.model.sync.PushResult
import com.example.domain.model.sync.SyncCoordinator
import com.example.domain.model.sync.SyncCursorStore
import com.example.domain.model.sync.SyncPort
import com.example.domain.model.sync.SyncScope

class DefaultSyncManager(
    private val ports: List<SyncPort<out AggregateRoot>>,
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
        // Gemini-added temporary logging. To remove, delete all lines containing "SyncManager".
        android.util.Log.d("SyncManager", "[SYNC START] Starting sync for port: ${port.name}")

        // Push local changes from Outbox
        val out = port.readOutboxBatch(pageSize)
        if (out.isNotEmpty()) {
            android.util.Log.d(
                "SyncManager",
                "[SYNC PUSH] Pushing ${out.size} items for port: ${port.name}. IDs: ${out.joinToString { it.aggregateId }}"
            )
            val result: PushResult = port.pushToRemote(out)
            if (result.successIds.isNotEmpty()) port.ackOutbox(result.successIds)
            if (result.failIds.isNotEmpty()) port.retryOutBox(result.failIds)
        } else {
            android.util.Log.d("SyncManager", "[SYNC PUSH] No items to push for port: ${port.name}")
        }

        // Pull remote changes
        var cursor = cursorStore.getCursor(port.name)
        do {
            android.util.Log.d(
                "SyncManager",
                "[SYNC PULL] Pulling items for port: ${port.name} since cursor: $cursor"
            )
            val remoteBatch = port.pullSince(cursor, pageSize)
            if (remoteBatch.items.isNotEmpty()) {
                android.util.Log.d(
                    "SyncManager",
                    "[SYNC PULL] Pulled ${remoteBatch.items.size} items for port: ${port.name}. IDs: ${remoteBatch.items.joinToString { it.id.value }}"
                )
                val outcome = port.applyRemote(remoteBatch, defaultResolver as ConflictResolver<T>)
                if (!outcome.success) {
                    android.util.Log.e(
                        "SyncManager",
                        "[SYNC PULL] Failed to apply remote batch for port: ${port.name}"
                    )
                    break
                }
            } else {
                android.util.Log.d(
                    "SyncManager",
                    "[SYNC PULL] No new items to pull for port: ${port.name}"
                )
            }


            if (!remoteBatch.hasMore) {
                android.util.Log.d(
                    "SyncManager",
                    "[SYNC PULL] No more items to pull for port: ${port.name}"
                )
            }

            remoteBatch.nextCursor?.let {
                cursor = it
                cursorStore.saveCursor(port.name, cursor)
            }

        } while (remoteBatch.hasMore)

        android.util.Log.d("SyncManager", "[SYNC END] Sync finished for port: ${port.name}")
    }


    private object NoopResolver : ConflictResolver<Any> {
        override fun resolve(local: Any, remote: Any): Any = remote
    }
}






