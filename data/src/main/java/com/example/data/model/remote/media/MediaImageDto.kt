package com.example.data.model.remote.media

import android.net.Uri // Required for MediaImage domain model
import androidx.core.net.toUri // For converting String URI to Uri
import com.example.core_common.constants.FirestoreConstants // Added for AttachmentMapKeys
import com.example.domain.model.MediaImage
import com.google.firebase.firestore.DocumentId // Added for @DocumentId
import com.google.firebase.firestore.PropertyName // Added for @PropertyName
import java.time.Instant // Required for MediaImage domain model

/**
 * 미디어 이미지 원격 데이터 전송 객체.
 * Firebase Storage에서 가져온 이미지 정보를 표현하며, Firestore 문서 내 첨부 파일 객체로도 사용됩니다.
 *
 * @property id 이미지의 고유 ID (파일명 또는 생성된 UUID). FirestoreConstants.MessageFields.AttachmentMapKeys.ID에 해당.
 * @property url 이미지 다운로드 URL (String). FirestoreConstants.MessageFields.AttachmentMapKeys.URL에 해당.
 * @property fileName 이미지 파일명. FirestoreConstants.MessageFields.AttachmentMapKeys.FILE_NAME에 해당.
 * @property type 첨부 파일 타입 (기본값: "image"). FirestoreConstants.MessageFields.AttachmentMapKeys.TYPE에 해당.
 * @property path Firebase Storage 경로 (Firestore 첨부 파일 맵에는 포함되지 않음).
 * @property mimeType 이미지 MIME 타입 (예: image/jpeg). FirestoreConstants.MessageFields.AttachmentMapKeys.MIME_TYPE에 해당.
 * @property size 이미지 파일 크기 (바이트). FirestoreConstants.MessageFields.AttachmentMapKeys.SIZE에 해당.
 * @property thumbnailUrl 썸네일 이미지 URL (선택 사항). FirestoreConstants.MessageFields.AttachmentMapKeys.THUMBNAIL_URL에 해당.
 * @property dateAdded 이미지가 추가된 시간 (Unix timestamp - Long) (Firestore 첨부 파일 맵에는 포함되지 않음).
 */
data class MediaImageDto(
    @DocumentId
    @get:PropertyName(FirestoreConstants.MessageFields.AttachmentMapKeys.ID)
    val id: String,

    @get:PropertyName(FirestoreConstants.MessageFields.AttachmentMapKeys.URL)
    val url: String,

    @get:PropertyName(FirestoreConstants.MessageFields.AttachmentMapKeys.FILE_NAME)
    val fileName: String = "",

    @get:PropertyName(FirestoreConstants.MessageFields.AttachmentMapKeys.TYPE)
    val type: String = "image",

    // path is specific to Storage, not part of Firestore attachment schema defined in AttachmentMapKeys
    val path: String = "",

    @get:PropertyName(FirestoreConstants.MessageFields.AttachmentMapKeys.MIME_TYPE)
    val mimeType: String = "",

    @get:PropertyName(FirestoreConstants.MessageFields.AttachmentMapKeys.SIZE)
    val size: Long = 0L,

    @get:PropertyName(FirestoreConstants.MessageFields.AttachmentMapKeys.THUMBNAIL_URL)
    val thumbnailUrl: String? = null,

    // dateAdded is file metadata, not part of Firestore attachment schema defined in AttachmentMapKeys
    val dateAdded: Long = 0L
) {
    /**
     * 이 DTO를 기본적인 MediaImage 도메인 모델로 변환합니다.
     * DTO의 'url'은 도메인의 'contentPath'로, 'fileName'은 도메인의 'name'으로 매핑됩니다.
     * 'type', 'thumbnailUrl', 'path', 'dateAdded' 필드는 MediaImage 도메인 모델에 직접 매핑되지 않습니다.
     */
    fun toBasicDomainModel(): MediaImage {
        return MediaImage(
            id = this.id,
            contentPath = this.url.toUri(), // url (String) to contentPath (Uri)
            name = this.fileName,          // fileName to name
            size = this.size,
            mimeType = this.mimeType,
            dateAdded = Instant.ofEpochMilli(this.dateAdded)
        )
    }

    /**
     * 이 DTO를 Firestore 첨부 파일 맵으로 변환합니다.
     * FirestoreConstants.MessageFields.AttachmentMapKeys의 키를 사용합니다.
     * 'path'와 'dateAdded' 필드는 맵에 포함되지 않습니다.
     */
    fun toAttachmentMap(): Map<String, Any?> {
        return mapOf(
            FirestoreConstants.MessageFields.AttachmentMapKeys.ID to this.id,
            FirestoreConstants.MessageFields.AttachmentMapKeys.TYPE to this.type,
            FirestoreConstants.MessageFields.AttachmentMapKeys.URL to this.url,
            FirestoreConstants.MessageFields.AttachmentMapKeys.FILE_NAME to this.fileName.ifEmpty { null },
            FirestoreConstants.MessageFields.AttachmentMapKeys.SIZE to this.size,
            FirestoreConstants.MessageFields.AttachmentMapKeys.MIME_TYPE to this.mimeType.ifEmpty { null },
            FirestoreConstants.MessageFields.AttachmentMapKeys.THUMBNAIL_URL to this.thumbnailUrl
        ).filterValues { it != null } // Null 값은 Firestore에 저장하지 않도록 필터링
    }

    companion object {
        /**
         * 기본적인 MediaImage 도메인 모델로부터 이 DTO를 생성합니다.
         * 도메인의 'contentPath'는 DTO의 'url'로, 'name'은 DTO의 'fileName'으로 매핑됩니다.
         * DTO의 'type'은 "image"로, 'thumbnailUrl'은 null로 기본 설정됩니다.
         * 'path'는 비워둡니다.
         */
        fun fromBasicDomainModel(domain: MediaImage): MediaImageDto {
            return MediaImageDto(
                id = domain.id,
                url = domain.contentPath.toString(), // contentPath (Uri) to url (String)
                fileName = domain.name,              // name to fileName
                type = "image", // Default type
                path = "", // path is not in domain model
                mimeType = domain.mimeType,
                size = domain.size,
                thumbnailUrl = null, // Default thumbnailUrl
                dateAdded = domain.dateAdded.toEpochMilli()
            )
        }

        /**
         * Firestore 첨부 파일 맵으로부터 MediaImageDto를 생성합니다.
         * FirestoreConstants.MessageFields.AttachmentMapKeys의 키를 사용합니다.
         * 맵에 없는 필드는 기본값으로 채워집니다.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromAttachmentMap(map: Map<String, Any?>, defaultId: String = ""): MediaImageDto {
            return MediaImageDto(
                id = map[FirestoreConstants.MessageFields.AttachmentMapKeys.ID] as? String ?: defaultId,
                url = map[FirestoreConstants.MessageFields.AttachmentMapKeys.URL] as? String ?: "",
                fileName = map[FirestoreConstants.MessageFields.AttachmentMapKeys.FILE_NAME] as? String ?: "",
                type = map[FirestoreConstants.MessageFields.AttachmentMapKeys.TYPE] as? String ?: "image",
                path = "", // Not stored in attachment map
                mimeType = map[FirestoreConstants.MessageFields.AttachmentMapKeys.MIME_TYPE] as? String ?: "",
                size = map[FirestoreConstants.MessageFields.AttachmentMapKeys.SIZE] as? Long ?: 0L,
                thumbnailUrl = map[FirestoreConstants.MessageFields.AttachmentMapKeys.THUMBNAIL_URL] as? String,
                dateAdded = 0L // Not stored in attachment map, default to epoch
            )
        }
    }
} 