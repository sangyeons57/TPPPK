package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.PermissionDTO
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 역할에 할당된 권한 정보에 접근하기 위한 인터페이스입니다.
 * DefaultDatasource를 확장하여 특정 프로젝트의 특정 역할에 할당된 권한에 대한
 * CRUD(Create, Read, Update, Delete) 및 관찰 기능을 제공합니다.
 *
 * 권한 데이터는 `projects/{projectId}/roles/{roleId}/permissions/{permissionId}` 경로에 저장되며,
 * `permissionId`는 일반적으로 권한의 이름(예: "CAN_EDIT_TASK")으로 사용됩니다.
 *
 * **중요:** 모든 데이터 접근 메소드 호출 전에 반드시 `setCollection(projectId, roleId)`를 호출하여
 * 대상 프로젝트 ID와 역할 ID 컨텍스트를 설정해야 합니다.
 * - `ids[0]` (첫 번째 인자): `projectId` (String)
 * - `ids[1]` (두 번째 인자): `roleId` (String)
 */
interface PermissionRemoteDataSource : DefaultDatasource<PermissionDTO>

@Singleton
class PermissionRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<PermissionDTO>(firestore, PermissionDTO::class.java), PermissionRemoteDataSource {

}
