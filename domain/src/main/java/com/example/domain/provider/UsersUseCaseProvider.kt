package com.example.domain.provider

import com.example.domain.repository.local.LocalUserRepository
import com.example.domain.usecase.local.users.CheckNicknameAvailabilityLocalUseCase
import com.example.domain.usecase.local.users.CheckNicknameAvailabilityLocalUseCaseImpl
import com.example.domain.usecase.local.users.DeleteUserUseCase
import com.example.domain.usecase.local.users.DeleteUserUseCaseImpl
import com.example.domain.usecase.local.users.GetAllUsersUseCase
import com.example.domain.usecase.local.users.GetAllUsersUseCaseImpl
import com.example.domain.usecase.local.users.GetCurrentUserStreamLocalUseCase
import com.example.domain.usecase.local.users.GetCurrentUserStreamLocalUseCaseImpl
import com.example.domain.usecase.local.users.GetUserByIdLocalUseCase
import com.example.domain.usecase.local.users.GetUserByIdLocalUseCaseImpl
import com.example.domain.usecase.local.users.GetUserStreamLocalUseCase
import com.example.domain.usecase.local.users.GetUserStreamLocalUseCaseImpl
import com.example.domain.usecase.local.users.GetUserUseCase
import com.example.domain.usecase.local.users.GetUserUseCaseImpl
import com.example.domain.usecase.local.users.InsertUserUseCase
import com.example.domain.usecase.local.users.InsertUserUseCaseImpl
import com.example.domain.usecase.local.users.ObserveUserUpdatedAtLocalUseCase
import com.example.domain.usecase.local.users.ObserveUserUpdatedAtLocalUseCaseImpl
import com.example.domain.usecase.local.users.RemoveProfileImageLocalUseCase
import com.example.domain.usecase.local.users.RemoveProfileImageLocalUseCaseImpl
import com.example.domain.usecase.local.users.SearchUserByNameLocalUseCase
import com.example.domain.usecase.local.users.SearchUserByNameLocalUseCaseImpl
import com.example.domain.usecase.local.users.SearchUsersByNameLocalUseCase
import com.example.domain.usecase.local.users.SearchUsersByNameLocalUseCaseImpl
import com.example.domain.usecase.local.users.SuspendAccountLocalUseCase
import com.example.domain.usecase.local.users.SuspendAccountLocalUseCaseImpl
import com.example.domain.usecase.local.users.UpdateFcmTokenLocalUseCase
import com.example.domain.usecase.local.users.UpdateFcmTokenLocalUseCaseImpl
import com.example.domain.usecase.local.users.UpdateNameLocalUseCase
import com.example.domain.usecase.local.users.UpdateNameLocalUseCaseImpl
import com.example.domain.usecase.local.users.UpdateUserMemoLocalUseCase
import com.example.domain.usecase.local.users.UpdateUserMemoLocalUseCaseImpl
import com.example.domain.usecase.local.users.UpdateUserStatusLocalUseCase
import com.example.domain.usecase.local.users.UpdateUserStatusLocalUseCaseImpl
import com.example.domain.usecase.local.users.UploadProfileImageLocalUseCase
import com.example.domain.usecase.local.users.UploadProfileImageLocalUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 사용자 정보 조회, 프로필 관리, 검색 등의 기능을 담당합니다.
 */
