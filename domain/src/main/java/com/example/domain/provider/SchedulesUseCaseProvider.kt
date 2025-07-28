package com.example.domain.provider

import com.example.domain.repository.local.ScheduleLocalRepository
import com.example.domain.usecase.local.schedules.AddScheduleLocalUseCase
import com.example.domain.usecase.local.schedules.AddScheduleLocalUseCaseImpl
import com.example.domain.usecase.local.schedules.DeleteScheduleLocalUseCase
import com.example.domain.usecase.local.schedules.DeleteScheduleLocalUseCaseImpl
import com.example.domain.usecase.local.schedules.DeleteScheduleUseCase
import com.example.domain.usecase.local.schedules.DeleteScheduleUseCaseImpl
import com.example.domain.usecase.local.schedules.GetScheduleDetailLocalUseCase
import com.example.domain.usecase.local.schedules.GetScheduleDetailLocalUseCaseImpl
import com.example.domain.usecase.local.schedules.GetSchedulesForDateLocalUseCase
import com.example.domain.usecase.local.schedules.GetSchedulesForDateLocalUseCaseImpl
import com.example.domain.usecase.local.schedules.GetScheduleSummaryForMonthLocalUseCase
import com.example.domain.usecase.local.schedules.GetScheduleSummaryForMonthLocalUseCaseImpl
import com.example.domain.usecase.local.schedules.GetSchedulesUseCase
import com.example.domain.usecase.local.schedules.GetSchedulesUseCaseImpl
import com.example.domain.usecase.local.schedules.GetScheduleUseCase
import com.example.domain.usecase.local.schedules.GetScheduleUseCaseImpl
import com.example.domain.usecase.local.schedules.InsertScheduleUseCase
import com.example.domain.usecase.local.schedules.InsertScheduleUseCaseImpl
import com.example.domain.usecase.local.schedules.UpdateScheduleLocalUseCase
import com.example.domain.usecase.local.schedules.UpdateScheduleLocalUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 일정 관리 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 일정 생성, 수정, 삭제, 조회 등의 기능을 담당합니다.
 */
@Singleton
class SchedulesUseCaseProvider @Inject constructor(
    private val scheduleLocalRepository: ScheduleLocalRepository
) {

    /**
     * 일정 기본 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 일정 기본 관리 UseCase 그룹
     */
    fun createBasicUseCases(): SchedulesLocalBasicUseCases {
        return SchedulesLocalBasicUseCases(
            // 일정 조회
            getScheduleUseCase = GetScheduleUseCaseImpl(
                scheduleLocalRepository = scheduleLocalRepository
            ),
            
            getSchedulesUseCase = GetSchedulesUseCaseImpl(
                scheduleLocalRepository = scheduleLocalRepository
            ),
            
            getScheduleDetailLocalUseCase = GetScheduleDetailLocalUseCaseImpl(
                scheduleLocalRepository = scheduleLocalRepository
            ),
            
            // 일정 관리
            insertScheduleUseCase = InsertScheduleUseCaseImpl(
                scheduleLocalRepository = scheduleLocalRepository
            ),
            
            deleteScheduleUseCase = DeleteScheduleUseCaseImpl(
                scheduleLocalRepository = scheduleLocalRepository
            ),
            
            scheduleLocalRepository = scheduleLocalRepository
        )
    }

    /**
     * 일정 고급 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 일정 고급 관리 UseCase 그룹
     */
    fun createAdvancedUseCases(): SchedulesLocalAdvancedUseCases {
        return SchedulesLocalAdvancedUseCases(
            // 일정 추가/수정/삭제 (Local 버전)
            addScheduleLocalUseCase = AddScheduleLocalUseCaseImpl(
                scheduleLocalRepository = scheduleLocalRepository
            ),
            
            updateScheduleLocalUseCase = UpdateScheduleLocalUseCaseImpl(
                scheduleLocalRepository = scheduleLocalRepository
            ),
            
            deleteScheduleLocalUseCase = DeleteScheduleLocalUseCaseImpl(
                scheduleLocalRepository = scheduleLocalRepository
            ),
            
            // 날짜별/월별 조회
            getSchedulesForDateLocalUseCase = GetSchedulesForDateLocalUseCaseImpl(
                scheduleLocalRepository = scheduleLocalRepository
            ),
            
            getScheduleSummaryForMonthLocalUseCase = GetScheduleSummaryForMonthLocalUseCaseImpl(
                scheduleLocalRepository = scheduleLocalRepository
            ),
            
            scheduleLocalRepository = scheduleLocalRepository
        )
    }
}

/**
 * 일정 기본 관리 Local UseCase 그룹
 */
data class SchedulesLocalBasicUseCases(
    // 일정 조회
    val getScheduleUseCase: GetScheduleUseCase,
    val getSchedulesUseCase: GetSchedulesUseCase,
    val getScheduleDetailLocalUseCase: GetScheduleDetailLocalUseCase,
    
    // 일정 관리
    val insertScheduleUseCase: InsertScheduleUseCase,
    val deleteScheduleUseCase: DeleteScheduleUseCase,
    
    val scheduleLocalRepository: ScheduleLocalRepository
)

/**
 * 일정 고급 관리 Local UseCase 그룹
 */
data class SchedulesLocalAdvancedUseCases(
    // 일정 추가/수정/삭제 (Local 버전)
    val addScheduleLocalUseCase: AddScheduleLocalUseCase,
    val updateScheduleLocalUseCase: UpdateScheduleLocalUseCase,
    val deleteScheduleLocalUseCase: DeleteScheduleLocalUseCase,
    
    // 날짜별/월별 조회
    val getSchedulesForDateLocalUseCase: GetSchedulesForDateLocalUseCase,
    val getScheduleSummaryForMonthLocalUseCase: GetScheduleSummaryForMonthLocalUseCase,
    
    val scheduleLocalRepository: ScheduleLocalRepository
) 