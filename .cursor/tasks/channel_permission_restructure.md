# Task: 프로젝트 역할 기반 채널 권한 시스템으로 리팩터링

## 배경
현재 코드베이스에는 두 개의 분리된 권한 시스템이 구현되어 있습니다:
1. **프로젝트 수준 역할 시스템**: `ProjectMember`의 `roleIds`와 `projects/{projectId}/roles/{roleId}` 컬렉션 기반
2. **채널 수준 권한 시스템**: `ChannelPermission` 클래스와 관련 리포지토리를 통한 독립적 구현

이를 Firestore 스키마 문서에 정의된 원래 기획대로 프로젝트 역할 기반의 단일 권한 시스템으로 통합해야 합니다.

## 작업 계획

- [x] **Step 1: ProjectRole 모델 및 권한 시스템 분석**
  - `projects/{projectId}/roles` 컬렉션과 관련된 현재 모델/코드 확인
  - 프로젝트 역할에 권한 정의 필드가 있는지 확인
  - 권한 정의가 없다면, 프로젝트 역할 모델에 권한 관련 필드 추가 계획 수립
  
  **분석 결과:**
  - **Role 모델 분석:**
    - 프로젝트 역할은 `domain/src/main/java/com/example/domain/model/Role.kt`에 정의되어 있음
    - 역할은 이미 `permissions: Map<RolePermission, Boolean>` 필드를 가지고 있어 권한 정의가 가능
    - `RolePermission` enum은 프로젝트 내 여러 권한들(`INVITE_MEMBERS`, `MANAGE_CHANNELS` 등)을 정의
    - `MANAGE_CHANNELS` 권한이 있지만, 세부 채널 작업 권한(`READ_MESSAGES`, `SEND_MESSAGES` 등)은 없음
  - **ChannelPermission 분석:**
    - `ChannelPermission` 클래스는 별도로 존재하며 `ChannelRole`(OWNER, ADMIN, MEMBER 등)과 `ChannelPermissionType`(READ_MESSAGES, SEND_MESSAGES 등) 사용
    - Firestore의 `channels/{channelId}/permissions/{userId}` 경로에 채널별 권한 데이터 저장
  - **문제점:**
    - 현재 `RolePermission`에는 채널 수준의 세부 권한(`READ_MESSAGES`, `SEND_MESSAGES` 등)이 없음
    - `ChannelPermission`은 프로젝트 역할과 연결되어 있지 않으며, 독립적으로 저장/관리됨
    - 결론적으로 두 시스템이 완전히 분리되어 있음

- [x] **Step 2: ChannelPermission 클래스 수정 계획**
  - `ChannelPermission`을 프로젝트 역할 기반으로 동작하도록 수정 방안 정의
  - `ChannelPermission` 새 구조 설계 (프로젝트 역할 참조 방식, 권한 확인 로직 등)

  **수정 계획:**
  - **채널 권한 관련 Enum 수정**:
    1. `ChannelPermissionType`의 항목들을 `RolePermission` Enum에 채널 관련 권한으로 추가
        ```kotlin
        // 추가할 채널 관련 권한들
        READ_MESSAGES("채널 메시지 읽기 권한"),
        SEND_MESSAGES("채널 메시지 전송 권한"),
        DELETE_MESSAGES("자신의 메시지 삭제 권한"),
        UPLOAD_FILES("파일 업로드 권한"),
        MENTION_MEMBERS("멤버 언급(@) 권한")
        ```

  - **ChannelPermission 클래스 구조 변경**:
    1. 현재: `ChannelPermission(channelId, userId, role, customPermissions)`
    2. 변경 후: `ChannelPermission(channelId, userId, projectId, roleIds, overridePermissions)`
       - `projectId`: 프로젝트 ID 추가 (채널이 속한 프로젝트)
       - `roleIds`: 해당 멤버가 가진 역할 ID 목록 (멤버의 역할 참조)
       - `overridePermissions`: 채널별 권한 재정의 (선택적, 대부분 비어있음)
    
  - **권한 확인 로직 수정**:
    1. `hasPermission()` 메소드 로직 변경:
       ```kotlin
       fun hasPermission(permission: RolePermission, roleRepository: ProjectRoleRepository): Boolean {
           // 1. 먼저 채널별 재정의된 권한 확인
           overridePermissions?.get(permission)?.let { return it }
           
           // 2. 없으면 사용자의 역할들에서 권한 확인
           // 사용자가 가진 역할 중 하나라도 권한이 있으면 true
           return roleIds.any { roleId ->
               roleRepository.getRole(projectId, roleId)?.permissions?.get(permission) == true
           }
       }
       ```

  - **기존 `ChannelRole` Enum 처리**:
    1. `ChannelRole` Enum 장기적으로 제거 계획 수립
    2. 마이그레이션 기간 동안은 이전 코드 호환을 위해 유지하고 경고 주석 추가

