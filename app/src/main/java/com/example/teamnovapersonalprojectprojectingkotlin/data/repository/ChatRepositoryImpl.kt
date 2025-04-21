package com.example.teamnovapersonalprojectprojectingkotlin.data.repository

import android.net.Uri
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.ChatMessage
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.MediaImage
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.Result

class ChatRepositoryImpl @Inject constructor(
    // TODO: ChatApiService, ChatDao 등 주입
) : ChatRepository {

    override fun getMessagesStream(channelId: String): Flow<List<ChatMessage>> {
        println("ChatRepositoryImpl: getMessagesStream called for $channelId (returning empty flow)")
        return flowOf(emptyList())
    }

    override suspend fun fetchPastMessages(channelId: String, beforeMessageId: Int, limit: Int): Result<List<ChatMessage>> {
        println("ChatRepositoryImpl: fetchPastMessages called for $channelId before $beforeMessageId (returning empty list)")
        return Result.success(emptyList())
    }

    override suspend fun sendMessage(channelId: String, message: String, attachmentUris: List<Uri>): Result<ChatMessage> {
        println("ChatRepositoryImpl: sendMessage called for $channelId (returning failure)")
        return Result.failure(NotImplementedError("구현 필요"))
        // 임시 성공 데이터 예시:
        // return Result.success(ChatMessage(Random.nextInt(), channelId, 1, "Me", null, message, LocalDateTime.now(), false, attachmentUris.map { it.toString() }))
    }

    override suspend fun editMessage(channelId: String, chatId: Int, newMessage: String): Result<Unit> {
        println("ChatRepositoryImpl: editMessage called for $chatId (returning success)")
        return Result.success(Unit)
    }

    override suspend fun deleteMessage(channelId: String, chatId: Int): Result<Unit> {
        println("ChatRepositoryImpl: deleteMessage called for $chatId (returning success)")
        return Result.success(Unit)
    }

    override suspend fun getLocalGalleryImages(page: Int, pageSize: Int): Result<List<MediaImage>> {
        println("ChatRepositoryImpl: getLocalGalleryImages called for page $page (returning empty list)")
        return Result.success(emptyList())
    }
}