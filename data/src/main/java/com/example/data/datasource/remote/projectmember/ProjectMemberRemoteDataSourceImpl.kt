package com.example.data.datasource.remote.projectmember

import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.MemberFields
import com.example.core_common.constants.FirestoreConstants.ProjectFields
import com.example.core_common.constants.FirestoreConstants.RoleFields
import com.example.core_common.constants.FirestoreConstants.UserFields
import com.example.domain.model.ProjectMember
import com.example.domain.model.Role
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

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

    /**
     * 특정 프로젝트의 모든 멤버 목록을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 목록
     */
    override suspend fun getProjectMembers(projectId: String): Result<List<ProjectMember>> = try {
        // 프로젝트 멤버 컬렉션 참조
        val membersCollection = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.MEMBERS)
        
        // 멤버 목록 가져오기
        val membersSnapshot = membersCollection.get().await()
        
        // 멤버 목록 매핑
        val members = mutableListOf<ProjectMember>()
        
        for (memberDoc in membersSnapshot.documents) {
            val userId = memberDoc.id
            // 역할 ID 목록 안전하게 가져오기
            val roleIds = memberDoc.get(MemberFields.ROLE_IDS)?.let { data ->
                if (data is List<*>) {
                    data.mapNotNull { it as? String }
                } else {
                    emptyList()
                }
            } ?: emptyList()
            
            // 사용자 정보 가져오기
            val userDoc = firestore.collection(Collections.USERS).document(userId).get().await()
            if (!userDoc.exists()) continue
            
            // 역할 정보 가져오기
            val roleNames = mutableListOf<String>()
            for (roleId in roleIds) {
                val roleDoc = firestore.collection(Collections.PROJECTS).document(projectId)
                    .collection(Collections.ROLES).document(roleId).get().await()
                if (roleDoc.exists()) {
                    val roleName = roleDoc.getString(RoleFields.NAME) ?: "역할"
                    roleNames.add(roleName)
                }
            }
            
            // 멤버 객체 생성
            val member = ProjectMember(
                userId = userId,
                userName = userDoc.getString(UserFields.NICKNAME) ?: "사용자",
                profileImageUrl = userDoc.getString(UserFields.PROFILE_IMAGE_URL),
                roleNames = roleNames
            )
            
            members.add(member)
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
        // 프로젝트 멤버 컬렉션 참조
        val membersCollection = firestore.collection(Collections.PROJECTS).document(projectId)
            .collection(Collections.MEMBERS)
        
        // 실시간 스냅샷 리스너 설정
        val subscription = membersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // 에러 발생 시 빈 목록 전송
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            if (snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            // 멤버 목록 처리
            firestore.runTransaction { transaction ->
                val members = mutableListOf<ProjectMember>()
                
                for (memberDoc in snapshot.documents) {
                    val userId = memberDoc.id
                    // 역할 ID 목록 안전하게 가져오기
                    val roleIds = memberDoc.get(MemberFields.ROLE_IDS)?.let { data ->
                        if (data is List<*>) {
                            data.mapNotNull { it as? String }
                        } else {
                            emptyList()
                        }
                    } ?: emptyList()
                    
                    // 사용자 정보 가져오기
                    val userDoc = transaction.get(firestore.collection(Collections.USERS).document(userId))
                    if (!userDoc.exists()) continue
                    
                    // 역할 이름 목록 가져오기
                    val roleNames = mutableListOf<String>()
                    for (roleId in roleIds) {
                        val roleDoc = transaction.get(
                            firestore.collection(Collections.PROJECTS).document(projectId)
                                .collection(Collections.ROLES).document(roleId)
                        )
                        if (roleDoc.exists()) {
                            val roleName = roleDoc.getString(RoleFields.NAME) ?: "역할"
                            roleNames.add(roleName)
                        }
                    }
                    
                    // 멤버 객체 생성
                    val member = ProjectMember(
                        userId = userId,
                        userName = userDoc.getString(UserFields.NICKNAME) ?: "사용자",
                        profileImageUrl = userDoc.getString(UserFields.PROFILE_IMAGE_URL),
                        roleNames = roleNames
                    )
                    
                    members.add(member)
                }
                
                // 트랜잭션은 값을 반환할 수 없으므로 별도로 멤버 목록 반환
                members
            }.addOnSuccessListener { members ->
                // 멤버 목록 전송
                trySend(members)
            }.addOnFailureListener {
                // 에러 발생 시 빈 목록 전송
                trySend(emptyList())
            }
        }
        
        // 구독 취소 시 스냅샷 리스너 제거
        awaitClose { subscription.remove() }
    }

    /**
     * 프로젝트에 새 멤버를 추가합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 추가할 사용자 ID
     * @param roleIds 부여할 역할 ID 목록
     * @return 작업 성공 여부
     */
    override suspend fun addMemberToProject(
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
            if (ownerId != currentId) {
                // TODO: 실제 구현에서는 사용자의 역할에 따른 권한 확인 로직 추가
                Result.failure(IllegalArgumentException("멤버를 추가할 권한이 없습니다."))
            } else {
                // 멤버 추가
                val memberData = hashMapOf(
                    MemberFields.ROLE_IDS to roleIds,
                    MemberFields.ADDED_AT to FieldValue.serverTimestamp(),
                    MemberFields.ADDED_BY to currentUserId
                )
                
                // 멤버 문서 생성/업데이트
                firestore.collection(Collections.PROJECTS).document(projectId)
                    .collection(Collections.MEMBERS).document(userId)
                    .set(memberData)
                    .await()
                
                // 사용자의 참여 프로젝트 목록에 추가
                firestore.collection(Collections.USERS).document(userId)
                    .update(UserFields.PARTICIPATING_PROJECT_IDS, FieldValue.arrayUnion(projectId))
                    .await()
                
                Result.success(Unit)
            }
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
                if (ownerId != currentId) {
                    // TODO: 실제 구현에서는 사용자의 역할에 따른 권한 확인 로직 추가
                    Result.failure(IllegalArgumentException("멤버를 제거할 권한이 없습니다."))
                } else {
                    // 멤버 제거
                    firestore.collection(Collections.PROJECTS).document(projectId)
                        .collection(Collections.MEMBERS).document(userId)
                        .delete()
                        .await()
                    
                    // 사용자의 참여 프로젝트 목록에서 제거
                    firestore.collection(Collections.USERS).document(userId)
                        .update(UserFields.PARTICIPATING_PROJECT_IDS, FieldValue.arrayRemove(projectId))
                        .await()
                    
                    Result.success(Unit)
                }
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
            if (ownerId != currentId) {
                // TODO: 실제 구현에서는 사용자의 역할에 따른 권한 확인 로직 추가
                Result.failure(IllegalArgumentException("멤버 역할을 변경할 권한이 없습니다."))
            } else {
                // 역할 업데이트
                val updateData = hashMapOf(
                    MemberFields.ROLE_IDS to roleIds,
                    MemberFields.UPDATED_AT to FieldValue.serverTimestamp(),
                    MemberFields.UPDATED_BY to currentUserId
                )
                
                // 멤버 문서 업데이트
                firestore.collection(Collections.PROJECTS).document(projectId)
                    .collection(Collections.MEMBERS).document(userId)
                    .update(updateData as Map<String, Any>)
                    .await()
                
                Result.success(Unit)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
} 