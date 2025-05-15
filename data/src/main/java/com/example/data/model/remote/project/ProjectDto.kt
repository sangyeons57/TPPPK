package com.example.data.model.remote.project

// import com.example.core_common.util.DateTimeUtil // No longer used for default values here
import com.example.core_common.constants.FirestoreConstants
import com.example.domain.model.Project
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.time.Instant // Required for toBasicDomainModel if domain model uses Instant directly

/**
 * Firestore에 저장되는 프로젝트 데이터 모델입니다.
 * Firestore 필드 이름은 @PropertyName을 통해 매핑됩니다.
 */
data class ProjectDto(
    @DocumentId
    val projectId: String = "",
    
    @PropertyName(FirestoreConstants.ProjectFields.NAME)
    var name: String = "",
    
    @PropertyName(FirestoreConstants.ProjectFields.DESCRIPTION)
    var description: String? = null,
    
    @PropertyName(FirestoreConstants.ProjectFields.IMAGE_URL)
    var imageUrl: String? = null,
    
    @PropertyName(FirestoreConstants.ProjectFields.CATEGORY_ID)
    var categoryId: String? = null,
    
    @PropertyName(FirestoreConstants.ProjectFields.OWNER_ID)
    var ownerId: String = "",
    
    @PropertyName(FirestoreConstants.ProjectFields.MEMBER_IDS)
    var memberIds: List<String> = emptyList(),
    
    @PropertyName(FirestoreConstants.ProjectFields.CREATED_AT)
    var createdAt: Timestamp? = null,

    @PropertyName(FirestoreConstants.ProjectFields.UPDATED_AT)
    var updatedAt: Timestamp? = null,

    @PropertyName(FirestoreConstants.ProjectFields.IS_PUBLIC)
    var isPublic: Boolean = false
) {
    /**
     * ProjectDto 객체를 Firestore에 저장하기 위한 Map으로 변환합니다.
     * DocumentId 필드(projectId)는 Firestore에 의해 자동으로 처리되므로 Map에 포함하지 않습니다.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            FirestoreConstants.ProjectFields.NAME to name,
            FirestoreConstants.ProjectFields.DESCRIPTION to description,
            FirestoreConstants.ProjectFields.IMAGE_URL to imageUrl,
            FirestoreConstants.ProjectFields.OWNER_ID to ownerId,
            FirestoreConstants.ProjectFields.MEMBER_IDS to memberIds,
            FirestoreConstants.ProjectFields.CREATED_AT to createdAt,
            FirestoreConstants.ProjectFields.UPDATED_AT to updatedAt,
            FirestoreConstants.ProjectFields.IS_PUBLIC to isPublic,
            FirestoreConstants.ProjectFields.CATEGORY_ID to categoryId
        )
    }

    /**
     * ProjectDto를 기본적인 Project 도메인 모델로 변환합니다.
     * 시간 관련 필드(createdAt, updatedAt)는 Instant.EPOCH로 초기화되며,
     * 실제 시간 변환은 Mapper의 toDomainModelWithTime과 같은 확장 함수에서 처리합니다.
     * categoryId는 Project 도메인 모델에 없으므로 이 변환에서는 무시됩니다.
     */
    fun toBasicDomainModel(): Project {
        return Project(
            id = this.projectId,
            name = this.name,
            description = this.description,
            imageUrl = this.imageUrl,
            ownerId = this.ownerId,
            memberIds = this.memberIds,
            createdAt = Instant.EPOCH, // Placeholder, to be overwritten by mapper
            updatedAt = Instant.EPOCH, // Placeholder, to be overwritten by mapper
            isPublic = this.isPublic
            // categoryId is not part of the Project domain model
        )
    }

    companion object {
        /**
         * Firestore Map 데이터로부터 ProjectDto 객체를 생성합니다.
         * 이 함수는 Firestore의 toObject() 기능을 보완하거나 테스트 시 사용될 수 있습니다.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, documentId: String): ProjectDto {
            return ProjectDto(
                projectId = documentId,
                name = map[FirestoreConstants.ProjectFields.NAME] as? String ?: "",
                description = map[FirestoreConstants.ProjectFields.DESCRIPTION] as? String,
                imageUrl = map[FirestoreConstants.ProjectFields.IMAGE_URL] as? String,
                ownerId = map[FirestoreConstants.ProjectFields.OWNER_ID] as? String ?: "",
                memberIds = (map[FirestoreConstants.ProjectFields.MEMBER_IDS] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                createdAt = map[FirestoreConstants.ProjectFields.CREATED_AT] as? Timestamp,
                updatedAt = map[FirestoreConstants.ProjectFields.UPDATED_AT] as? Timestamp,
                isPublic = map[FirestoreConstants.ProjectFields.IS_PUBLIC] as? Boolean ?: false,
                categoryId = map[FirestoreConstants.ProjectFields.CATEGORY_ID] as? String
            )
        }

        /**
         * 기본적인 Project 도메인 모델로부터 ProjectDto 객체를 생성합니다.
         * 시간 관련 필드(createdAt, updatedAt)는 null로 초기화되며,
         * 실제 Timestamp 변환은 Mapper의 toDtoWithTime과 같은 확장 함수에서 처리합니다.
         * categoryId는 Project 도메인 모델에 없으므로 null로 설정합니다.
         * (현재 ProjectMapper는 빈 문자열 ""로 설정하고 있으므로, 추후 Mapper 로직과 일관성 확인 필요)
         */
        fun fromBasicDomainModel(domain: Project): ProjectDto {
            return ProjectDto(
                projectId = domain.id,
                name = domain.name,
                description = domain.description,
                imageUrl = domain.imageUrl,
                ownerId = domain.ownerId,
                memberIds = domain.memberIds,
                createdAt = null, // Placeholder, to be overwritten by mapper
                updatedAt = null, // Placeholder, to be overwritten by mapper
                isPublic = domain.isPublic,
                // Project domain model doesn't have categoryId. Defaulting to null.
                // The mapper's toFirestore currently sets it to "". This DTO will set to null.
                // Mapper logic will need to decide final value if domain model doesn't provide it.
                categoryId = null 
            )
        }
    }
} 