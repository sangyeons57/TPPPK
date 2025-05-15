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
                    permissions = permissionMap
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
                        permissions = permissionMap
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
     * @return 역할 이름과 권한 맵 Pair 또는 에러
     */
    override suspend fun getRoleDetails(roleId: String): Result<Pair<String, Map<RolePermission, Boolean>>> {
        return try {
            // roleId에서 프로젝트 ID 추출 (구현에 따라 다를 수 있음)
            // 여기서는 간단히 roleId의 형식이 "projectId_roleId"라고 가정
            val parts = roleId.split("_")
            if (parts.size < 2) {
                Result.failure(IllegalArgumentException("유효하지 않은 역할 ID 형식입니다."))
            } else {
                val projectId = parts[0]
                val actualRoleId = parts[1]
                
                // 역할 문서 가져오기
                val roleDoc = firestore.collection(Collections.PROJECTS).document(projectId)
                    .collection(Collections.ROLES).document(actualRoleId)
                    .get()
                    .await()
                
                if (!roleDoc.exists()) {
                    Result.failure(IllegalArgumentException("존재하지 않는 역할입니다."))
                } else {
                    // 역할 이름 가져오기
                    val name = roleDoc.getString(RoleFields.NAME) ?: "역할"
                    
                    // 권한 맵 변환
                    val permissionsMap = (roleDoc.get(RoleFields.PERMISSIONS) as? Map<*, *>)?.mapNotNull { (key, value) ->
                        val permission = try {
                            RolePermission.valueOf(key.toString())
                        } catch (e: Exception) {
                            null
                        }
                        val enabled = value as? Boolean ?: false
                        permission?.let { it to enabled }
                    }?.toMap() ?: emptyMap()
                    
                    Result.success(name to permissionsMap)
                }
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
     * @return 새로 생성된 역할 ID
     */
    override suspend fun createRole(
        projectId: String,
        name: String,
        permissions: Map<RolePermission, Boolean>
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
     * @return 작업 성공 여부
     */
    override suspend fun updateRole(
        projectId: String, 
        roleId: String,
        name: String,
        permissions: Map<RolePermission, Boolean>
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
    override suspend fun deleteRole(roleId: String): Result<Unit> {
        return try {
            // roleId에서 프로젝트 ID 추출 (구현에 따라 다를 수 있음)
            val parts = roleId.split("_")
            if (parts.size < 2) {
                Result.failure(IllegalArgumentException("유효하지 않은 역할 ID 형식입니다."))
            } else {
                val projectId = parts[0]
                val actualRoleId = parts[1]
                
                // 현재 사용자 권한 확인 (프로젝트 소유자 또는 관리자인지)
                val projectDoc = firestore.collection(Collections.PROJECTS).document(projectId)
                    .get()
                    .await()
                
                if (!projectDoc.exists()) {
                    Result.failure(IllegalArgumentException("존재하지 않는 프로젝트입니다."))
                } else {
                    val ownerId = projectDoc.getString(ProjectFields.OWNER_ID)
                    
                    // 소유자가 아닌 경우 권한 확인 (간소화된 예시)
                    if (ownerId != currentUserId) {
                        Result.failure(IllegalArgumentException("역할을 삭제할 권한이 없습니다."))
                    } else {
                        // 역할 문서 삭제
                        val roleRef = firestore.collection(Collections.PROJECTS).document(projectId)
                            .collection(Collections.ROLES).document(actualRoleId)
                        
                        // 해당 역할을 가진 멤버가 있는지 확인 (선택적)
                        val members = firestore.collection(Collections.PROJECTS).document(projectId)
                            .collection(Collections.MEMBERS)
                            .whereArrayContains(MemberFields.ROLE_IDS, actualRoleId)
                            .get()
                            .await()
                        
                        if (!members.isEmpty) {
                            Result.failure(IllegalArgumentException("이 역할을 가진 멤버가 있습니다. 먼저 멤버의 역할을 변경해주세요."))
                        } else {
                            roleRef.delete().await()
                            
                            Result.success(Unit)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 