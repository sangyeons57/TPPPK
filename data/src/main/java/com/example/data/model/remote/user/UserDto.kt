package com.example.data.model.remote.user

import com.example.core_common.constants.FirestoreConstants
import com.example.domain.model.AccountStatus
import com.example.domain.model.User
import com.example.domain.model.UserStatus
// import com.example.core_common.util.DateTimeUtil // No longer used for default value here
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
// import com.google.firebase.firestore.IgnoreExtraProperties // Not in original, but good practice
import java.time.Instant

/**
 * Firestore 'users' 컬렉션 문서와 매핑되는 데이터 클래스
 * 사용자 프로필 정보를 담고 있습니다.
 */
data class UserDto(
    /**
     * 사용자 ID (Firestore 문서 내 필드, 문서 ID와 동일)
     */
    @DocumentId
    var id: String = "",

    @get:PropertyName(FirestoreConstants.UserFields.EMAIL) @set:PropertyName(FirestoreConstants.UserFields.EMAIL)
    var email: String = "",

    @get:PropertyName(FirestoreConstants.UserFields.NAME) @set:PropertyName(FirestoreConstants.UserFields.NAME)
    var name: String = "",

    @get:PropertyName(FirestoreConstants.UserFields.PROFILE_IMAGE_URL) @set:PropertyName(FirestoreConstants.UserFields.PROFILE_IMAGE_URL)
    var profileImageUrl: String? = null,

    @get:PropertyName(FirestoreConstants.UserFields.STATUS_MESSAGE) @set:PropertyName(FirestoreConstants.UserFields.STATUS_MESSAGE)
    var statusMessage: String? = null,

    @get:PropertyName(FirestoreConstants.UserFields.MEMO) @set:PropertyName(FirestoreConstants.UserFields.MEMO)
    var memo: String? = null,

    @get:PropertyName(FirestoreConstants.UserFields.STATUS) @set:PropertyName(FirestoreConstants.UserFields.STATUS)
    var status: UserStatus = UserStatus.UNKNOWN,

    @get:PropertyName(FirestoreConstants.UserFields.CREATED_AT) @set:PropertyName(FirestoreConstants.UserFields.CREATED_AT)
    var createdAt: Timestamp? = null, // Firebase Auth의 메타데이터로 대체 가능성 검토

    @get:PropertyName(FirestoreConstants.UserFields.UPDATED_AT) @set:PropertyName(FirestoreConstants.UserFields.UPDATED_AT)
    var updatedAt: Timestamp? = null,

    @get:PropertyName(FirestoreConstants.UserFields.FCM_TOKEN) @set:PropertyName(FirestoreConstants.UserFields.FCM_TOKEN)
    var fcmToken: String? = null,

    @get:PropertyName(FirestoreConstants.UserFields.PARTICIPATING_PROJECT_IDS) @set:PropertyName(FirestoreConstants.UserFields.PARTICIPATING_PROJECT_IDS)
    var participatingProjectIds: List<String> = emptyList(),

    @get:PropertyName(FirestoreConstants.UserFields.ACCOUNT_STATUS) @set:PropertyName(FirestoreConstants.UserFields.ACCOUNT_STATUS)
    var accountStatus: AccountStatus = AccountStatus.UNKNOWN,

    @get:PropertyName(FirestoreConstants.UserFields.PARTICIPATING_DM_IDS) @set:PropertyName(FirestoreConstants.UserFields.PARTICIPATING_DM_IDS)
    var activeDmIds: List<String> = emptyList(),

    @get:PropertyName(FirestoreConstants.UserFields.IS_EMAIL_VERIFIED) @set:PropertyName(FirestoreConstants.UserFields.IS_EMAIL_VERIFIED)
    var isEmailVerified: Boolean = false, // Firebase Auth에서 직접 확인
    
    @get:PropertyName(FirestoreConstants.UserFields.CONSENT_TIMESTAMP) @set:PropertyName(FirestoreConstants.UserFields.CONSENT_TIMESTAMP)
    var consentTimeStamp: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            FirestoreConstants.UserFields.EMAIL to email,
            FirestoreConstants.UserFields.NAME to name,
            FirestoreConstants.UserFields.PROFILE_IMAGE_URL to profileImageUrl,
            FirestoreConstants.UserFields.STATUS_MESSAGE to statusMessage,
            FirestoreConstants.UserFields.MEMO to memo,
            FirestoreConstants.UserFields.STATUS to status,
            FirestoreConstants.UserFields.CREATED_AT to createdAt,
            FirestoreConstants.UserFields.UPDATED_AT to updatedAt,
            FirestoreConstants.UserFields.FCM_TOKEN to fcmToken,
            FirestoreConstants.UserFields.PARTICIPATING_PROJECT_IDS to participatingProjectIds,
            FirestoreConstants.UserFields.ACCOUNT_STATUS to accountStatus,
            FirestoreConstants.UserFields.PARTICIPATING_DM_IDS to activeDmIds,
            FirestoreConstants.UserFields.IS_EMAIL_VERIFIED to isEmailVerified,
            FirestoreConstants.UserFields.CONSENT_TIMESTAMP to consentTimeStamp
        ).filterValues { it != null }
    }

    // UserDto의 필드만 사용하여 User 도메인 모델을 생성합니다.
    fun toDomainModel(): User {
        return User(
            id = this.id,
            email = this.email,
            name = this.name,
            profileImageUrl = this.profileImageUrl,
            statusMessage = this.statusMessage,
            memo = this.memo,
            status = this.status,
            createdAt = this.createdAt?.toDate()?.toInstant() ?: Instant.EPOCH,
            updatedAt = this.updatedAt?.toDate()?.toInstant(),
            fcmToken = this.fcmToken,
            participatingProjectIds = this.participatingProjectIds,
            accountStatus = this.accountStatus,
            activeDmIds = this.activeDmIds,
            isEmailVerified = this.isEmailVerified,
            consentTimeStamp = this.consentTimeStamp?.toDate()?.toInstant()
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, documentId: String): UserDto {
            return UserDto(
                id = documentId,
                email = map[FirestoreConstants.UserFields.EMAIL] as? String ?: "",
                name = map[FirestoreConstants.UserFields.NAME] as? String ?: "",
                profileImageUrl = map[FirestoreConstants.UserFields.PROFILE_IMAGE_URL] as? String,
                statusMessage = map[FirestoreConstants.UserFields.STATUS_MESSAGE] as? String,
                memo = map[FirestoreConstants.UserFields.MEMO] as? String,
                status = (map[FirestoreConstants.UserFields.STATUS] as? String)?.let { UserStatus.valueOf(it) } ?: UserStatus.UNKNOWN,
                createdAt = map[FirestoreConstants.UserFields.CREATED_AT] as? Timestamp,
                updatedAt = map[FirestoreConstants.UserFields.UPDATED_AT] as? Timestamp,
                fcmToken = map[FirestoreConstants.UserFields.FCM_TOKEN] as? String,
                participatingProjectIds = (map[FirestoreConstants.UserFields.PARTICIPATING_PROJECT_IDS] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                accountStatus = (map[FirestoreConstants.UserFields.ACCOUNT_STATUS] as? String)?.let { AccountStatus.valueOf(it) } ?: AccountStatus.UNKNOWN,
                activeDmIds = (map[FirestoreConstants.UserFields.PARTICIPATING_DM_IDS] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                isEmailVerified = map[FirestoreConstants.UserFields.IS_EMAIL_VERIFIED] as? Boolean ?: false,
                consentTimeStamp = map[FirestoreConstants.UserFields.CONSENT_TIMESTAMP] as? Timestamp
            )
        }

        // User 도메인 모델을 UserDto로 변환합니다. (Firestore 저장용)
        fun fromDomainModel(domain: User): UserDto {
            return UserDto(
                id = domain.id,
                email = domain.email,
                name = domain.name,
                profileImageUrl = domain.profileImageUrl,
                statusMessage = domain.statusMessage,
                memo = domain.memo,
                status = domain.status,
                createdAt = domain.createdAt?.let { Timestamp(it.epochSecond, it.nano) },
                updatedAt = domain.updatedAt?.let { Timestamp(it.epochSecond, it.nano) },
                fcmToken = domain.fcmToken,
                participatingProjectIds = domain.participatingProjectIds,
                accountStatus = domain.accountStatus,
                activeDmIds = domain.activeDmIds,
                isEmailVerified = domain.isEmailVerified,
                consentTimeStamp = domain.consentTimeStamp?.let { Timestamp(it.epochSecond, it.nano) }
            )
        }
    }
} 