- [x] **Step 3: ChannelRepository 및 구현체 수정 계획**
  - `getChannelPermission` 등의 메소드가 프로젝트 역할 기반으로 권한을 조회하도록 수정 방안 정의
  - Firestore 데이터 구조 변경 필요 사항 식별

  **수정 계획:**
  - **ChannelRepository 인터페이스 수정**:
    1. 기존 메소드 시그니처 유지하되 내부 구현 변경
    2. 신규 메소드 추가:
       ```kotlin
       // 프로젝트 역할 기반의 채널 권한 조회
       suspend fun getChannelPermissionByProjectRole(
           channelId: String, 
           userId: String, 
           projectId: String
       ): Result<ChannelPermission>
       
       // 채널별 권한 재정의 (overridePermissions) 설정
       suspend fun setChannelPermissionOverride(
           channelId: String,
           userId: String, 
           permission: RolePermission, 
           value: Boolean
       ): Result<Unit>
       
       // 채널별 권한 재정의 제거
       suspend fun removeChannelPermissionOverride(
           channelId: String,
           userId: String,
           permission: RolePermission
       ): Result<Unit>
       ```

  - **ChannelRepositoryImpl 구현체 수정**:
    1. `getChannelPermission` 메소드 수정:
       ```kotlin
       override suspend fun getChannelPermission(channelId: String, userId: String): Result<ChannelPermission> {
           return try {
               // 채널 정보 가져오기
               val channelSnapshot = firestore.collection(Collections.CHANNELS)
                   .document(channelId)
                   .get()
                   .await()
               
               // 채널에서 projectId 추출 (채널이 속한 프로젝트)
               val projectData = channelSnapshot.get(ChannelFields.PROJECT_SPECIFIC_DATA) as? Map<String, Any>
               val projectId = projectData?.get(ChannelProjectDataFields.PROJECT_ID) as? String
               
               if (projectId != null) {
                   // 프로젝트 역할 기반 권한 조회
                   return getChannelPermissionByProjectRole(channelId, userId, projectId)
               } else {
                   // DM 채널 등 프로젝트 외부 채널의 권한은 기존 방식 사용
                   // 기본 권한 반환 (DM은 항상 읽기/쓰기 가능)
                   val defaultPermission = createDefaultDmPermission(channelId, userId)
                   Result.success(defaultPermission)
               }
           } catch (e: Exception) {
               Result.failure(e)
           }
       }
       ```

    2. `getChannelPermissionByProjectRole` 메소드 구현:
       ```kotlin
       override suspend fun getChannelPermissionByProjectRole(
           channelId: String, 
           userId: String, 
           projectId: String
       ): Result<ChannelPermission> {
           return try {
               // 1. 채널별 권한 재정의 가져오기 (channels/{channelId}/permission_overrides/{userId})
               val overridesSnapshot = firestore.collection(Collections.CHANNELS)
                   .document(channelId)
                   .collection("permission_overrides")
                   .document(userId)
                   .get()
                   
               val overridePermissions = if (overridesSnapshot.exists()) {
                   (overridesSnapshot.get("permissions") as? Map<*, *>)?.mapNotNull { (k, v) ->
                       try {
                           val permission = RolePermission.valueOf(k.toString())
                           val value = v as? Boolean ?: return@mapNotNull null
                           permission to value
                       } catch (e: Exception) {
                           null
                       }
                   }?.toMap()
               } else {
                   null
               }
               
               // 2. 사용자의 프로젝트 역할 IDs 가져오기 (projects/{projectId}/members/{userId})
               val memberSnapshot = firestore.collection("projects")
                   .document(projectId)
                   .collection("members")
                   .document(userId)
                   .get()
                   
               val roleIds = if (memberSnapshot.exists()) {
                   (memberSnapshot.get("roleIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
               } else {
                   emptyList()
               }
               
               // 3. 새 ChannelPermission 객체 생성
               val permission = ChannelPermission(
                   channelId = channelId,
                   userId = userId,
                   projectId = projectId,
                   roleIds = roleIds,
                   overridePermissions = overridePermissions
               )
               
               Result.success(permission)
           } catch (e: Exception) {
               Result.failure(e)
           }
       }
       ```

  - **Firestore 데이터 구조 변경**:
    1. 기존: `channels/{channelId}/permissions/{userId}` - `role`, `permissions` 필드
    2. 변경: `channels/{channelId}/permission_overrides/{userId}` - `permissions` 맵 (특정 권한만 재정의)
    3. 추가: 채널 문서에 `projectId` 필드 명시적 추가 (이미 있을 수 있음)

