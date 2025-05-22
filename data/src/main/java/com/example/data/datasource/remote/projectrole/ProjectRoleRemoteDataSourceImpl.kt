package com.example.data.datasource.remote.projectrole

import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.MemberFields
import com.example.core_common.constants.FirestoreConstants.ProjectFields
import com.example.core_common.constants.FirestoreConstants.RoleFields
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.Role
import com.example.domain.model.RolePermission
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.util.Log

/**
 * 프로젝트 역할 관련 원격 데이터 소스 구현
 * Firebase Firestore를 사용하여 프로젝트 내 역할 및 권한 관련 기능을 구현합니다.
 * @param firestore Firebase Firestore 인스턴스
 * @param auth Firebase Auth 인스턴스
 */
class ProjectRoleRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ProjectRoleRemoteDataSource {

    // 현재 사용자 ID를 가져오는 헬퍼 함수
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("사용자가 로그인되어 있지 않습니다.")

    // Firestore List<String>을 List<RolePermission>으로 변환하는 헬퍼 함수
    private fun mapFirestorePermissionsToDomain(firestorePermissions: Any?): List<RolePermission> {
        return (firestorePermissions as? List<*>)?.mapNotNull { permString ->
            try {
                RolePermission.valueOf(permString.toString())
            } catch (e: IllegalArgumentException) {
                Log.w("ProjectRoleDataSource", "Unknown permission string: $permString")
                null
            }
        } ?: emptyList()
    }

    // Domain List<RolePermission>을 Firestore List<String>으로 변환하는 헬퍼 함수
    private fun mapDomainPermissionsToFirestore(domainPermissions: List<RolePermission>): List<String> {
        return domainPermissions.map { it.name }
    }

