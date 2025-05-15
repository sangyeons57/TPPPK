package com.example.data.datasource.remote.invite

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.InviteFields
import com.example.core_common.constants.FirestoreConstants.MemberFields
import com.example.core_common.constants.FirestoreConstants.ProjectFields
import com.example.core_common.constants.FirestoreConstants.RoleFields
import com.example.core_common.constants.FirestoreConstants.UserFields
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.Invite
import com.example.domain.model.InviteType
import com.example.domain.model.ProjectInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
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
        expiresAt: Instant?
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
                val nowInstant = DateTimeUtil.nowInstant()
                val expirationInstant = expiresAt ?: nowInstant.plus(DEFAULT_EXPIRATION_HOURS, ChronoUnit.HOURS)

                // 초대 데이터 생성
                val inviteData = hashMapOf(
                    InviteFields.TYPE to InviteType.PROJECT_INVITE,
                    InviteFields.INVITER_ID to inviterId,
                    InviteFields.PROJECT_ID to projectId,
                    InviteFields.CREATED_AT to DateTimeUtil.instantToFirebaseTimestamp(nowInstant),
                    InviteFields.EXPIRES_AT to DateTimeUtil.instantToFirebaseTimestamp(expirationInstant)
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
            val expiresAtFirebaseTimestamp = inviteDoc.getTimestamp(InviteFields.EXPIRES_AT)
            val expiresAtInstant = DateTimeUtil.firebaseTimestampToInstant(expiresAtFirebaseTimestamp)
            
            if (expiresAtInstant == null || expiresAtInstant.isBefore(DateTimeUtil.nowInstant())) {
                Result.success(false) // 초대가 만료됨
            } else {
                // 프로젝트 ID 확인
                val projectId = inviteDoc.getString(InviteFields.PROJECT_ID)
                if (projectId == null) {
                    Result.success(false) // 프로젝트 ID 없음
                } else {
                    // 프로젝트 존재 확인
                    val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId).get().await()
                    Result.success(projectDoc.exists()) // True if project exists, false otherwise
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
        if (validationResult.isFailure || validationResult.getOrDefault(false) == false) {
            Result.failure(validationResult.exceptionOrNull() ?: IllegalArgumentException("유효하지 않은 초대 토큰입니다."))
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
            val expiresAtFirebaseTimestamp = inviteDoc.getTimestamp(InviteFields.EXPIRES_AT)
            val expiresAtInstant = DateTimeUtil.firebaseTimestampToInstant(expiresAtFirebaseTimestamp)
            
            if (expiresAtInstant == null || expiresAtInstant.isBefore(DateTimeUtil.nowInstant())) {
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
                    
                    val inviteCreatedAtFirebaseTimestamp = inviteDoc.getTimestamp(InviteFields.CREATED_AT)
                    
                    // createdAtDateTime 계산 로직은 그대로 유지
                    val createdAtInstant = if (inviteCreatedAtFirebaseTimestamp != null) {
                        DateTimeUtil.firebaseTimestampToInstant(inviteCreatedAtFirebaseTimestamp)
                    } else {
                        DateTimeUtil.nowInstant()
                    }

                    // expiresAtDateTime 계산 로직은 수정 없음
                    val expiresAtInstant = DateTimeUtil.firebaseTimestampToInstant(expiresAtFirebaseTimestamp)
                    
                    val invite = Invite(
                        token = token,
                        type = InviteType.fromString(inviteDoc.getString(InviteFields.TYPE)),
                        inviterId = inviterId,
                        inviterName = inviterDoc.getString(UserFields.NAME) ?: "알 수 없음",
                        projectId = projectId,
                        projectName = projectDoc.getString(ProjectFields.NAME) ?: "알 수 없음",
                        expiresAt = expiresAtInstant!!,
                        createdAt = createdAtInstant!!
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