- [x] **Step 4: ProjectRole 모델 및 Repository 구현/수정**
  - 프로젝트 역할 모델에 채널 권한 관련 필드 추가
  - 역할 조회 및 관리 기능 구현/수정

  **수정 계획:**
  - **RolePermission Enum 수정 (완료):**
    1. 기존 RolePermission Enum에 채널 관련 권한 추가:
       ```kotlin
       package com.example.domain.model

       enum class RolePermission(val description: String) {
           // 기존 권한들...
           INVITE_MEMBERS("멤버 초대 권한"),
           KICK_MEMBERS("멤버 추방 권한"),
           MANAGE_ROLES("역할 생성/편집/삭제 권한"),
           ASSIGN_ROLES("멤버에게 역할 할당 권한"),
           MANAGE_CHANNELS("채널 생성/편집/삭제 권한"),
           DELETE_OTHERS_MESSAGES("다른 멤버 메시지 삭제 권한"),
           PIN_MESSAGES("메시지 고정 권한"),
           EDIT_PROJECT_INFO("프로젝트 이름/정보 변경 권한"),
           MANAGE_PROJECT_SETTINGS("프로젝트 전반 설정 변경 권한"),
           CREATE_SCHEDULE("일정 생성/편집 권한"),
           MENTION_EVERYONE("@everyone 언급 권한"),
           
           // 채널 관련 권한 추가 (ChannelPermissionType에서 이전)
           READ_MESSAGES("채널 메시지 읽기 권한"),
           SEND_MESSAGES("채널 메시지 전송 권한"),
           DELETE_MESSAGES("자신의 메시지 삭제 권한"),
           UPLOAD_FILES("파일 업로드 권한"),
           MENTION_MEMBERS("멤버 언급(@) 권한"),
           MANAGE_MESSAGE_THREADS("메시지 스레드 관리 권한")
       }
       ```

  - **Role 모델 및 관련 엔티티 확인**:
    1. 기존 `Role` 클래스는 이미 `permissions: Map<RolePermission, Boolean>` 필드를 가지고 있어 수정이 필요 없음
    2. 채널 권한이 추가되면 UI에서 설정 가능하도록 `RoleEntity` 및 `RolePermissionEntity`만 확인 필요

  - **ProjectRoleRepository 확장**:
    1. 채널 권한 관련 메소드 추가:
       ```kotlin
       // 특정 채널에 대한 역할 권한 조회 헬퍼 메소드
       suspend fun getChannelPermissionsForRole(
           roleId: String, 
           channelId: String
       ): Result<Map<RolePermission, Boolean>>
       
       // 모든 채널 권한을 갖는 "관리자" 역할 생성 유틸리티 메소드
       suspend fun createAdminRole(projectId: String): Result<Role>
       
       // 일반 멤버 기본 역할 생성 유틸리티 메소드 (읽기, 쓰기만 가능)
       suspend fun createDefaultMemberRole(projectId: String): Result<Role>
       ```

  - **UI에 권한 설정 기능 구현 고려**:
    1. 채널 권한을 프로젝트 역할 설정 UI에 추가
    2. "채널 관리" 섹션에 세부 권한들 그룹화:
       - 채널 메시지 읽기
       - 채널 메시지 전송
       - 파일 업로드
       - 멤버 언급
       - 메시지 삭제/관리
       - 등...

