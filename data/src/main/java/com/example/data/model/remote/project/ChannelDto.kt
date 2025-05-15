package com.example.data.model.remote.project

import com.example.core_common.constants.FirestoreConstants
import com.example.domain.model.Channel
import com.example.domain.model.ChannelMode
import com.example.domain.model.ChannelType
import com.example.domain.model.channel.DmSpecificData
import com.example.domain.model.channel.ProjectSpecificData
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.time.Instant

/**
 * Firestore 'channels' 컬렉션의 문서를 나타내는 데이터 클래스입니다.
 * 채널의 모든 유형(DM, 프로젝트, 카테고리)을 포괄합니다.
 */
data class ChannelDto(
    @DocumentId
    val id: String = "",

    @PropertyName(FirestoreConstants.ChannelFields.NAME)
    var name: String = "",

    @PropertyName(FirestoreConstants.ChannelFields.DESCRIPTION)
    var description: String? = null,

    @PropertyName(FirestoreConstants.ChannelFields.CHANNEL_TYPE)
    var type: ChannelType = ChannelType.PROJECT,

    @PropertyName(FirestoreConstants.ChannelFields.LAST_MESSAGE_PREVIEW)
    var lastMessagePreview: String? = null,

    @PropertyName(FirestoreConstants.ChannelFields.LAST_MESSAGE_TIMESTAMP)
    var lastMessageTimestamp: Timestamp? = null,

    @PropertyName(FirestoreConstants.ChannelFields.CREATED_AT)
    var createdAt: Timestamp? = null,

    @PropertyName(FirestoreConstants.ChannelFields.CREATED_BY)
    var createdBy: String? = null,

    @PropertyName(FirestoreConstants.ChannelFields.UPDATED_AT)
    var updatedAt: Timestamp? = null,

    @PropertyName(FirestoreConstants.ChannelFields.PROJECT_SPECIFIC_DATA)
    var projectSpecificData: Map<String, Any?>? = null,

    @PropertyName(FirestoreConstants.ChannelFields.DM_SPECIFIC_DATA)
    var dmSpecificData: Map<String, Any?>? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            FirestoreConstants.ChannelFields.NAME to name,
            FirestoreConstants.ChannelFields.DESCRIPTION to description,
            FirestoreConstants.ChannelFields.CHANNEL_TYPE to type,
            FirestoreConstants.ChannelFields.LAST_MESSAGE_PREVIEW to lastMessagePreview,
            FirestoreConstants.ChannelFields.LAST_MESSAGE_TIMESTAMP to lastMessageTimestamp,
            FirestoreConstants.ChannelFields.CREATED_AT to createdAt,
            FirestoreConstants.ChannelFields.CREATED_BY to createdBy,
            FirestoreConstants.ChannelFields.UPDATED_AT to updatedAt,
            FirestoreConstants.ChannelFields.PROJECT_SPECIFIC_DATA to projectSpecificData,
            FirestoreConstants.ChannelFields.DM_SPECIFIC_DATA to dmSpecificData
        ).filterValues { it != null } // Firestore는 null 값을 가진 필드를 업데이트하지 않도록 함
    }

    @Suppress("UNCHECKED_CAST")
    fun toBasicDomainModel(): Channel {

        val domainProjectSpecificData = this.projectSpecificData?.let { map ->
            ProjectSpecificData(
                projectId = map[FirestoreConstants.ChannelProjectDataFields.PROJECT_ID] as? String ?: "",
                categoryId = map[FirestoreConstants.ChannelProjectDataFields.CATEGORY_ID] as? String,
                order = (map[FirestoreConstants.ChannelProjectDataFields.ORDER] as? Number)?.toInt() ?: 0,
                channelMode = map[FirestoreConstants.ChannelProjectDataFields.CHANNEL_MODE] as? ChannelMode ?: ChannelMode.UNKNOWN
            )
        }

        val domainDmSpecificData = this.dmSpecificData?.let { map ->
            DmSpecificData(
                participantIds = (map[FirestoreConstants.ChannelDmDataFields.PARTICIPANT_IDS] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        }

        return Channel(
            id = this.id,
            name = this.name,
            description = this.description,
            type = this.type,
            projectSpecificData = domainProjectSpecificData,
            dmSpecificData = domainDmSpecificData,
            lastMessagePreview = this.lastMessagePreview,
            lastMessageTimestamp = null, // Placeholder, mapper handles time
            createdAt = Instant.EPOCH,     // Placeholder, mapper handles time
            updatedAt = Instant.EPOCH,     // Placeholder, mapper handles time
            createdBy = this.createdBy
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, documentId: String): ChannelDto {
            return ChannelDto(
                id = documentId,
                name = map[FirestoreConstants.ChannelFields.NAME] as? String ?: "",
                description = map[FirestoreConstants.ChannelFields.DESCRIPTION] as? String,
                type = map[FirestoreConstants.ChannelFields.CHANNEL_TYPE] as? ChannelType ?: ChannelType.UNKNOWN,
                lastMessagePreview = map[FirestoreConstants.ChannelFields.LAST_MESSAGE_PREVIEW] as? String,
                lastMessageTimestamp = map[FirestoreConstants.ChannelFields.LAST_MESSAGE_TIMESTAMP] as? Timestamp,
                createdAt = map[FirestoreConstants.ChannelFields.CREATED_AT] as? Timestamp,
                createdBy = map[FirestoreConstants.ChannelFields.CREATED_BY] as? String,
                updatedAt = map[FirestoreConstants.ChannelFields.UPDATED_AT] as? Timestamp,
                projectSpecificData = map[FirestoreConstants.ChannelFields.PROJECT_SPECIFIC_DATA] as? Map<String, Any?>,
                dmSpecificData = map[FirestoreConstants.ChannelFields.DM_SPECIFIC_DATA] as? Map<String, Any?>
            )
        }

        fun fromBasicDomainModel(domain: Channel): ChannelDto {
            val dtoProjectSpecificData = domain.projectSpecificData?.let {
                mapOf(
                    FirestoreConstants.ChannelProjectDataFields.PROJECT_ID to it.projectId,
                    FirestoreConstants.ChannelProjectDataFields.CATEGORY_ID to it.categoryId,
                    FirestoreConstants.ChannelProjectDataFields.ORDER to it.order,
                    FirestoreConstants.ChannelProjectDataFields.CHANNEL_MODE to it.channelMode
                ).filterValues { value -> value != null }
            }

            val dtoDmSpecificData = domain.dmSpecificData?.let {
                mapOf(
                    FirestoreConstants.ChannelDmDataFields.PARTICIPANT_IDS to it.participantIds
                )
            }
            
            return ChannelDto(
                id = domain.id,
                name = domain.name,
                description = domain.description,
                type = domain.type,
                lastMessagePreview = domain.lastMessagePreview,
                lastMessageTimestamp = null, // Placeholder
                createdAt = null,            // Placeholder
                updatedAt = null,            // Placeholder
                createdBy = domain.createdBy,
                projectSpecificData = dtoProjectSpecificData,
                dmSpecificData = dtoDmSpecificData
            )
        }
    }
}

// Helper extension in domain model or common module if not present
// fun ChannelType.Companion.fromString(type: String): ChannelType {
//    return ChannelType.values().firstOrNull { it.name.equals(type, ignoreCase = true) } ?: ChannelType.PROJECT
// }
// For ChannelDto, using ChannelType.name for serialization to String is fine.
// Deserialization from String to ChannelType in toBasicDomainModel needs robust handling.
// Assuming ChannelType.fromString or similar exists or will be added.
// For now, a simple ChannelType.valueOf might work if strings match enum names exactly.
// Changed to ChannelType.fromString for robustness.
// Need to define ChannelType.fromString - for example, in ChannelType.kt
/*
enum class ChannelType {
    DM, PROJECT, CATEGORY;

    companion object {
        fun fromString(type: String?): ChannelType {
            return when (type?.uppercase()) {
                "DM" -> DM
                "PROJECT" -> PROJECT
                "CATEGORY" -> CATEGORY
                else -> PROJECT // Default or throw exception
            }
        }
    }
}
*/ 