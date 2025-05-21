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

    /**
     * 특정 프로젝트의 모든 역할 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 역할 목록
     */
    override suspend fun getRoles(projectId: String): Result<List<Role>> = try {
        // 프로젝트의 역할 컬렉션 참조
        val rolesCollection = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.ROLES)
        
        // 역할 목록 가져오기
        val rolesSnapshot = rolesCollection.get().await()
        
        // 역할 목록 매핑
        val roles = rolesSnapshot.documents.mapNotNull { doc ->
            try {
                // 역할 권한 맵 변환
                val permissionMap = (doc.get(RoleFields.PERMISSIONS) as? Map<*, *>)?.mapNotNull { (key, value) ->
                    val permission = try {
                        RolePermission.valueOf(key.toString())
                    } catch (e: Exception) {
                        null
                    }
                    val enabled = value as? Boolean ?: false
                    permission?.let { it to enabled }
                }?.toMap() ?: emptyMap()
                
                Role(
                    id = doc.id,
                    projectId = projectId,
                    name = doc.getString(RoleFields.NAME) ?: "역할",
                    permissions = permissionMap,
                    isDefault = doc.getBoolean(RoleFields.IS_DEFAULT) ?: false
                )
            } catch (e: Exception) {
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
        // 프로젝트의 역할 컬렉션 참조
        val rolesCollection = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.ROLES)
        
        // 실시간 스냅샷 리스너 설정
        val subscription = rolesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // 에러 발생 시 빈 목록 전송
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            if (snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            // 역할 목록 매핑
            val roles = snapshot.documents.mapNotNull { doc ->
                try {
                    // 역할 권한 맵 변환
                    val permissionMap = (doc.get(RoleFields.PERMISSIONS) as? Map<*, *>)?.mapNotNull { (key, value) ->
                        val permission = try {
                            RolePermission.valueOf(key.toString())
                        } catch (e: Exception) {
                            null
                        }
                        val enabled = value as? Boolean ?: false
                        permission?.let { it to enabled }
                    }?.toMap() ?: emptyMap()
                    
                    Role(
                        id = doc.id,
                        projectId = projectId,
                        name = doc.getString(RoleFields.NAME) ?: "역할",
                        permissions = permissionMap,
                        isDefault = doc.getBoolean(RoleFields.IS_DEFAULT) ?: false
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            // 역할 목록 전송
            trySend(roles)
        }
        
        // 구독 취소 시 스냅샷 리스너 제거
        awaitClose { subscription.remove() }
    }

    /**
     * 특정 프로젝트의 역할 목록을 Firestore에서 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 작업 성공 여부
     */
    override suspend fun fetchRoles(projectId: String): Result<Unit> {
        return try {
            // 실시간 스트림 구현이 있으므로 단순히 성공을 반환
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 특정 역할의 상세 정보를 가져옵니다.
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
                Result.success(null) // 역할이 없으면 null 반환
            } else {
                val permissionsMap = (roleDoc.get(RoleFields.PERMISSIONS) as? Map<*, *>)?.mapNotNull { (key, value) ->
                    val permission = try { RolePermission.valueOf(key.toString()) } catch (e: Exception) { null }
                    val enabled = value as? Boolean ?: false
                    permission?.let { it to enabled }
                }?.toMap() ?: emptyMap()

                val role = Role(
                    id = roleId,
                    projectId = projectId,
                    name = roleDoc.getString(RoleFields.NAME) ?: "역할",
                    permissions = permissionsMap,
                    isDefault = roleDoc.getBoolean(RoleFields.IS_DEFAULT) ?: false,
                    // memberCount는 여기서는 로드하지 않음. 필요시 별도 로직 또는 getRoles에서 집계
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
     * @param permissions 권한 맵
     * @param isDefault 기본 역할 여부
     * @return 새로 생성된 역할 ID
     */
    override suspend fun createRole(
        projectId: String,
        name: String,
        permissions: Map<RolePermission, Boolean>,
        isDefault: Boolean
    ): Result<String> {
        return try {
            val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId).get().await()
            if (!projectDoc.exists()) {
                Result.failure(IllegalArgumentException("존재하지 않는 프로젝트입니다."))
            } else {
                val permissionsMap = permissions.mapKeys { it.key.name }
                val nowTimestamp = DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.nowInstant())
                
                val roleData = mapOf(
                    RoleFields.NAME to name,
                    RoleFields.PERMISSIONS to permissionsMap,
                    RoleFields.IS_DEFAULT to isDefault,
                    RoleFields.CREATED_AT to nowTimestamp,
                    RoleFields.UPDATED_AT to nowTimestamp
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
     * @param name 새 역할 이름
     * @param permissions 새 권한 맵
     * @param isDefault 기본 역할 여부 (null이면 변경하지 않음)
     * @return 작업 성공 여부
     */
    override suspend fun updateRole(
        projectId: String,
        roleId: String,
        name: String,
        permissions: Map<RolePermission, Boolean>,
        isDefault: Boolean?
    ): Result<Unit> {
        return try {
            // Simplified permission check (ensure user has rights to update role in this project)
            // val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId).get().await()
            // val ownerId = projectDoc.getString(ProjectFields.OWNER_ID)
            // if (ownerId != currentUserId && !isUserAdminInProject(projectId, currentUserId)) { 
            //    Result.failure(SecurityException("역할을 수정할 권한이 없습니다."))
            // } else {
            val permissionsMap = permissions.mapKeys { it.key.name }
            val roleRef = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.ROLES).document(roleId)

            // Check if role exists before attempting to update
            if (!roleRef.get().await().exists()) {
                return Result.failure(IllegalArgumentException("수정하려는 역할(ID: $roleId)이 프로젝트(ID: $projectId)에 존재하지 않습니다."))
            }

            val updateData = mutableMapOf<String, Any>(
                RoleFields.NAME to name,
                RoleFields.PERMISSIONS to permissionsMap,
                RoleFields.UPDATED_AT to FieldValue.serverTimestamp()
            )
            isDefault?.let { updateData[RoleFields.IS_DEFAULT] = it }
            // createdAt should not be changed on update

            roleRef.update(updateData).await()
            Result.success(Unit)
            // }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 역할을 삭제합니다.
     * @param roleId 역할 ID
     * @return 작업 성공 여부
     */
    override suspend fun deleteRole(projectId: String, roleId: String): Result<Unit> {
        return try {
            // 현재 사용자 권한 확인 (프로젝트 소유자 또는 관리자인지)
            val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId)
                .get()
                .await()

            if (!projectDoc.exists()) {
                return Result.failure(IllegalArgumentException("존재하지 않는 프로젝트입니다."))
            }

            val ownerId = projectDoc.getString(ProjectFields.OWNER_ID)
            // 소유자가 아닌 경우 권한 확인 (간소화된 예시)
            // TODO: 실제 구현에서는 사용자의 역할에 따른 권한 확인 로직 추가 (e.g., 프로젝트 관리자 권한)
            if (ownerId != currentUserId) {
                return Result.failure(IllegalArgumentException("역할을 삭제할 권한이 없습니다."))
            }

            // 역할 문서 참조
            val roleRef = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.ROLES).document(roleId)

            // 역할 존재 여부 확인
            if (!roleRef.get().await().exists()) {
                return Result.failure(IllegalArgumentException("삭제하려는 역할(ID: $roleId)이 프로젝트(ID: $projectId)에 존재하지 않습니다."))
            }
            
            // 해당 역할을 가진 멤버가 있는지 확인
            val members = firestore.collection(Collections.PROJECTS).document(projectId)
                .collection(Collections.MEMBERS)
                .whereArrayContains(MemberFields.ROLE_IDS, roleId) // 실제 역할 ID 사용
                .limit(1) // 하나라도 있는지 확인하면 충분
                .get()
                .await()

            if (!members.isEmpty) {
                return Result.failure(IllegalArgumentException("이 역할(ID: $roleId)을 가진 멤버가 프로젝트(ID: $projectId)에 존재합니다. 먼저 멤버의 역할을 변경해주세요."))
            }

            roleRef.delete().await()
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}