- [x] **Step 5: ChannelPermission 클래스 수정 구현**
  - Step 2에서 정의한 계획에 따라 `ChannelPermission` 클래스 수정
  - 권한 확인 로직(`hasPermission`) 수정

  **구현 계획:**
  - **1. 새 ChannelPermission 클래스 정의:**
    ```kotlin
    /**
     * 채널 권한을 나타내는 데이터 클래스입니다.
     * 프로젝트 역할 기반으로 권한을 확인합니다.
     */
    data class ChannelPermission(
        /**
         * 대상 채널 ID입니다.
         */
        val channelId: String,
        
        /**
         * 사용자 ID입니다.
         */
        val userId: String,
        
        /**
         * 채널이 속한 프로젝트 ID입니다.
         * DM 채널 등 프로젝트에 속하지 않은 채널은 null 가능합니다.
         */
        val projectId: String?,
        
        /**
         * 사용자가 해당 프로젝트에서 가진 역할 ID 목록입니다.
         * 프로젝트에 속하지 않은 채널은 빈 리스트일 수 있습니다.
         */
        val roleIds: List<String>,
        
        /**
         * 채널별 권한 재정의입니다. null인 경우 역할의 기본 권한이 적용됩니다.
         * 특정 권한에 대해 역할과 관계없이 허용/거부를 설정할 수 있습니다.
         */
        val overridePermissions: Map<RolePermission, Boolean>? = null,
        
        /**
         * 레거시 호환을 위한 채널 역할입니다. 새 구현에서는 사용하지 않습니다.
         * @deprecated 역할 기반 권한으로 대체되었습니다. `roleIds`를 사용하세요.
         */
        @Deprecated("레거시 호환용. 새 코드에서 사용 금지")
        val role: ChannelRole? = null,
        
        /**
         * 레거시 호환용 커스텀 권한입니다. 새 구현에서는 사용하지 않습니다.
         * @deprecated `overridePermissions`로 대체되었습니다.
         */
        @Deprecated("레거시 호환용. 새 코드에서 사용 금지")
        val customPermissions: Map<ChannelPermissionType, Boolean>? = null
    ) {
        /**
         * 주어진 권한 유형에 대한 접근 가능 여부를 확인합니다.
         * 1. 채널별 권한 재정의가 있으면 해당 값 사용
         * 2. 없으면 사용자의 역할 기반 권한 확인
         * 3. 프로젝트 속성이 없는 채널(예: DM)은 기본 권한 정책 적용
         */
        suspend fun hasPermission(
            permission: RolePermission,
            roleRepository: ProjectRoleRepository
        ): Boolean {
            // 1. 채널별 권한 재정의 확인
            overridePermissions?.get(permission)?.let { return it }
            
            // 2. 프로젝트가 없는 채널(DM 등)은 기본 권한 정책 적용
            if (projectId == null) {
                return when (permission) {
                    // DM 채널은 기본적으로 읽기/쓰기 허용
                    RolePermission.READ_MESSAGES, 
                    RolePermission.SEND_MESSAGES,
                    RolePermission.UPLOAD_FILES, 
                    RolePermission.MENTION_MEMBERS -> true
                    // 다른 관리 권한은 기본적으로 거부
                    else -> false
                }
            }
            
            // 3. 역할 없으면 기본 거부
            if (roleIds.isEmpty()) return false
            
            // 4. 각 역할 권한 확인 - 하나라도 허용하면 true
            return roleIds.any { roleId ->
                val role = roleRepository.getRole(projectId, roleId).getOrNull()
                role?.permissions?.get(permission) == true
            }
        }
        
        /**
         * 레거시 호환을 위한 메소드입니다.
         * @deprecated 새 구현의 `hasPermission(RolePermission, ProjectRoleRepository)`를 사용하세요.
         */
        @Deprecated("레거시 호환용. 새 메소드로 마이그레이션하세요.")
        fun hasPermission(permission: ChannelPermissionType): Boolean {
            // 레거시 메소드는 이전 방식대로 동작
            // 커스텀 권한이 설정되어 있으면 그 값을 사용
            customPermissions?.get(permission)?.let { return it }
            
            // 그렇지 않으면 역할 기반 권한 확인
            return when (role) {
                ChannelRole.OWNER -> true
                ChannelRole.ADMIN -> permission != ChannelPermissionType.MANAGE_PERMISSIONS
                ChannelRole.MODERATOR -> when (permission) {
                    ChannelPermissionType.READ_MESSAGES,
                    ChannelPermissionType.SEND_MESSAGES,
                    ChannelPermissionType.MANAGE_MESSAGES,
                    ChannelPermissionType.MENTION_MEMBERS,
                    ChannelPermissionType.UPLOAD_FILES,
                    ChannelPermissionType.INVITE_MEMBERS -> true
                    ChannelPermissionType.MANAGE_CHANNEL,
                    ChannelPermissionType.MANAGE_PERMISSIONS -> false
                }
                ChannelRole.MEMBER -> when (permission) {
                    ChannelPermissionType.READ_MESSAGES,
                    ChannelPermissionType.SEND_MESSAGES,
                    ChannelPermissionType.MENTION_MEMBERS,
                    ChannelPermissionType.UPLOAD_FILES -> true
                    ChannelPermissionType.MANAGE_MESSAGES,
                    ChannelPermissionType.INVITE_MEMBERS,
                    ChannelPermissionType.MANAGE_CHANNEL,
                    ChannelPermissionType.MANAGE_PERMISSIONS -> false
                }
                ChannelRole.GUEST -> permission == ChannelPermissionType.READ_MESSAGES
                null -> false
            }
        }

        /**
         * ChannelPermission 객체를 Firestore에 저장하기 위한 Map으로 변환합니다.
         * 새로운 permissions_override 컬렉션용 변환 메소드입니다.
         */
        fun toFirestoreOverrideMap(): Map<String, Any> {
            val map = mutableMapOf<String, Any>()
            
            if (overridePermissions != null && overridePermissions.isNotEmpty()) {
                val permMap = mutableMapOf<String, Boolean>()
                overridePermissions.forEach { (type, allowed) -> 
                    permMap[type.name] = allowed 
                }
                map["permissions"] = permMap
            }
            
            map["userId"] = userId
            map["updatedAt"] = Timestamp.now()
            
            return map
        }
        
        /**
         * 레거시 호환용 Firestore 변환 메소드입니다.
         * @deprecated 새 구현에서는 `toFirestoreOverrideMap()`을 사용하세요.
         */
        @Deprecated("레거시 호환용. 새 메소드로 마이그레이션하세요.")
        fun toFirestoreMap(): Map<String, Any> {
            val map = mutableMapOf<String, Any>()
            map["channelId"] = channelId
            map["userId"] = userId
            role?.let { map["role"] = it.name }
            
            customPermissions?.let {
                val permMap = mutableMapOf<String, Boolean>()
                it.forEach { (type, allowed) -> permMap[type.name] = allowed }
                map["customPermissions"] = permMap
            }
            
            return map
        }

        /**
         * 프로젝트 역할 기반 채널 권한 생성 메소드
         */
        companion object {
            fun createDefaultDmPermission(channelId: String, userId: String): ChannelPermission {
                return ChannelPermission(
                    channelId = channelId,
                    userId = userId,
                    projectId = null, // DM은 프로젝트에 속하지 않음
                    roleIds = emptyList(), // 역할 없음
                    // DM 기본 권한
                    overridePermissions = mapOf(
                        RolePermission.READ_MESSAGES to true,
                        RolePermission.SEND_MESSAGES to true,
                        RolePermission.UPLOAD_FILES to true,
                        RolePermission.MENTION_MEMBERS to true
                    )
                )
            }

            fun createDefaultProjectMemberPermission(
                channelId: String, 
                userId: String,
                projectId: String,
                roleIds: List<String>
            ): ChannelPermission {
                return ChannelPermission(
                    channelId = channelId,
                    userId = userId,
                    projectId = projectId,
                    roleIds = roleIds,
                    overridePermissions = null // 역할에서 권한 상속
                )
            }
        }
    }
    ```
    
  - **2. 이전 ChannelRole 및 ChannelPermissionType Enum에 Deprecated 추가:**
    ```kotlin
    /**
     * 채널 내 역할을 나타내는 열거형입니다.
     * @deprecated 프로젝트 역할 기반 권한으로 대체되었습니다. RolePermission을 사용하세요.
     */
    @Deprecated("프로젝트 역할 기반 권한으로 대체되었습니다.")
    enum class ChannelRole {
        // ... 기존 코드 ...
    }

    /**
     * 채널 권한 유형을 나타내는 열거형입니다.
     * @deprecated RolePermission enum으로 통합되었습니다.
     */
    @Deprecated("RolePermission으로 통합되었습니다.")
    enum class ChannelPermissionType {
        // ... 기존 코드 ...
    }
    ```

  - **3. 필요한 확장 함수 추가:**
    ```kotlin
    /**
     * ChannelPermissionType을 RolePermission으로 변환하는 확장 함수입니다.
     * 마이그레이션에 사용됩니다.
     */
    @Deprecated("마이그레이션 목적으로만 사용하세요")
    fun ChannelPermissionType.toRolePermission(): RolePermission {
        return when (this) {
            ChannelPermissionType.READ_MESSAGES -> RolePermission.READ_MESSAGES
            ChannelPermissionType.SEND_MESSAGES -> RolePermission.SEND_MESSAGES
            ChannelPermissionType.MANAGE_MESSAGES -> RolePermission.DELETE_MESSAGES
            ChannelPermissionType.MENTION_MEMBERS -> RolePermission.MENTION_MEMBERS
            ChannelPermissionType.UPLOAD_FILES -> RolePermission.UPLOAD_FILES
            ChannelPermissionType.INVITE_MEMBERS -> RolePermission.INVITE_MEMBERS
            ChannelPermissionType.MANAGE_CHANNEL -> RolePermission.MANAGE_CHANNELS
            ChannelPermissionType.MANAGE_PERMISSIONS -> RolePermission.MANAGE_ROLES
        }
    }
    ```

