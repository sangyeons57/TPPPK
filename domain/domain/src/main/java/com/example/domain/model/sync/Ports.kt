package com.example.domain.model.sync

interface ConflictResolver<T> {
    fun resolve(local: T, remote: T): T
}

sealed class SyncScope {
    data object All : SyncScope()
    data class Stream(val name: String, val since: String? = null) : SyncScope()
}

interface SyncCursorStore {
    suspend fun getCursor(stream: String): String?
    suspend fun saveCursor(stream: String, cursor: String?)
}

interface SyncCoordinator {
    suspend fun syncAll()
    suspend fun sync(scope: SyncScope)
}

interface SyncPort<T> {
    val name: String

    // ---------클라 -> 서버 (OutBox flush)---------
    suspend fun readOutboxBatch(limit: Int): List<OutBoxRecord>

    suspend fun pushToRemote(events: List<OutBoxRecord>): PushResult

    suspend fun ackOutbox(successIds: List<String>)

    suspend fun retryOutBox(failed: List<FailedEvent>)


    // ---------서버 -> 로컬 (fetch)---------
    suspend fun pullSince(cursor: String?, limit: Int): RemoteBatch<T>

    suspend fun applyRemote(batch: RemoteBatch<T>, resolver: ConflictResolver<T>): ApplyOutcome

    //배치 성공시 커서 갱신
    suspend fun commitCursor(newCursor: String)

    suspend fun resetAndFullResync() {}

}


