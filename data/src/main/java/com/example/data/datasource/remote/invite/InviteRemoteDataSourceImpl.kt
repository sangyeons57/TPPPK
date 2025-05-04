package com.example.data.datasource.remote.invite

import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.InviteFields
import com.example.core_common.constants.FirestoreConstants.MemberFields
import com.example.core_common.constants.FirestoreConstants.ProjectFields
import com.example.core_common.constants.FirestoreConstants.RoleFields
import com.example.core_common.constants.FirestoreConstants.Status
import com.example.core_common.constants.FirestoreConstants.UserFields
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.Invite
import com.example.domain.model.ProjectInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 초대 관련 원격 데이터 소스 구현체
 * Firebase Firestore를 사용하여 초대 관련 기능을 구현합니다.
 *
 * @param firestore Firebase Firestore 인스턴스
 * @param auth Firebase Auth 인스턴스
 */
@Singleton
class InviteRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : InviteRemoteDataSource {

    // 초대 토큰 기본 만료 시간 (24시간)
    private val DEFAULT_EXPIRATION_HOURS = 24L

    // 현재 사용자 ID를 가져오는 헬퍼 함수
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("사용자가 로그인되어 있지 않습니다.")
    
    // LocalDateTime을 Date로 변환하는 헬퍼 함수
    private fun LocalDateTime.toDate(): Date =
        Date.from(this.toInstant(ZoneOffset.UTC))
    
    // Date를 LocalDateTime으로 변환하는 헬퍼 함수
    private fun Date.toLocalDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(this.toInstant(), ZoneId.systemDefault())