- [x] **Step 6: ChannelRepository 구현체 수정**
  - Step 3에서 정의한 계획에 따라 `ChannelRepositoryImpl` 수정
  - Firestore 데이터 조회/저장 로직 업데이트

  **구현 계획:**
  - **1. Firestore 경로 상수 정의:**
    ```kotlin
    // core_common/src/main/java/com/example/core_common/constants/FirestoreConstants.kt
    object Collections {
        // ... (기존 상수들)
        const val PERMISSION_OVERRIDES = "permission_overrides" 
    }

    object ChannelPermissionOverrideFields {
        const val PERMISSIONS = "permissions" // Map<String, Boolean>
        const val USER_ID = "userId" // String
        const val UPDATED_AT = "updatedAt" // Timestamp
    }
    ```

  - **2. `ChannelRepositoryImpl` 메소드 수정 및 구현:**
    - `getChannelPermission(channelId, userId)`: Step 3의 계획대로 수정
    - `getChannelPermissionByProjectRole(channelId, userId, projectId)`: Step 3의 계획대로 구현
    - `setChannelPermissionOverride(channelId, userId, permission, value)`:
      ```kotlin
      override suspend fun setChannelPermissionOverride(
          channelId: String,
          userId: String,
          permission: RolePermission,
          value: Boolean
      ): Result<Unit> {
          return try {
              val overrideRef = firestore.collection(Collections.CHANNELS)
                  .document(channelId)
                  .collection(Collections.PERMISSION_OVERRIDES)
                  .document(userId)

              firestore.runTransaction { transaction ->
                  val snapshot = transaction.get(overrideRef)
                  val existingPermissions = 
                      (snapshot.get(ChannelPermissionOverrideFields.PERMISSIONS) as? Map<*, *>)
                          ?.mapNotNull { (k, v) -> 
                              try { RolePermission.valueOf(k.toString()) to (v as? Boolean ?: false) } 
                              catch (e: Exception) { null } 
                          }?.toMap()?.toMutableMap() ?: mutableMapOf()

                  existingPermissions[permission] = value
                  
                  val dataToSet = mapOf(
                      ChannelPermissionOverrideFields.USER_ID to userId,
                      ChannelPermissionOverrideFields.PERMISSIONS to existingPermissions,
                      ChannelPermissionOverrideFields.UPDATED_AT to FieldValue.serverTimestamp()
                  )
                  transaction.set(overrideRef, dataToSet, SetOptions.merge())
                  null // Transaction 성공 시 반환값
              }.await()
              Result.success(Unit)
          } catch (e: Exception) {
              Result.failure(e)
          }
      }
      ```
    - `removeChannelPermissionOverride(channelId, userId, permission)`:
      ```kotlin
      override suspend fun removeChannelPermissionOverride(
          channelId: String,
          userId: String,
          permission: RolePermission
      ): Result<Unit> {
          return try {
              val overrideRef = firestore.collection(Collections.CHANNELS)
                  .document(channelId)
                  .collection(Collections.PERMISSION_OVERRIDES)
                  .document(userId)

              firestore.runTransaction { transaction ->
                  val snapshot = transaction.get(overrideRef)
                  if (snapshot.exists()) {
                      val existingPermissions =
                          (snapshot.get(ChannelPermissionOverrideFields.PERMISSIONS) as? Map<*, *>)
                              ?.mapNotNull { (k, v) ->
                                  try { RolePermission.valueOf(k.toString()) to (v as? Boolean ?: false) }
                                  catch (e: Exception) { null }
                              }?.toMap()?.toMutableMap() ?: mutableMapOf()

                      if (existingPermissions.containsKey(permission)) {
                          existingPermissions.remove(permission)
                          
                          val dataToUpdate = mapOf(
                              ChannelPermissionOverrideFields.PERMISSIONS to existingPermissions,
                              ChannelPermissionOverrideFields.UPDATED_AT to FieldValue.serverTimestamp()
                          )
                          transaction.update(overrideRef, dataToUpdate)
                      }
                  }
                  null // Transaction 성공 시 반환값
              }.await()
              Result.success(Unit)
          } catch (e: Exception) {
              Result.failure(e)
          }
      }
      ```
    - `createDefaultDmPermission` 헬퍼 메소드 (ChannelPermission.kt에서 ChannelRepositoryImpl 내부로 이동하거나, ChannelPermission의 companion object 유지):
      ```kotlin
      private fun createDefaultDmPermission(channelId: String, userId: String): ChannelPermission {
          return ChannelPermission(
              channelId = channelId,
              userId = userId,
              projectId = null, // DM은 프로젝트에 속하지 않음
              roleIds = emptyList(), // 역할 없음
              overridePermissions = mapOf( // DM 기본 권한
                  RolePermission.READ_MESSAGES to true,
                  RolePermission.SEND_MESSAGES to true,
                  RolePermission.UPLOAD_FILES to true,
                  RolePermission.MENTION_MEMBERS to true
              )
          )
      }
      ```

  - **3. Firestore에서 기존 `channels/{channelId}/permissions` 컬렉션 제거 준비**:
    - 마이그레이션 스크립트 작성 계획 (Step 9에서 구체화)
    - 데이터 마이그레이션 전까지는 해당 컬렉션 접근 코드 유지 (읽기 전용으로)

  - **4. Firestore 규칙 업데이트 필요**:
    - `channels/{channelId}/permission_overrides/{userId}` 경로에 대한 보안 규칙 추가/수정
    - 채널 소유자 또는 특정 역할을 가진 사용자만 `permission_overrides`를 수정할 수 있도록 제한

