package com.example.data.datasource.remote.projectmember

import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.MemberFields
import com.example.core_common.constants.FirestoreConstants.ProjectFields
import com.example.core_common.constants.FirestoreConstants.RoleFields
import com.example.core_common.constants.FirestoreConstants.UserFields
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.ProjectMember
import com.example.domain.model.Role
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.example.core_common.constants.FirestoreConstants
import com.example.domain.model.RolePermission
import com.google.firebase.firestore.FieldPath

/**
 * 프로젝트 멤버 관련 원격 데이터 소스 구현체
 * Firebase Firestore를 사용하여 프로젝트 멤버 관련 기능을 구현합니다.
 * 
 * @param firestore Firebase Firestore 인스턴스
 * @param auth Firebase Auth 인스턴스
 */
@Singleton
class ProjectMemberRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ProjectMemberRemoteDataSource {

    // 현재 사용자 ID를 가져오는 헬퍼 함수
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("사용자가 로그인되어 있지 않습니다.")

    // Firestore List<String>을 List<RolePermission>으로 변환하는 헬퍼 함수 (ProjectRoleRemoteDataSourceImpl에서 가져옴)
    private fun mapFirestorePermissionsToDomain(firestorePermissions: Any?): List<RolePermission> {
        return (firestorePermissions as? List<*>)?.mapNotNull { permString ->
            try {
                RolePermission.valueOf(permString.toString())
            } catch (e: IllegalArgumentException) {
                Log.w("ProjectMemberDataSource", "Unknown permission string: $permString")
                null
            }
        } ?: emptyList()
    }