    /**
     * 새 초대 토큰을 생성합니다.
     *
     * @param projectId 프로젝트 ID
     * @param inviterId 초대자 ID (로그인된 사용자)
     * @param expiresAt 만료 시간 (null인 경우 기본값 사용)
     * @return 생성된 초대 토큰 결과
     */
    override suspend fun createInviteToken(
        projectId: String,
        inviterId: String,
        expiresAt: LocalDateTime?
    ): Result<String> = try {
        // 프로젝트 존재 확인
        val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId).get().await()
        if (!projectDoc.exists()) {
            Result.failure(IllegalArgumentException("존재하지 않는 프로젝트입니다."))
        } else {
            // 초대자가 프로젝트 멤버인지 확인
            val memberDoc = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.MEMBERS).document(inviterId).get().await()
            if (!memberDoc.exists()) {
                Result.failure(IllegalArgumentException("프로젝트 멤버만 초대를 생성할 수 있습니다."))
            } else {
                // 토큰 고유 ID 생성
                val token = UUID.randomUUID().toString()

                // 만료 시간 계산 (파라미터가 없으면 기본값 사용)
                val expirationTime = expiresAt ?: LocalDateTime.now(ZoneOffset.UTC)
                    .plusHours(DEFAULT_EXPIRATION_HOURS)

                // 초대 데이터 생성
                val inviteData = hashMapOf(
                    InviteFields.TYPE to Status.PROJECT_INVITE,
                    InviteFields.INVITER_ID to inviterId,
                    InviteFields.PROJECT_ID to projectId,
                    InviteFields.CREATED_AT to DateTimeUtil.toTimestamp(LocalDateTime.now(ZoneOffset.UTC)),
                    InviteFields.EXPIRES_AT to DateTimeUtil.toTimestamp(expirationTime)
                )

                // Firestore에 초대 문서 생성
                firestore.collection(Collections.INVITES).document(token).set(inviteData).await()

                Result.success(token)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 초대 토큰의 유효성을 검사합니다.
     *
     * @param token 초대 토큰
     * @return 유효성 검사 결과 (true: 유효, false: 무효)
     */
    override suspend fun validateInviteToken(token: String): Result<Boolean> = try {
        val inviteDoc = firestore.collection(Collections.INVITES).document(token).get().await()

        if (!inviteDoc.exists()) {
            Result.success(false) // 초대 토큰이 존재하지 않음
        } else {
            // 초대 만료 시간 확인
            val expiresAt = inviteDoc.getTimestamp(InviteFields.EXPIRES_AT)
            if (expiresAt == null || expiresAt.toDate().time < System.currentTimeMillis()) {
                Result.success(false) // 초대가 만료됨
            } else {
                // 프로젝트 ID 확인
                val projectId = inviteDoc.getString(InviteFields.PROJECT_ID)
                if (projectId == null) {
                    Result.success(false) // 프로젝트 ID 없음
                } else {
                    // 프로젝트 존재 확인
                    val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId).get().await()
                    if (!projectDoc.exists()) {
                        Result.success(false) // 연결된 프로젝트가 존재하지 않음
                    } else {
                        Result.success(true) // 유효한 초대
                    }
                }
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 초대를 수락하고 프로젝트에 사용자를 추가합니다.
     *
     * @param token 초대 토큰
     * @param userId 수락하는 사용자 ID
     * @return 프로젝트 ID 결과
     */
    override suspend fun acceptInvite(token: String, userId: String): Result<String> = try {
        // 초대 토큰 유효성 검사
        val validationResult = validateInviteToken(token)
        if (validationResult.isFailure) {
            Result.failure(validationResult.exceptionOrNull() ?: Exception("초대 유효성 검사 실패"))
        } else if (validationResult.getOrDefault(false) == false) {
            Result.failure(IllegalArgumentException("유효하지 않은 초대 토큰입니다."))
        } else {
            // 초대 정보 가져오기
            val inviteDoc = firestore.collection(Collections.INVITES).document(token).get().await()
            val projectId = inviteDoc.getString(InviteFields.PROJECT_ID)
                ?: return Result.failure(IllegalArgumentException("프로젝트 정보가 없습니다."))

            // 이미 프로젝트 멤버인지 확인
            val memberDoc = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.MEMBERS).document(userId).get().await()
            
            if (memberDoc.exists()) {
                Result.failure(IllegalArgumentException("이미 프로젝트 멤버입니다."))
            } else {
                // 기본 멤버 역할 가져오기
                val defaultRoleQuery = firestore.collection(Collections.PROJECTS).document(projectId)
                    .collection(Collections.ROLES)
                    .whereEqualTo(RoleFields.IS_DEFAULT, true)
                    .limit(1)
                    .get().await()

                val defaultRoleId = if (!defaultRoleQuery.isEmpty) {
                    defaultRoleQuery.documents.first().id
                } else {
                    // 기본 역할이 없는 경우 빈 배열 사용
                    null
                }

                // 사용자를 프로젝트 멤버로 추가
                val memberData = hashMapOf<String, Any>(
                    MemberFields.ROLE_IDS to listOfNotNull(defaultRoleId),
                    MemberFields.JOINED_AT to FieldValue.serverTimestamp()
                )

                // 트랜잭션으로 멤버 추가 및 사용자 프로젝트 목록 업데이트
                firestore.runTransaction { transaction ->
                    // 프로젝트에 멤버 추가
                    transaction.set(
                        firestore.collection(Collections.PROJECTS).document(projectId)
                            .collection(Collections.MEMBERS).document(userId),
                        memberData
                    )

                    // 사용자 문서의 participatingProjectIds 배열에 프로젝트 추가
                    val userRef = firestore.collection(Collections.USERS).document(userId)
                    transaction.update(userRef, UserFields.PARTICIPATING_PROJECT_IDS, FieldValue.arrayUnion(projectId))
                }.await()

                Result.success(projectId)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 초대 정보를 가져옵니다.
     *
     * @param token 초대 토큰
     * @return 초대 정보 결과
     */
    override suspend fun getInviteDetails(token: String): Result<Invite> = try {
        val inviteDoc = firestore.collection(Collections.INVITES).document(token).get().await()

        if (!inviteDoc.exists()) {
            Result.failure(Exception("초대 토큰이 유효하지 않습니다."))
        } else {
            // 만료 시간 확인
            val expiresAt = inviteDoc.getTimestamp(InviteFields.EXPIRES_AT)
            val expiresAtDate = expiresAt?.toDate()
            if (expiresAtDate == null || expiresAtDate.time < System.currentTimeMillis()) {
                Result.failure(Exception("초대 토큰이 만료되었습니다."))
            } else {
                // 프로젝트 정보 가져오기
                val projectId = inviteDoc.getString(InviteFields.PROJECT_ID) ?: ""
                val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId).get().await()
                
                if (!projectDoc.exists()) {
                    Result.failure(Exception("프로젝트를 찾을 수 없습니다."))
                } else {
                    // 초대자 정보 가져오기
                    val inviterId = inviteDoc.getString(InviteFields.INVITER_ID) ?: ""
                    val inviterDoc = firestore.collection(Collections.USERS).document(inviterId).get().await()
                    
                    val inviteCreatedAt = inviteDoc.getTimestamp(InviteFields.CREATED_AT)
                    
                    // createdAtDateTime 계산 로직은 그대로 유지
                    val createdAtDateTime = if (inviteCreatedAt != null) {
                        DateTimeUtil.toLocalDateTime(inviteCreatedAt)
                    } else {
                        LocalDateTime.now() // Fallback
                    }

                    // expiresAtDateTime 계산 로직은 수정 없음
                    val expiresAtDateTime = DateTimeUtil.toLocalDateTime(expiresAt) // expiresAt은 null 체크 완료됨
                    
                    val invite = Invite(
                        token = token,
                        type = inviteDoc.getString(InviteFields.TYPE) ?: Status.PROJECT_INVITE,
                        inviterId = inviterId,
                        inviterName = inviterDoc.getString(UserFields.NICKNAME) ?: "알 수 없음",
                        projectId = projectId,
                        projectName = projectDoc.getString(ProjectFields.NAME) ?: "알 수 없음",
                        expiresAt = expiresAtDateTime!!,
                        createdAt = createdAtDateTime!!
                    )
                    
                    Result.success(invite)
                }
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 초대 토큰에서 프로젝트 정보를 가져옵니다.
     *
     * @param token 초대 토큰
     * @return 프로젝트 정보 결과
     */
    override suspend fun getProjectInfoFromToken(token: String): Result<ProjectInfo> = try {
        // 초대 토큰 확인
        val inviteDoc = firestore.collection(Collections.INVITES).document(token).get().await()

        if (!inviteDoc.exists()) {
            Result.failure(IllegalArgumentException("존재하지 않는 초대 토큰입니다."))
        } else {
            val projectId = inviteDoc.getString(InviteFields.PROJECT_ID)
                ?: return Result.failure(IllegalArgumentException("초대에 프로젝트 정보가 없습니다."))

            // 프로젝트 정보 가져오기
            val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId).get().await()

            if (!projectDoc.exists()) {
                Result.failure(IllegalArgumentException("존재하지 않는 프로젝트입니다."))
            } else {
                val projectName = projectDoc.getString(ProjectFields.NAME) ?: "알 수 없는 프로젝트"
                val description = projectDoc.getString(ProjectFields.DESCRIPTION)

                // 프로젝트 멤버 수 계산
                val membersQuery = firestore.collection(Collections.PROJECTS).document(projectId)
                    .collection(Collections.MEMBERS).get().await()
                val memberCount = membersQuery.size()

                // ProjectInfo 객체 생성
                val projectInfo = ProjectInfo(
                    projectName = projectName,
                    memberCount = memberCount
                )

                Result.success(projectInfo)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
} 