- [x] **Step 7: 영향 받는 기능 모듈 확인 및 수정**
  - `feature_chat`, `feature_project` 등 영향 받는 모듈 확인
  - 필요한 UI 및 ViewModel 수정

  **수정 계획:**
  - **1. 권한 확인 로직 변경:**
    - 기존 `ChannelPermission.hasPermission(ChannelPermissionType)` 호출 부분을 `ChannelPermission.hasPermission(RolePermission, ProjectRoleRepository)`로 변경
    - 필요시 `ProjectRoleRepository`를 ViewModel/UseCase에 주입
  - **2. 영향 받는 모듈:**
    - `feature_chat`:
      - 메시지 전송, 파일 업로드, 멤버 언급 등 권한 체크 로직 수정
      - `ChatViewModel` 또는 관련 UseCase 수정
    - `feature_project`:
      - 역할 설정 UI (`EditRoleScreen`, `EditRoleViewModel`): RolePermission Enum 업데이트 반영하여 채널 권한 설정 UI 추가
      - 멤버 역할 할당 UI (`EditMemberScreen`, `EditMemberViewModel`): 변경 없음 (ProjectMemberRepository 사용)
      - 채널 생성/관리 UI (아직 미구현 시, 향후 `MANAGE_CHANNELS` 권한 체크)
    - `feature_main` (및 기타 관련 모듈):
      - 채널 목록 표시 시 권한에 따른 필터링 (예: `READ_MESSAGES` 권한 없는 채널 숨기기 등) 확인/수정
  - **3. UI 수정:**
    - 역할 편집 화면 (`EditRoleScreen`): 채널 권한 그룹 추가 및 체크박스/스위치 제공
    - 채팅 화면 (`ChatScreen`): 메시지 입력 필드, 파일 업로드 버튼 등의 활성화/비활성화 여부를 새 권한 확인 로직에 따라 결정
  - **4. ViewModel 수정:**
    - `EditRoleViewModel`: `RolePermission` 업데이트 반영, 권한 저장 로직 확인
    - `ChatViewModel`: `hasPermission` 호출 방식 변경, `ProjectRoleRepository` 주입 (또는 관련 UseCase 사용)
    - `HomeViewModel` 등 채널 목록 사용하는 ViewModel: 권한 기반 채널 필터링 로직 확인/수정
  - **5. UseCase 수정:**
    - `GetChannelPermissionUseCase` (가칭, 만약 존재한다면): 내부 로직을 `ChannelRepository`의 새 메소드 호출로 변경
    - 권한 체크를 수행하는 다른 UseCase들도 필요시 수정

