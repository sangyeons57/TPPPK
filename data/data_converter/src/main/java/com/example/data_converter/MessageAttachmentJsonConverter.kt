package com.example.data_converter

import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.enum.MessageAttachmentUploadStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.model.vo.messageattachment.MessageAttachmentThumbnailUrl
import com.example.domain.model.vo.messageattachment.MessageAttachmentUploadProgress
import com.example.domain.model.vo.messageattachment.MessageAttachmentUrl
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MessageAttachment Domain Model과 JSON 간의 변환을 담당하는 Converter
 */
@Singleton
class MessageAttachmentJsonConverter @Inject constructor(
    private val gson: Gson
) : JsonConverter<MessageAttachment> {

    override fun toJson(data: MessageAttachment): String {
        return try {
            val attachmentData = mapOf(
                AggregateRoot.KEY_ID to data.id.value,
                MessageAttachment.KEY_ATTACHMENT_TYPE to data.attachmentType.value,
                MessageAttachment.KEY_ATTACHMENT_URL to data.attachmentUrl.value,
                MessageAttachment.KEY_FILE_NAME to data.fileName?.value,
                MessageAttachment.KEY_FILE_SIZE to data.fileSize?.value,
                MessageAttachment.KEY_THUMBNAIL_URL to data.thumbnailUrl?.value,
                MessageAttachment.KEY_UPLOAD_STATUS to data.uploadStatus.value,
                MessageAttachment.KEY_UPLOAD_PROGRESS to data.uploadProgress.value,
                AggregateRoot.KEY_CREATED_AT to data.createdAt.toEpochMilli(),
                AggregateRoot.KEY_UPDATED_AT to data.updatedAt.toEpochMilli()
            )
            gson.toJson(attachmentData)
        } catch (e: Exception) {
            throw JsonConversionException(
                "Failed to convert MessageAttachment to JSON: ${e.message}",
                e
            )
        }
    }

    override fun fromJson(json: String): MessageAttachment {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val attachmentData: Map<String, Any?> = gson.fromJson(json, type)

            MessageAttachment.fromDataSource(
                id = DocumentId(attachmentData[AggregateRoot.KEY_ID] as String),
                attachmentType = MessageAttachmentType.valueOf(attachmentData[MessageAttachment.KEY_ATTACHMENT_TYPE] as String),
                attachmentUrl = MessageAttachmentUrl(attachmentData[MessageAttachment.KEY_ATTACHMENT_URL] as String),
                fileName = (attachmentData[MessageAttachment.KEY_FILE_NAME] as? String)?.let {
                    MessageAttachmentFileName(
                        it
                    )
                },
                fileSize = (attachmentData[MessageAttachment.KEY_FILE_SIZE] as? Double)?.toLong()
                    ?.let { MessageAttachmentFileSize(it) },
                thumbnailUrl = (attachmentData[MessageAttachment.KEY_THUMBNAIL_URL] as? String)?.let {
                    MessageAttachmentThumbnailUrl(
                        it
                    )
                },
                uploadStatus = MessageAttachmentUploadStatus.valueOf(attachmentData[MessageAttachment.KEY_UPLOAD_STATUS] as String),
                uploadProgress = MessageAttachmentUploadProgress((attachmentData[MessageAttachment.KEY_UPLOAD_PROGRESS] as Double).toFloat()),
                createdAt = (attachmentData[AggregateRoot.KEY_CREATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) },
                updatedAt = (attachmentData[AggregateRoot.KEY_UPDATED_AT] as? Double)?.toLong()
                    ?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: JsonSyntaxException) {
            throw JsonConversionException(
                "Invalid JSON format for MessageAttachment: ${e.message}",
                e
            )
        } catch (e: ClassCastException) {
            throw JsonConversionException(
                "JSON structure mismatch for MessageAttachment: ${e.message}",
                e
            )
        } catch (e: Exception) {
            throw JsonConversionException(
                "Failed to convert JSON to MessageAttachment: ${e.message}",
                e
            )
        }
    }
}