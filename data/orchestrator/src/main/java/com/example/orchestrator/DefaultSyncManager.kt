package com.example.orchestrator

import com.example.domain.AggregateRoot
import com.example.domain.model.sync.ConflictResolver
import com.example.domain.model.sync.PushResult
import com.example.domain.model.sync.SyncCoordinator
import com.example.domain.model.sync.SyncCursorStore
import com.example.domain.model.sync.SyncPort
import com.example.domain.model.sync.SyncScope
import com.example.orchestrator.util.SyncLogger

class DefaultSyncManager(
    private val ports: List<SyncPort<out AggregateRoot>>,
    private val cursorStore: SyncCursorStore,
    private val pageSize: Int = 200,
    private val defaultResolver: ConflictResolver<Any> = NoopResolver,
) : SyncCoordinator {

    private val logger = SyncLogger("DefaultSyncManager")

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
        val timer = logger.startTimer("Full Sync")
        logger.logSyncStart(port.name)

        try {
            // Push local changes from Outbox
            val out = port.readOutboxBatch(pageSize)
            logger.logPushStart(port.name, out.size, out)

            if (out.isNotEmpty()) {
                val result: PushResult = port.pushToRemote(out)
                logger.logPushResult(port.name, result.successIds.size, result.failIds.size)

                if (result.successIds.isNotEmpty()) port.ackOutbox(result.successIds)
                if (result.failIds.isNotEmpty()) port.retryOutBox(result.failIds)
            }

            // Pull remote changes
            var cursor = cursorStore.getCursor(port.name)
            do {
                logger.logPullStart(port.name, cursor)

                val remoteBatch = port.pullSince(cursor, pageSize)
                logger.logPullSuccess(port.name, remoteBatch)

                if (remoteBatch.items.isNotEmpty()) {
                    logger.logApplyStart(port.name, remoteBatch.items.size)

                    val outcome =
                        port.applyRemote(remoteBatch, defaultResolver as ConflictResolver<T>)
                    if (!outcome.success) {
                        logger.logApplyFailure(
                            port.name,
                            outcome.error ?: Exception("Unknown apply error")
                        )
                        break
                    }
                }

                remoteBatch.nextCursor?.let { newCursor ->
                    logger.logCursorUpdate(port.name, cursor, newCursor)
                    cursor = newCursor
                    cursorStore.saveCursor(port.name, cursor)
                }

            } while (remoteBatch.hasMore)

            timer.finish(port.name)
            logger.logSyncEnd(port.name)

        } catch (e: Exception) {
            logger.logException("syncInternal", port.name, e)
            throw e
        }
    }


    private object NoopResolver : ConflictResolver<Any> {
        override fun resolve(local: Any, remote: Any): Any = remote
    }
}