- [x] **Step 8: 테스트 계획 및 구현**
  - 수정된 권한 시스템에 대한 단위 테스트 작성
  - 통합 테스트 작성

  **구현 계획:**
  - **1. 단위 테스트(Unit Tests):**
    - `RolePermission` Enum: 각 권한 값 및 설명 문자열 정확성 테스트
    - `ChannelPermission` 클래스:
      - `hasPermission()` 메소드:
        - 채널별 재정의(`overridePermissions`)가 있을 때 정확히 반환하는지 테스트
        - 프로젝트 역할(`roleIds`)에 따른 권한 상속이 올바르게 작동하는지 테스트 (Mock `ProjectRoleRepository` 사용)
        - DM 채널 등 `projectId`가 null일 때 기본 권한 정책이 적용되는지 테스트
        - 역할이 없는 사용자의 경우 기본적으로 권한이 없는지 테스트
      - 레거시 `hasPermission(ChannelPermissionType)`: `@Deprecated` 확인 및 기존 로직 유지 테스트 (마이그레이션 기간 동안)
      - `toFirestoreOverrideMap()` / `toFirestoreMap()`: Firestore 저장용 Map 변환 로직 테스트
    - `Role` 클래스: `permissions` 맵이 잘 관리되는지 테스트
    - `ChannelRepositoryImpl`:
      - `getChannelPermission()`: 프로젝트 채널과 DM 채널에 대해 각각 올바른 `ChannelPermission` 객체를 반환하는지 테스트 (Mock Firestore 사용)
      - `getChannelPermissionByProjectRole()`: Firestore에서 `permission_overrides`와 `member roleIds`를 조합하여 `ChannelPermission` 객체를 생성하는 로직 테스트
      - `setChannelPermissionOverride()` / `removeChannelPermissionOverride()`: Firestore 트랜잭션 및 데이터 업데이트 로직 테스트
    - `ProjectRoleRepositoryImpl` (확장된 경우):
      - `getChannelPermissionsForRole()`: 특정 역할에 대한 채널 권한을 올바르게 반환하는지 테스트
      - `createAdminRole()` / `createDefaultMemberRole()`: 유틸리티 메소드가 의도한 권한을 가진 역할을 생성하는지 테스트
    - 관련 ViewModels (`ChatViewModel`, `EditRoleViewModel` 등):
      - 권한 상태(`UiState`)가 올바르게 업데이트되는지 테스트
      - 권한에 따라 이벤트가 올바르게 발생하는지 테스트 (Mock UseCases/Repositories 사용)

  - **2. 통합 테스트(Integration Tests):**
    - 실제 Firestore (에뮬레이터 또는 테스트 프로젝트)를 사용하여 전체 권한 흐름 테스트
    - **시나리오 기반 테스트:**
      - **역할 생성 및 권한 할당:** 새 역할 생성 -> 채널 관련 권한(읽기, 쓰기 등) 할당 -> Firestore에 저장 확인
      - **멤버에게 역할 부여:** 사용자에게 특정 역할 부여 -> Firestore에 멤버 `roleIds` 업데이트 확인
      - **채널 권한 확인 (프로젝트 채널):**
        - 특정 역할을 가진 사용자가 프로젝트 채널에 접근 시, 해당 역할에 정의된 대로 메시지 읽기/쓰기/파일 업로드 등이 가능한지/불가능한지 확인
        - 채널 관리자가 `MANAGE_CHANNELS` 권한으로 채널 설정을 변경할 수 있는지 확인
      - **채널 권한 확인 (DM 채널):**
        - DM 채널 참여자는 기본적으로 메시지 읽기/쓰기가 가능한지 확인
        - DM 채널에서는 프로젝트 역할이 적용되지 않는지 확인
      - **채널 권한 재정의:**
        - 특정 사용자에게 특정 채널에 대해 역할 권한과 다른 재정의 권한(예: 읽기만 가능) 설정 -> 재정의된 권한이 우선 적용되는지 확인
        - 재정의 권한 제거 시 다시 역할 기반 권한으로 돌아가는지 확인
      - **소유자/관리자 권한:** 프로젝트 소유자/관리자는 대부분의 권한을 갖는지 확인

  - **3. UI 테스트 (Espresso - 선택 사항):**
    - `EditRoleScreen`: 채널 권한 설정 UI가 올바르게 표시되고, 권한 변경 시 ViewModel과 상호작용하는지 테스트
    - `ChatScreen`: 권한에 따라 메시지 입력창, 버튼 등의 UI 요소가 활성화/비활성화되는지 테스트

