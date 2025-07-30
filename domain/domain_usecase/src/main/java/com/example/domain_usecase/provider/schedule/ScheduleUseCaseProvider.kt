package com.example.domain_usecase.provider.schedule

import com.example.domain.vo.CollectionPath
import com.example.domain_repository.RepositoryFactory
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.ScheduleRepository
import com.example.domain_repository.context.AuthRepositoryFactoryContext
import com.example.domain_repository.context.ScheduleRepositoryFactoryContext
import com.example.domain_usecase.usecase.schedule.AddScheduleUseCase
import com.example.domain_usecase.usecase.schedule.AddScheduleUseCaseImpl
import com.example.domain_usecase.usecase.schedule.DeleteScheduleUseCase
import com.example.domain_usecase.usecase.schedule.DeleteScheduleUseCaseImpl
import com.example.domain_usecase.usecase.schedule.GetScheduleDetailUseCase
import com.example.domain_usecase.usecase.schedule.GetScheduleDetailUseCaseImpl
import com.example.domain_usecase.usecase.schedule.GetScheduleSummaryForMonthUseCase
import com.example.domain_usecase.usecase.schedule.GetSchedulesForDateUseCase
import com.example.domain_usecase.usecase.schedule.GetSchedulesForDateUseCaseImpl
import com.example.domain_usecase.usecase.schedule.UpdateScheduleUseCase
import com.example.domain_usecase.usecase.schedule.UpdateScheduleUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 일정 관리 관련 UseCase들을 제공하는 Provider
 * 
 * 일정 생성, 조회, 수정, 삭제 등의 기능을 담당합니다.
 */
@Singleton
class ScheduleUseCaseProvider @Inject constructor(
    private val scheduleRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<ScheduleRepositoryFactoryContext, ScheduleRepository>,
    private val authRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {

    /**
     * 특정 사용자의 일정 관련 UseCase들을 생성합니다.
     * 
     * @param userId 사용자 ID
     * @return 일정 관련 UseCase 그룹
     */
    fun createForUser(userId: String): ScheduleUseCases {
        val scheduleRepository = scheduleRepositoryFactory.create(
            ScheduleRepositoryFactoryContext(
                collectionPath = CollectionPath.userSchedules(userId)
            )
        )

        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        return ScheduleUseCases(
            addScheduleUseCase = AddScheduleUseCaseImpl(
                scheduleRepository = scheduleRepository,
                authRepository = authRepository
            ),
            
            deleteScheduleUseCase = DeleteScheduleUseCaseImpl(
                scheduleRepository = scheduleRepository,
                authRepository = authRepository
            ),
            
            getScheduleDetailUseCase = GetScheduleDetailUseCaseImpl(
                scheduleRepository = scheduleRepository
            ),
            
            getScheduleSummaryForMonthUseCase = GetScheduleSummaryForMonthUseCase(
                scheduleRepository = scheduleRepository,
                authRepository = authRepository
            ),
            
            getSchedulesForDateUseCase = GetSchedulesForDateUseCaseImpl(
                scheduleRepository = scheduleRepository,
                authRepository = authRepository
            ),
            
            updateScheduleUseCase = UpdateScheduleUseCaseImpl(
                scheduleRepository = scheduleRepository
            ),
            
            // 공통 Repository
            authRepository = authRepository,
            scheduleRepository = scheduleRepository
        )
    }

    /**
     * 현재 로그인한 사용자의 일정 관련 UseCase들을 생성합니다.
     * 
     * @return 일정 관련 UseCase 그룹 (현재 사용자 기준)
     */
    fun createForCurrentUser(): ScheduleUseCases {
        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        // 현재 사용자 ID를 기반으로 ScheduleRepository 생성 (AuthRepository에서 가져오도록 구성)
        val scheduleRepository = scheduleRepositoryFactory.create(
            ScheduleRepositoryFactoryContext(
                collectionPath = CollectionPath.schedules // 전역 스케줄 컬렉션 사용
            )
        )

        return ScheduleUseCases(
            addScheduleUseCase = AddScheduleUseCaseImpl(
                scheduleRepository = scheduleRepository,
                authRepository = authRepository
            ),
            
            deleteScheduleUseCase = DeleteScheduleUseCaseImpl(
                scheduleRepository = scheduleRepository,
                authRepository = authRepository
            ),
            
            getScheduleDetailUseCase = GetScheduleDetailUseCaseImpl(
                scheduleRepository = scheduleRepository
            ),
            
            getScheduleSummaryForMonthUseCase = GetScheduleSummaryForMonthUseCase(
                scheduleRepository = scheduleRepository,
                authRepository = authRepository
            ),
            
            getSchedulesForDateUseCase = GetSchedulesForDateUseCaseImpl(
                scheduleRepository = scheduleRepository,
                authRepository = authRepository
            ),
            
            updateScheduleUseCase = UpdateScheduleUseCaseImpl(
                scheduleRepository = scheduleRepository
            ),
            
            // 공통 Repository
            authRepository = authRepository,
            scheduleRepository = scheduleRepository
        )
    }
}

/**
 * 일정 관련 UseCase 그룹
 */
data class ScheduleUseCases(
    val addScheduleUseCase: AddScheduleUseCase,
    val deleteScheduleUseCase: DeleteScheduleUseCase,
    val getScheduleDetailUseCase: GetScheduleDetailUseCase,
    val getScheduleSummaryForMonthUseCase: GetScheduleSummaryForMonthUseCase,
    val getSchedulesForDateUseCase: GetSchedulesForDateUseCase,
    val updateScheduleUseCase: UpdateScheduleUseCase,
    
    // 공통 Repository
    val authRepository: AuthRepository,
    val scheduleRepository: ScheduleRepository
)