    // Role 정보를 가져오는 내부 헬퍼 함수
    private suspend fun getRolesForMember(projectId: String, roleIds: List<String>): List<Role> {
        if (roleIds.isEmpty()) return emptyList()
        return try {
            val rolesSnapshot = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.ROLES)
                .whereIn(FieldPath.documentId(), roleIds)
                .get()
                .await()
            rolesSnapshot.documents.mapNotNull { roleDoc ->
                val permissionsList = mapFirestorePermissionsToDomain(roleDoc.get(RoleFields.PERMISSIONS))
                Role(
                    id = roleDoc.id,
                    projectId = projectId, // projectId 추가
                    name = roleDoc.getString(RoleFields.NAME) ?: "",
                    permissions = permissionsList, // 변환된 리스트 사용
                    isDefault = roleDoc.getBoolean(RoleFields.IS_DEFAULT) ?: false,
                    memberCount = roleDoc.getLong("memberCount")?.toInt() // "memberCount" 사용 및 nullable 처리
                )
            }
        } catch (e: Exception) {
            Log.e("ProjectMemberDataSource", "Error fetching roles: ${roleIds.joinToString()}", e)
            emptyList()
        }
    }

    /**
     * 특정 프로젝트의 모든 멤버 목록을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 목록
     */
    override suspend fun getProjectMembers(projectId: String): Result<List<ProjectMember>> = try {
        val membersCollection = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.MEMBERS)
        val membersSnapshot = membersCollection.get().await()
        val members = mutableListOf<ProjectMember>()

        coroutineScope { // 병렬 처리를 위해 coroutineScope 사용
            membersSnapshot.documents.map { memberDoc ->
                async { // 각 멤버의 정보를 비동기적으로 가져옴
                    val userId = memberDoc.id
                    val roleIds = memberDoc.get(MemberFields.ROLE_IDS)?.let { data ->
                        if (data is List<*>) data.mapNotNull { it as? String } else emptyList()
                    } ?: emptyList()

                    val userDoc = firestore.collection(Collections.USERS).document(userId).get().await()
                    if (!userDoc.exists()) return@async null // 사용자 정보 없으면 null 반환

                    val joinedAtTimestamp = memberDoc.getTimestamp(MemberFields.JOINED_AT)
                    val joinedAtInstant = joinedAtTimestamp?.let { DateTimeUtil.firebaseTimestampToInstant(it) }
                        ?: Instant.EPOCH

                    // 역할 정보 가져오기
                    val roles = getRolesForMember(projectId, roleIds)

                    ProjectMember(
                        userId = userId,
                        userName = userDoc.getString(UserFields.NAME) ?: "사용자",
                        profileImageUrl = userDoc.getString(UserFields.PROFILE_IMAGE_URL),
                        roles = roles, // roleIds 대신 roles 사용
                        joinedAt = joinedAtInstant
                    )
                }
            }.mapNotNull { it.await() }.let { members.addAll(it) } // await 결과가 null이 아닌 것만 추가
        }
        Result.success(members)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 특정 프로젝트의 멤버 목록 실시간 스트림을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 목록의 Flow
     */
    override fun getProjectMembersStream(projectId: String): Flow<List<ProjectMember>> = callbackFlow {
        val membersCollection = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.MEMBERS)

        val listenerRegistration = membersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ProjectMemberDataSource", "Error listening to project members stream", error)
                trySend(emptyList()).isSuccess // trySend 결과 사용
                close(error)
                return@addSnapshotListener
            }
            if (snapshot == null || snapshot.isEmpty) {
                trySend(emptyList()).isSuccess
                return@addSnapshotListener
            }

            // 멤버 목록 처리 (비동기적으로 역할 정보 포함)
            // 스냅샷 리스너 콜백 내에서 launch를 사용하여 비동기 작업 수행
            launch {
                try {
                    val members = mutableListOf<ProjectMember>()
                    coroutineScope {
                        snapshot.documents.map { memberDoc ->
                            async {
                                val userId = memberDoc.id
                                val roleIds = memberDoc.get(MemberFields.ROLE_IDS)?.let { data ->
                                    if (data is List<*>) data.mapNotNull { it as? String } else emptyList()
                                } ?: emptyList()

                                val userDoc = firestore.collection(Collections.USERS).document(userId).get().await()
                                if (!userDoc.exists()) return@async null

                                val joinedAtTimestamp = memberDoc.getTimestamp(MemberFields.JOINED_AT)
                                val joinedAtInstant =
                                    joinedAtTimestamp?.let { DateTimeUtil.firebaseTimestampToInstant(it) }
                                        ?: Instant.EPOCH

                                val roles = getRolesForMember(projectId, roleIds)

                                ProjectMember(
                                    userId = userId,
                                    userName = userDoc.getString(UserFields.NAME) ?: "사용자",
                                    profileImageUrl = userDoc.getString(UserFields.PROFILE_IMAGE_URL),
                                    roles = roles,
                                    joinedAt = joinedAtInstant
                                )
                            }
                        }.mapNotNull { it.await() }.let { members.addAll(it) }
                    }
                    trySend(members).isSuccess
                } catch (e: Exception) {
                    Log.e("ProjectMemberDataSource", "Error processing members stream snapshot", e)
                    trySend(emptyList()).isSuccess // 오류 발생 시 빈 리스트 전송
                }
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    /**
     * 특정 프로젝트의 특정 멤버 정보를 가져옵니다.
     *
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return 프로젝트 멤버 정보 또는 null (에러 발생 시 Result.failure)
     */
    override suspend fun getProjectMember(projectId: String, userId: String): Result<ProjectMember?> = try {
        val memberDocRef = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.MEMBERS).document(userId)
        val memberDoc = memberDocRef.get().await()

        if (!memberDoc.exists()) {
            Result.success(null)
        } else {
            val roleIds = memberDoc.get(MemberFields.ROLE_IDS)?.let { data ->
                if (data is List<*>) data.mapNotNull { it as? String } else emptyList()
            } ?: emptyList()

            val userDocRef = firestore.collection(Collections.USERS).document(userId)
            val userDoc = userDocRef.get().await()

            if (!userDoc.exists()) {
                Result.failure(IllegalStateException("멤버에 연결된 사용자 정보($userId)를 찾을 수 없습니다."))
            } else {
                val joinedAtTimestamp = memberDoc.getTimestamp(MemberFields.JOINED_AT)
                val joinedAtInstant = joinedAtTimestamp?.let { DateTimeUtil.firebaseTimestampToInstant(it) }
                    ?: Instant.EPOCH

                // 역할 정보 가져오기
                val roles = getRolesForMember(projectId, roleIds)

                val member = ProjectMember(
                    userId = userId,
                    userName = userDoc.getString(UserFields.NAME) ?: "사용자",
                    profileImageUrl = userDoc.getString(UserFields.PROFILE_IMAGE_URL),
                    roles = roles, // roleIds 대신 roles 사용
                    joinedAt = joinedAtInstant
                )
                Result.success(member)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 프로젝트에 새 멤버를 추가합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 추가할 사용자 ID
     * @param roleIds 부여할 역할 ID 목록 (Domain 모델 변경과 별개로 여기서는 ID 목록을 받음)
     * @return 작업 성공 여부
     */
    override suspend fun addMemberToProject(
        projectId: String,
        userId: String,
        roleIds: List<String> // 이 메서드는 외부(UseCase)에서 ID 목록을 받아 처리
    ): Result<Unit> = try {
        val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId)
            .get()
            .await()

        if (!projectDoc.exists()) {
            Result.failure(IllegalArgumentException("존재하지 않는 프로젝트입니다."))
        } else {
            val ownerId = projectDoc.getString(ProjectFields.OWNER_ID)
            val currentId = auth.currentUser?.uid

            if (ownerId != currentId && currentId != null /* TODO: 더 정교한 권한 확인 */ ) {
                 // 실제 구현에서는 사용자의 역할에 따른 권한 확인 로직 추가 (예: 프로젝트 관리자 역할 확인)
                 // 여기서는 일단 소유자만 추가 가능하도록 단순화
                 // Result.failure(IllegalArgumentException("멤버를 추가할 권한이 없습니다."))
                 // 임시로 주석 처리. 실제 권한 로직 필요.
            }

            val memberData = hashMapOf(
                MemberFields.ROLE_IDS to roleIds, // Firestore에는 roleIds 저장
                MemberFields.JOINED_AT to DateTimeUtil.instantToFirebaseTimestamp(Instant.now()),
                MemberFields.CHANNEL_IDS to emptyList<String>()
            )

            firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.MEMBERS).document(userId)
                .set(memberData)
                .await()

            firestore.collection(Collections.USERS).document(userId)
                .update(UserFields.PARTICIPATING_PROJECT_IDS, FieldValue.arrayUnion(projectId))
                .await()

            firestore.collection(Collections.PROJECTS).document(projectId)
                .update(ProjectFields.MEMBER_IDS, FieldValue.arrayUnion(userId))
                .await()

            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 프로젝트에서 멤버를 제거합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 제거할 사용자 ID
     * @return 작업 성공 여부
     */
    override suspend fun removeMemberFromProject(
        projectId: String,
        userId: String
    ): Result<Unit> = try {
        // 현재 사용자 권한 확인 (프로젝트 소유자 또는 관리자인지)
        val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId)
            .get()
            .await()

        if (!projectDoc.exists()) {
            Result.failure(IllegalArgumentException("존재하지 않는 프로젝트입니다."))
        } else {
            val ownerId = projectDoc.getString(ProjectFields.OWNER_ID)

            // 소유자는 제거할 수 없음
            if (userId == ownerId) {
                Result.failure(IllegalArgumentException("프로젝트 소유자는 제거할 수 없습니다."))
            } else {
                val currentId = auth.currentUser?.uid

                // 소유자가 아닌 경우 권한 확인 (간소화된 예시)
                if (ownerId != currentId  /* TODO: 더 정교한 권한 확인 */ ) {
                    // TODO: 실제 구현에서는 사용자의 역할에 따른 권한 확인 로직 추가
                    // Result.failure(IllegalArgumentException("멤버를 제거할 권한이 없습니다."))
                    // 임시로 주석 처리. 실제 권한 로직 필요.
                }
                // 멤버 제거
                firestore.collection(Collections.PROJECTS).document(projectId)
                    .collection(Collections.MEMBERS).document(userId)
                    .delete()

                // 사용자의 참여 프로젝트 목록에서 제거
                firestore.collection(Collections.USERS).document(userId)
                    .update(UserFields.PARTICIPATING_PROJECT_IDS, FieldValue.arrayRemove(projectId))
                    .await()

                // 프로젝트 문서의 memberIds 필드에서 사용자 ID 제거
                firestore.collection(Collections.PROJECTS).document(projectId)
                    .update(ProjectFields.MEMBER_IDS, FieldValue.arrayRemove(userId))
                    .await()

                Result.success(Unit)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 멤버의 역할을 업데이트합니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param roleIds 새로운 역할 ID 목록
     * @return 작업 성공 여부
     */
    override suspend fun updateMemberRoles(
        projectId: String,
        userId: String,
        roleIds: List<String>
    ): Result<Unit> = try {
        // 현재 사용자 권한 확인 (프로젝트 소유자 또는 관리자인지)
        val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId)
            .get()
            .await()

        if (!projectDoc.exists()) {
            Result.failure(IllegalArgumentException("존재하지 않는 프로젝트입니다."))
        } else {
            val ownerId = projectDoc.getString(ProjectFields.OWNER_ID)
            val currentId = auth.currentUser?.uid

            // 소유자가 아닌 경우 권한 확인 (간소화된 예시)
            if (ownerId != currentId  /* TODO: 더 정교한 권한 확인 */ ) {
                // TODO: 실제 구현에서는 사용자의 역할에 따른 권한 확인 로직 추가
                // Result.failure(IllegalArgumentException("멤버 역할을 변경할 권한이 없습니다."))
                // 임시로 주석 처리. 실제 권한 로직 필요.
            }
            // 멤버 문서 업데이트
            firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.MEMBERS).document(userId)
                .update(MemberFields.ROLE_IDS, roleIds) // Firestore에는 roleIds를 직접 업데이트
                .await()

            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 멤버에게 채널 접근 권한을 추가합니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param channelId 채널 ID
     * @return 작업 성공 여부
     */
    override suspend fun addChannelAccessToMember(
        projectId: String,
        userId: String,
        channelId: String
    ): Result<Unit> = try {
        // 프로젝트 존재 여부 확인
        val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId)
            .get()
            .await()

        if (!projectDoc.exists()) {
            Result.failure(IllegalArgumentException("존재하지 않는 프로젝트입니다."))
        } else {
            // 멤버 문서 참조
            val memberRef = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.MEMBERS).document(userId)

            // 멤버 문서 확인
            val memberDoc = memberRef.get().await()

            if (memberDoc.exists()) {
                // 기존 멤버 문서의 채널 접근 권한에 추가
                memberRef.update(MemberFields.CHANNEL_IDS, FieldValue.arrayUnion(channelId))
                    .await()

                Result.success(Unit)
            } else {
                // 멤버가 존재하지 않는 경우 오류 반환
                Result.failure(IllegalArgumentException("프로젝트에 존재하지 않는 멤버입니다."))
            }
        }
    } catch (e: Exception) {
        Log.e("ProjectMemberDataSource", "Error adding channel access to member", e)
        Result.failure(e)
    }

    /**
     * 멤버의 채널 접근 권한을 제거합니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param channelId 채널 ID
     * @return 작업 성공 여부
     */
    override suspend fun removeChannelAccessFromMember(
        projectId: String,
        userId: String,
        channelId: String
    ): Result<Unit> = try {
        // 프로젝트 존재 여부 확인
        val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId)
            .get()
            .await()

        if (!projectDoc.exists()) {
            Result.failure(IllegalArgumentException("존재하지 않는 프로젝트입니다."))
        } else {
            // 멤버 문서 참조
            val memberRef = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.MEMBERS).document(userId)

            // 멤버 문서 확인
            val memberDoc = memberRef.get().await()

            if (memberDoc.exists()) {
                // 기존 멤버 문서의 채널 접근 권한에서 제거
                memberRef.update(MemberFields.CHANNEL_IDS, FieldValue.arrayRemove(channelId))
                    .await()

                Result.success(Unit)
            } else {
                // 멤버가 존재하지 않는 경우 오류 반환
                Result.failure(IllegalArgumentException("프로젝트에 존재하지 않는 멤버입니다."))
            }
        }
    } catch (e: Exception) {
        Log.e("ProjectMemberDataSource", "Error removing channel access from member", e)
        Result.failure(e)
    }

    /**
     * 채널에 접근 가능한 모든 멤버 ID 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 멤버 ID 목록
     */
    override suspend fun getMembersWithChannelAccess(
        projectId: String,
        channelId: String
    ): Result<List<String>> = try {
        // 프로젝트 멤버 컬렉션 참조
        val membersCollection = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.MEMBERS)

        // 멤버 목록 가져오기
        val membersSnapshot = membersCollection.get().await()

        // 특정 채널에 접근 가능한 멤버 ID 필터링
        val memberIds = membersSnapshot.documents.filter { memberDoc ->
            val channelIds = memberDoc.get(MemberFields.CHANNEL_IDS)?.let { data ->
                if (data is List<*>) {
                    data.mapNotNull { it as? String }
                } else {
                    emptyList()
                }
            } ?: emptyList()

            channelIds.contains(channelId)
        }.map { it.id }

        Result.success(memberIds)
    } catch (e: Exception) {
        Log.e("ProjectMemberDataSource", "Error getting members with channel access", e)
        Result.failure(e)
    }

    /**
     * 멤버가 접근 가능한 모든 채널 ID 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return 채널 ID 목록
     */
    override suspend fun getMemberChannelAccess(
        projectId: String,
        userId: String
    ): Result<List<String>> = try {
        // 멤버 문서 참조
        val memberDoc = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.MEMBERS).document(userId)
            .get()
            .await()

        if (!memberDoc.exists()) {
            // 멤버가 존재하지 않는 경우 빈 목록 반환
            Result.success(emptyList())
        } else {
            // 멤버가 접근 가능한 채널 ID 목록 가져오기
            val channelIds = memberDoc.get(MemberFields.CHANNEL_IDS)?.let { data ->
                if (data is List<*>) {
                    data.mapNotNull { it as? String }
                } else {
                    emptyList()
                }
            } ?: emptyList()

            Result.success(channelIds)
        }
    } catch (e: Exception) {
        Log.e("ProjectMemberDataSource", "Error getting member channel access", e)
        Result.failure(e)
    }
} 