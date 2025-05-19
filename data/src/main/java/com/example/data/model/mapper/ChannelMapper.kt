package com.example.data.model.mapper

import com.example.core_common.util.DateTimeUtil
import com.example.data.model.local.ChannelEntity
import com.example.data.model.remote.project.ChannelDto
import com.example.domain.model.Channel
import com.example.domain.model.ChannelMode
import com.example.domain.model.ChannelType
import com.example.domain.model.channel.DmSpecificData
import com.example.domain.model.channel.ProjectSpecificData
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import javax.inject.Inject

/**
 * ChannelDto, ChannelEntity, Channel 도메인 모델 간의 변환을 담당합니다.
 * DateTimeUtil을 주입받아 Firestore Timestamp 관련 변환을 처리합니다.
 */
class ChannelMapper @Inject constructor(
    private val dateTimeUtil: DateTimeUtil
) {

    /**
     * Firestore DocumentSnapshot을 Channel 도메인 모델로 변환합니다.
     * 내부적으로 Snapshot을 DTO로 변환 후, 도메인 모델로 최종 변환합니다.
     */
    fun mapToDomain(document: DocumentSnapshot): Channel? {
        return try {
            val dto = document.toObject(ChannelDto::class.java)
            // ID는 dto.id에 자동으로 채워짐 (@DocumentId)
            dto?.toDomainModelWithTime(dateTimeUtil)
        } catch (e: Exception) {
            // Log error: e.g., println("Error mapping snapshot to Channel: ${e.message}")
            null
        }
    }

    /**
     * Firestore DocumentSnapshot을 Channel 도메인 모델로 변환합니다.
     * mapToDomain 메서드와 동일한 기능을 수행하지만 다른 이름으로 제공합니다.
     */
    fun fromFirestore(document: DocumentSnapshot): Channel? {
        return mapToDomain(document)
    }

    /**
     * ChannelDto (Firestore 데이터)를 Channel 도메인 모델로 변환합니다.
     */
    fun mapToDomain(dto: ChannelDto): Channel {
        return dto.toDomainModelWithTime(dateTimeUtil)
    }

    /**
     * Channel 도메인 모델을 Firestore에 저장하기 위한 ChannelDto로 변환합니다.
     */
    fun mapToDto(domain: Channel): ChannelDto {
        return domain.toDtoWithTime()
    }

    // --- Room Entity <-> Domain Model Mappers --- //
    // These do not directly use DateTimeUtil for Firestore Timestamp conversion,
    // but handle Long epoch millis for local storage.

    /**
     * ChannelEntity (Room Entity)를 도메인 모델 Channel로 변환합니다.
     */
    fun toDomain(entity: ChannelEntity): Channel {
        // Updated to use ChannelType.fromString
        val channelType = ChannelType.fromString(entity.type)

        val projectData = if (channelType == ChannelType.PROJECT || channelType == ChannelType.PROJECT) {
            entity.projectId?.let {
                ProjectSpecificData(
                    projectId = it,
                    categoryId = entity.categoryId,
                    order = entity.channelOrder,
                    // Ensure FirestoreConstants.ChannelModeValues.TEXT is correct here
                    channelMode = entity.channelMode
                )
            }
        } else null

        val dmData = if (channelType == ChannelType.DM) {
            DmSpecificData(
                participantIds = if (entity.participantIds.isNotEmpty()) entity.participantIds.split(",") else emptyList()
            )
        } else null

        return Channel(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            type = channelType,
            projectSpecificData = projectData,
            dmSpecificData = dmData,
            lastMessagePreview = entity.lastMessagePreview,
            lastMessageTimestamp = if (entity.lastMessageTimestamp > 0) Instant.ofEpochMilli(entity.lastMessageTimestamp) else null,
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            updatedAt = Instant.ofEpochMilli(entity.updatedAt),
            createdBy = entity.createdBy
        )
    }

    /**
     * 도메인 모델 Channel을 ChannelEntity (Room Entity)로 변환합니다.
     */
    fun toEntity(domain: Channel): ChannelEntity {
        return ChannelEntity(
            id = domain.id,
            name = domain.name,
            description = domain.description,
            type = domain.type.name, // Store enum name as string
            projectId = domain.projectSpecificData?.projectId,
            categoryId = domain.projectSpecificData?.categoryId,
            channelOrder = domain.projectSpecificData?.order ?: 0,
            channelMode = domain.projectSpecificData?.channelMode ?: ChannelMode.UNKNOWN,
            participantIds = domain.dmSpecificData?.participantIds?.joinToString(",") ?: "",
            lastMessagePreview = domain.lastMessagePreview,
            lastMessageTimestamp = domain.lastMessageTimestamp?.toEpochMilli() ?: 0,
            createdAt = domain.createdAt.toEpochMilli(),
            updatedAt = domain.updatedAt.toEpochMilli(),
            createdBy = domain.createdBy
            // cachedAt is set by ChannelEntity by default
        )
    }
}

/**
 * ChannelDto를 Channel 도메인 모델로 변환합니다.
 * DateTimeUtil을 사용하여 Timestamp 필드를 Instant로 변환합니다.
 */
fun ChannelDto.toDomainModelWithTime(dateTimeUtil: DateTimeUtil): Channel {
    val basicDomain = this.toBasicDomainModel() // Uses ChannelType.fromString internally
    return basicDomain.copy(
        lastMessageTimestamp = this.lastMessageTimestamp?.let { dateTimeUtil.firebaseTimestampToInstant(it) },
        createdAt = this.createdAt?.let { dateTimeUtil.firebaseTimestampToInstant(it) } ?: Instant.EPOCH,
        updatedAt = this.updatedAt?.let { dateTimeUtil.firebaseTimestampToInstant(it) } ?: Instant.EPOCH
    )
}

/**
 * Channel 도메인 모델을 ChannelDto로 변환합니다.
 * DateTimeUtil을 사용하여 Instant 필드를 Timestamp로 변환합니다.
 */
fun Channel.toDtoWithTime(): ChannelDto {
    val basicDto = ChannelDto.fromBasicDomainModel(this)
    return basicDto.copy(
        lastMessageTimestamp = this.lastMessageTimestamp?.let { DateTimeUtil.instantToFirebaseTimestamp(it) },
        createdAt = DateTimeUtil.instantToFirebaseTimestamp(this.createdAt),
        updatedAt = DateTimeUtil.instantToFirebaseTimestamp(this.updatedAt)
    )
}