    /**
     * 특정 프로젝트의 모든 역할 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 역할 목록
     */
    override suspend fun getRoles(projectId: String): Result<List<Role>> = try {
        val rolesCollection = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.ROLES)
        val rolesSnapshot = rolesCollection.get().await()
        val roles = rolesSnapshot.documents.mapNotNull { doc ->
            try {
                val permissionsList = mapFirestorePermissionsToDomain(doc.get(RoleFields.PERMISSIONS))
                Role(
                    id = doc.id,
                    projectId = projectId,
                    name = doc.getString(RoleFields.NAME) ?: "역할",
                    permissions = permissionsList,
                    isDefault = doc.getBoolean(RoleFields.IS_DEFAULT) ?: false,
                    memberCount = doc.getLong("memberCount")?.toInt() // Firestore에서 직접 읽음
                )
            } catch (e: Exception) {
                Log.e("ProjectRoleDataSource", "Error mapping role document: ${doc.id}", e)
                null
            }
        }
        Result.success(roles)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 특정 프로젝트의 역할 목록 실시간 스트림을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 역할 목록의 Flow
     */
    override fun getRolesStream(projectId: String): Flow<List<Role>> = callbackFlow {
        val rolesCollection = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.ROLES)
        val subscription = rolesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList()).isSuccess
                close(error)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(emptyList()).isSuccess
                return@addSnapshotListener
            }
            val roles = snapshot.documents.mapNotNull { doc ->
                try {
                    val permissionsList = mapFirestorePermissionsToDomain(doc.get(RoleFields.PERMISSIONS))
                    Role(
                        id = doc.id,
                        projectId = projectId,
                        name = doc.getString(RoleFields.NAME) ?: "역할",
                        permissions = permissionsList,
                        isDefault = doc.getBoolean(RoleFields.IS_DEFAULT) ?: false,
                        memberCount = doc.getLong("memberCount")?.toInt()
                    )
                } catch (e: Exception) {
                    Log.e("ProjectRoleDataSource", "Error mapping role document in stream: ${doc.id}", e)
                    null
                }
            }
            trySend(roles).isSuccess
        }
        awaitClose { subscription.remove() }
    }

    /**
     * 특정 프로젝트의 역할 목록을 Firestore에서 가져옵니다. (주로 초기 로딩 또는 강제 새로고침용)
     * @param projectId 프로젝트 ID
     * @return 작업 성공 여부 (이 메서드는 주로 데이터를 로컬에 캐싱하거나 상태를 업데이트하는 데 사용될 수 있으며, 직접적인 데이터 반환보다는 작업 완료를 알림)
     */
    override suspend fun fetchRoles(projectId: String): Result<Unit> {
        // getRoles를 호출하여 데이터를 가져오고, 성공/실패 여부만 반환할 수 있으나
        // Firestore는 실시간 동기화를 제공하므로, 별도의 fetch가 반드시 필요하지 않을 수 있음.
        // 여기서는 getRoles를 호출하고 그 결과를 Unit으로 변환하여 반환하는 예시 (실제 사용처에 따라 구현 달라질 수 있음)
        return getRoles(projectId).map { /* Unit */ } 
    }

    /**
     * 특정 역할의 상세 정보를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param roleId 역할 ID
     * @return 역할 정보 또는 null (에러 발생 시 Result.failure)
     */
    override suspend fun getRoleDetails(projectId: String, roleId: String): Result<Role?> {
        return try {
            val roleDoc = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.ROLES).document(roleId)
                .get()
                .await()

            if (!roleDoc.exists()) {
                Result.success(null)
            } else {
                val permissionsList = mapFirestorePermissionsToDomain(roleDoc.get(RoleFields.PERMISSIONS))
                val role = Role(
                    id = roleId,
                    projectId = projectId,
                    name = roleDoc.getString(RoleFields.NAME) ?: "역할",
                    permissions = permissionsList,
                    isDefault = roleDoc.getBoolean(RoleFields.IS_DEFAULT) ?: false,
                    memberCount = roleDoc.getLong("memberCount")?.toInt()
                )
                Result.success(role)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 새 역할을 생성합니다.
     * @param projectId 프로젝트 ID
     * @param name 역할 이름
     * @param permissions 부여할 권한 리스트 (Domain 모델에 맞춰 List<RolePermission>으로 변경)
     * @param isDefault 기본 역할 여부
     * @return 새로 생성된 역할 ID
     */
    override suspend fun createRole(
        projectId: String,
        name: String,
        permissions: List<RolePermission>, // Map에서 List로 변경
        isDefault: Boolean
    ): Result<String> {
        return try {
            val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId).get().await()
            if (!projectDoc.exists()) {
                Result.failure(IllegalArgumentException("존재하지 않는 프로젝트입니다."))
            } else {
                val firestorePermissions = mapDomainPermissionsToFirestore(permissions) // List<String>으로 변환
                val nowTimestamp = DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.nowInstant())

                val roleData: Map<String, Any?> = mapOf(
                    RoleFields.NAME to name,
                    RoleFields.PERMISSIONS to firestorePermissions,
                    RoleFields.IS_DEFAULT to isDefault,
                    RoleFields.CREATED_AT to nowTimestamp,
                    RoleFields.UPDATED_AT to nowTimestamp,
                    "memberCount" to 0
                )

                val roleRef = firestore.collection(Collections.PROJECTS).document(projectId)
                    .collection(Collections.ROLES).document()
                roleRef.set(roleData).await()
                Result.success(roleRef.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 역할을 업데이트합니다.
     * @param projectId 프로젝트 ID
     * @param roleId 역할 ID
     * @param name 새 역할 이름 (null이면 변경하지 않음)
     * @param permissions 새 권한 리스트 (null이면 변경하지 않음)
     * @param isDefault 기본 역할 여부 (null이면 변경하지 않음)
     * @return 작업 성공 여부
     */
    override suspend fun updateRole(
        projectId: String,
        roleId: String,
        name: String?,
        permissions: List<RolePermission>?, // Map에서 List로 변경
        isDefault: Boolean?
    ): Result<Unit> {
        return try {
            val roleRef = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.ROLES).document(roleId)
            
            // 역할 존재 여부 확인 (선택적, Firestore 업데이트는 문서가 없으면 실패하지 않음)
            // val roleDoc = roleRef.get().await()
            // if (!roleDoc.exists()) {
            //     return Result.failure(IllegalArgumentException("수정하려는 역할이 존재하지 않습니다."))
            // }

            val updates = mutableMapOf<String, Any?>()
            name?.let { updates[RoleFields.NAME] = it }
            permissions?.let { updates[RoleFields.PERMISSIONS] = mapDomainPermissionsToFirestore(it) } // 변환된 리스트 저장
            isDefault?.let { updates[RoleFields.IS_DEFAULT] = it }
            
            if (updates.isNotEmpty()) {
                updates[RoleFields.UPDATED_AT] = DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.nowInstant()) // Timestamp? 타입
                roleRef.update(updates).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 역할을 삭제합니다.
     * @param projectId 프로젝트 ID
     * @param roleId 역할 ID
     * @return 작업 성공 여부
     */
    override suspend fun deleteRole(projectId: String, roleId: String): Result<Unit> {
        return try {
            // TODO: 역할 삭제 시 해당 역할을 가진 멤버들의 roleIds에서 이 roleId를 제거하는 로직 필요
            // 이는 트랜잭션으로 처리하거나 Cloud Function을 사용하는 것이 안전할 수 있음.
            // 역할이 isDefault=true인 경우 삭제 정책 고려 (예: 삭제 불가 또는 다른 기본 역할 지정 필요)

            val roleRef = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.ROLES).document(roleId)
            
            val roleDoc = roleRef.get().await()
            if (roleDoc.getBoolean(RoleFields.IS_DEFAULT) == true) {
                return Result.failure(IllegalArgumentException("기본 역할은 삭제할 수 없습니다."))
            }

            // 실제 역할 삭제 전에 해당 역할을 가진 멤버가 있는지 확인하고, 있다면 역할을 제거하거나 다른 역할로 변경하는 로직이 필요할 수 있다.
            // 여기서는 역할을 가진 멤버 수를 확인하고, 0이 아니면 삭제를 막는 예시 (memberCount 필드 사용)
            val memberCount = roleDoc.getLong("memberCount")?.toInt() ?: 0
            if (memberCount > 0) {
                // 대안: 이 역할을 가진 멤버들에게서 이 역할 ID를 제거하고 memberCount를 0으로 만든 후 삭제
                // return Result.failure(IllegalStateException("해당 역할을 가진 멤버가 있어 삭제할 수 없습니다. 먼저 멤버들의 역할을 변경해주세요."))
                // 여기서는 일단 경고만 하고 삭제 진행 (실제 정책에 따라 달라짐)
                Log.w("ProjectRoleDataSource", "Deleting role '$roleId' which has $memberCount members.")
            }

            roleRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 역할에 멤버를 추가하고 해당 역할의 memberCount를 1 증가시킵니다.
     * 트랜잭션으로 처리되어야 합니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param roleId 역할 ID
     * @return 작업 성공 여부
     */
    override suspend fun addMemberToRoleAndUpdateCount(projectId: String, userId: String, roleId: String): Result<Unit> {
        val roleRef = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.ROLES).document(roleId)
        val memberRef = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.MEMBERS).document(userId)

        return firestore.runTransaction { transaction ->
            val roleSnapshot = transaction.get(roleRef)
            if (!roleSnapshot.exists()) {
                throw Exception("Role with ID $roleId not found in project $projectId")
            }

            // 멤버 문서에 역할 ID 추가
            transaction.update(memberRef, MemberFields.ROLE_IDS, FieldValue.arrayUnion(roleId))
            // 역할 문서의 memberCount 증가
            transaction.update(roleRef, "memberCount", FieldValue.increment(1))
            Result.success(Unit) // 트랜잭션 내에서 Result를 직접 반환하기보다는, 성공 시 Void, 실패 시 Exception을 던지도록 함
        }.await()
    }

    /**
     * 역할에서 멤버를 제거하고 해당 역할의 memberCount를 1 감소시킵니다.
     * 트랜잭션으로 처리되어야 합니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param roleId 역할 ID
     * @return 작업 성공 여부
     */
    override suspend fun removeMemberFromRoleAndUpdateCount(projectId: String, userId: String, roleId: String): Result<Unit> {
        val roleRef = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.ROLES).document(roleId)
        val memberRef = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.MEMBERS).document(userId)

        return firestore.runTransaction { transaction ->
            val roleSnapshot = transaction.get(roleRef)
            if (!roleSnapshot.exists()) {
                throw Exception("Role with ID $roleId not found in project $projectId")
            }
            val currentMemberCount = roleSnapshot.getLong("memberCount") ?: 0
            if (currentMemberCount <= 0) {
                 Log.w("ProjectRoleDataSource", "Attempting to decrement member count for role $roleId which is already $currentMemberCount")
                // memberCount가 0 이하면 더 이상 감소시키지 않거나, 0으로 설정
            }

            // 멤버 문서에서 역할 ID 제거
            transaction.update(memberRef, MemberFields.ROLE_IDS, FieldValue.arrayRemove(roleId))
            // 역할 문서의 memberCount 감소 (0 미만으로 내려가지 않도록)
            transaction.update(roleRef, "memberCount", FieldValue.increment(-1))
            Result.success(Unit) // 트랜잭션 내에서 Result.success(Unit)을 직접 반환하는 것은 Firestore API와 다를 수 있음.
        }.await()
    }
}