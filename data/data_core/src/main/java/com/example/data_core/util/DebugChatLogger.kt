package com.example.data_core.util

import android.util.Log
import com.example.data_core.dao.MessagesDao
import com.example.data_core.model.local.MessagesEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DebugChatLogger @Inject constructor(
    private val messagesDao: MessagesDao
) {

    companion object {
        private const val TAG = "DebugChatCache"
    }

    suspend fun printChannelChatMessages(channelId: String) = withContext(Dispatchers.IO) {
        val messages = messagesDao.getAllMessagesInChannelForDebug(channelId)
        if (messages.isEmpty()) {
            Log.d(TAG, "No chat messages found in the local cache for channel: $channelId")
            return@withContext
        }

        val logString = buildLogString("LOCAL CACHE DUMP FOR CHANNEL: $channelId", messages)
        logString.chunked(4000).forEach { chunk ->
            Log.d(TAG, chunk)
        }
    }

    suspend fun printAllChatMessages() = withContext(Dispatchers.IO) {
        val allMessages = messagesDao.getAllMessagesForDebug()
        if (allMessages.isEmpty()) {
            Log.d(TAG, "No chat messages found in the local cache.")
            return@withContext
        }

        val logString =
            buildLogString("FULL LOCAL CHAT CACHE DUMP", allMessages, showChannelId = true)
        logString.chunked(4000).forEach { chunk ->
            Log.d(TAG, chunk)
        }
    }

    private fun buildLogString(
        title: String,
        messages: List<MessagesEntity>,
        showChannelId: Boolean = false
    ): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("\n\n--- START OF $title ---\n")

        var currentChannelId: String? = null

        messages.forEach { message ->
            if (showChannelId && message.channelId != currentChannelId) {
                currentChannelId = message.channelId
                stringBuilder.append("\n--------------------------------------------------\n")
                stringBuilder.append("CHANNEL: $currentChannelId\n")
                stringBuilder.append("--------------------------------------------------\n")
            }

            stringBuilder.append(
                String.format(
                    "| %-25s | %-30s | %-50s | %-10s |\n",
                    "ID: ${message.id.take(15)}...",
                    "Sender: ${message.senderId}",
                    "Content: ${message.content.take(40)}",
                    "Deleted: ${message.isDeleted}"
                )
            )
            stringBuilder.append(
                String.format(
                    "| %-25s | %-30s | %-50s | %-10s |\n",
                    "Created: ${message.createdAt}",
                    "Updated: ${message.updatedAt}",
                    "",
                    ""
                )
            )
            stringBuilder.append("|----------------------------------------------------------------------------------------------------------------|\n")
        }

        stringBuilder.append("--- END OF $title ---\n\n")
        return stringBuilder.toString()
    }
}