@Singleton
class UsersUseCaseProvider @Inject constructor(
    private val localUserRepository: LocalUserRepository
) {

    /**
     * 사용자 기본 CRUD 관련 UseCase들을 생성합니다.
     * 
     * @return 사용자 기본 관리 UseCase 그룹
     */
    fun createBasicUseCases(): UsersLocalBasicUseCases {
        return UsersLocalBasicUseCases(
            // 조회
            getAllUsersUseCase = GetAllUsersUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            getUserByIdLocalUseCase = GetUserByIdLocalUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            getUserStreamLocalUseCase = GetUserStreamLocalUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            getCurrentUserStreamLocalUseCase = GetCurrentUserStreamLocalUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            getUserUseCase = GetUserUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            // 생성/수정/삭제
            insertUserUseCase = InsertUserUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            deleteUserUseCase = DeleteUserUseCaseImpl(
                localUserRepository = localUserRepository
            ),

            localUserRepository = localUserRepository
        )
    }

    /**
     * 사용자 프로필 관련 UseCase들을 생성합니다.
     * 
     * @return 사용자 프로필 관리 UseCase 그룹
     */
    fun createProfileUseCases(): UsersLocalProfileUseCases {
        return UsersLocalProfileUseCases(
            // 프로필 업데이트
            updateNameLocalUseCase = UpdateNameLocalUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            updateUserStatusLocalUseCase = UpdateUserStatusLocalUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            updateUserMemoLocalUseCase = UpdateUserMemoLocalUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            updateFcmTokenLocalUseCase = UpdateFcmTokenLocalUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            // 프로필 이미지
            uploadProfileImageLocalUseCase = UploadProfileImageLocalUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            removeProfileImageLocalUseCase = RemoveProfileImageLocalUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            // 계정 관리
            suspendAccountLocalUseCase = SuspendAccountLocalUseCaseImpl(
                localUserRepository = localUserRepository
            ),

            localUserRepository = localUserRepository
        )
    }

    /**
     * 사용자 검색 및 기타 기능 관련 UseCase들을 생성합니다.
     * 
     * @return 사용자 검색 및 기타 UseCase 그룹
     */
    fun createSearchUseCases(): UsersLocalSearchUseCases {
        return UsersLocalSearchUseCases(
            // 검색
            searchUserByNameLocalUseCase = SearchUserByNameLocalUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            searchUsersByNameLocalUseCase = SearchUsersByNameLocalUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            // 닉네임 관련
            checkNicknameAvailabilityLocalUseCase = CheckNicknameAvailabilityLocalUseCaseImpl(
                localUserRepository = localUserRepository
            ),
            
            // 관찰
            observeUserUpdatedAtLocalUseCase = ObserveUserUpdatedAtLocalUseCaseImpl(
                localUserRepository = localUserRepository
            ),

            localUserRepository = localUserRepository
        )
    }
}

/**
 * 사용자 기본 관리 Local UseCase 그룹
 */
data class UsersLocalBasicUseCases(
    // 조회
    val getAllUsersUseCase: GetAllUsersUseCase,
    val getUserByIdLocalUseCase: GetUserByIdLocalUseCase,
    val getUserStreamLocalUseCase: GetUserStreamLocalUseCase,
    val getCurrentUserStreamLocalUseCase: GetCurrentUserStreamLocalUseCase,
    val getUserUseCase: GetUserUseCase,
    
    // 생성/수정/삭제
    val insertUserUseCase: InsertUserUseCase,
    val deleteUserUseCase: DeleteUserUseCase,

    val localUserRepository: LocalUserRepository
)

/**
 * 사용자 프로필 관리 Local UseCase 그룹
 */
data class UsersLocalProfileUseCases(
    // 프로필 업데이트
    val updateNameLocalUseCase: UpdateNameLocalUseCase,
    val updateUserStatusLocalUseCase: UpdateUserStatusLocalUseCase,
    val updateUserMemoLocalUseCase: UpdateUserMemoLocalUseCase,
    val updateFcmTokenLocalUseCase: UpdateFcmTokenLocalUseCase,
    
    // 프로필 이미지
    val uploadProfileImageLocalUseCase: UploadProfileImageLocalUseCase,
    val removeProfileImageLocalUseCase: RemoveProfileImageLocalUseCase,
    
    // 계정 관리
    val suspendAccountLocalUseCase: SuspendAccountLocalUseCase,

    val localUserRepository: LocalUserRepository
)

/**
 * 사용자 검색 및 기타 Local UseCase 그룹
 */
data class UsersLocalSearchUseCases(
    // 검색
    val searchUserByNameLocalUseCase: SearchUserByNameLocalUseCase,
    val searchUsersByNameLocalUseCase: SearchUsersByNameLocalUseCase,
    
    // 닉네임 관련
    val checkNicknameAvailabilityLocalUseCase: CheckNicknameAvailabilityLocalUseCase,
    
    // 관찰
    val observeUserUpdatedAtLocalUseCase: ObserveUserUpdatedAtLocalUseCase,

    val localUserRepository: LocalUserRepository
) 