- [x] **Step 9: 데이터 마이그레이션 계획 (필요 시)**
  - 수정된 권한 시스템에 대한 단위 테스트 작성
  - 통합 테스트 작성

  **구현 계획:**
  - **1. 단위 테스트(Unit Tests):**
    - `RolePermission` Enum: 각 권한 값 및 설명 문자열 정확성 테스트
    - `ChannelPermission` 클래스:
      - `hasPermission()` 메소드:
        - 채널별 재정의(`overridePermissions`)가 있을 때 정확히 반환하는지 테스트
        - 프로젝트 역할(`roleIds`)에 따른 권한 상속이 올바르게 작동하는지 테스트 (Mock `ProjectRoleRepository` 사용)
        - DM 채널 등 `projectId`가 null일 때 기본 권한 정책이 적용되는지 테스트
        - 역할이 없는 사용자의 경우 기본적으로 권한이 없는지 테스트
      - 레거시 `hasPermission(ChannelPermissionType)`: `@Deprecated` 확인 및 기존 로직 유지 테스트 (마이그레이션 기간 동안)
      - `toFirestoreOverrideMap()` / `toFirestoreMap()`: Firestore 저장용 Map 변환 로직 테스트
    - `Role` 클래스: `permissions` 맵이 잘 관리되는지 테스트
    - `ChannelRepositoryImpl`:
      - `getChannelPermission()`: 프로젝트 채널과 DM 채널에 대해 각각 올바른 `ChannelPermission` 객체를 반환하는지 테스트 (Mock Firestore 사용)
      - `getChannelPermissionByProjectRole()`: Firestore에서 `permission_overrides`와 `member roleIds`를 조합하여 `ChannelPermission` 객체를 생성하는 로직 테스트
      - `setChannelPermissionOverride()` / `removeChannelPermissionOverride()`: Firestore 트랜잭션 및 데이터 업데이트 로직 테스트
    - `ProjectRoleRepositoryImpl` (확장된 경우):
      - `getChannelPermissionsForRole()`: 특정 역할에 대한 채널 권한을 올바르게 반환하는지 테스트
      - `createAdminRole()` / `createDefaultMemberRole()`: 유틸리티 메소드가 의도한 권한을 가진 역할을 생성하는지 테스트
    - 관련 ViewModels (`ChatViewModel`, `EditRoleViewModel` 등):
      - 권한 상태(`UiState`)가 올바르게 업데이트되는지 테스트
      - 권한에 따라 이벤트가 올바르게 발생하는지 테스트 (Mock UseCases/Repositories 사용)

  - **2. 통합 테스트(Integration Tests):**
    - 실제 Firestore (에뮬레이터 또는 테스트 프로젝트)를 사용하여 전체 권한 흐름 테스트
    - **시나리오 기반 테스트:**
      - **역할 생성 및 권한 할당:** 새 역할 생성 -> 채널 관련 권한(읽기, 쓰기 등) 할당 -> Firestore에 저장 확인
      - **멤버에게 역할 부여:** 사용자에게 특정 역할 부여 -> Firestore에 멤버 `roleIds` 업데이트 확인
      - **채널 권한 확인 (프로젝트 채널):**
        - 특정 역할을 가진 사용자가 프로젝트 채널에 접근 시, 해당 역할에 정의된 대로 메시지 읽기/쓰기/파일 업로드 등이 가능한지/불가능한지 확인
        - 채널 관리자가 `MANAGE_CHANNELS` 권한으로 채널 설정을 변경할 수 있는지 확인
      - **채널 권한 확인 (DM 채널):**
        - DM 채널 참여자는 기본적으로 메시지 읽기/쓰기가 가능한지 확인
        - DM 채널에서는 프로젝트 역할이 적용되지 않는지 확인
      - **채널 권한 재정의:**
        - 특정 사용자에게 특정 채널에 대해 역할 권한과 다른 재정의 권한(예: 읽기만 가능) 설정 -> 재정의된 권한이 우선 적용되는지 확인
        - 재정의 권한 제거 시 다시 역할 기반 권한으로 돌아가는지 확인
      - **소유자/관리자 권한:** 프로젝트 소유자/관리자는 대부분의 권한을 갖는지 확인

  - **3. UI 테스트 (Espresso - 선택 사항):**
    - `EditRoleScreen`: 채널 권한 설정 UI가 올바르게 표시되고, 권한 변경 시 ViewModel과 상호작용하는지 테스트
    - `ChatScreen`: 권한에 따라 메시지 입력창, 버튼 등의 UI 요소가 활성화/비활성화되는지 테스트

  **결론: 기존 데이터가 없으므로 데이터 마이그레이션 작업은 필요하지 않습니다.** 