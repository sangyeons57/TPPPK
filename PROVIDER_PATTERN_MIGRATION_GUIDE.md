# Repository Factory + Provider 패턴 마이그레이션 가이드

## 🎯 목적

기존의 개별 UseCase 주입 방식에서 **ProjectUseCaseProvider 패턴**으로 마이그레이션하여:
- Navigation 매개변수를 완벽 활용
- ViewModel 복잡성 대폭 감소  
- Repository Factory Context 패턴 유지
- Clean Architecture 원칙 준수

## 📊 Before vs After

### ❌ 기존 방식 (복잡함)
```kotlin
@HiltViewModel
class SomeViewModel @Inject constructor(
    private val projectRepositoryFactory: RepositoryFactory<ProjectRepositoryFactoryContext, ProjectRepository>,
    private val categoryRepositoryFactory: RepositoryFactory<CategoryRepositoryFactoryContext, CategoryRepository>,
    private val memberRepositoryFactory: RepositoryFactory<MemberRepositoryFactoryContext, MemberRepository>,
    // ... 더 많은 Factory들
) : ViewModel() {
    
    val projectRepository = projectRepositoryFactory.create(
        ProjectRepositoryFactoryContext(collectionPath = CollectionPath.projects)
    )
    val categoryRepository = categoryRepositoryFactory.create(
        CategoryRepositoryFactoryContext(collectionPath = TODO("복잡한 경로 설정"))
    )
    // ... 수동 Repository 생성
    
    private val someUseCase = SomeUseCase(projectRepository, categoryRepository, ...)
}
```

### ✅ 새로운 방식 (간단함)
```kotlin
@HiltViewModel
class SomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectUseCaseProvider: ProjectUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {
    
    private val projectId: String = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID) 
        ?: error("프로젝트 ID가 필요합니다")
    
    // 한 번에 모든 UseCase 생성 (올바른 경로로 자동 설정)
    private val useCases = projectUseCaseProvider.createForProject(projectId)
    
    fun doSomething() {
        useCases.someUseCase()
    }
    
    fun navigateToSomewhere() {
        navigationManger.navigateToSomewhere()
    }
}
```

## 🔧 마이그레이션 단계

### 1단계: ViewModel 의존성 변경

**Before:**
```kotlin
@HiltViewModel
class YourViewModel @Inject constructor(
    private val useCase1: UseCase1,
    private val useCase2: UseCase2,
    // ... 개별 UseCase들
) : ViewModel()
```

**After:**
```kotlin
@HiltViewModel
class YourViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, // 매개변수 수신용
    private val projectUseCaseProvider: ProjectUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel()
```

### 2단계: UseCase 생성 방식 변경

**Before:**
```kotlin
// 개별 UseCase 직접 사용
fun doSomething() {
    useCase1()
}
```

**After:**
```kotlin
// Navigation에서 projectId 추출
private val projectId: String = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID) 
    ?: error("프로젝트 ID가 필요합니다")

// Provider를 통해 UseCase 그룹 생성
private val useCases = projectUseCaseProvider.createForProject(projectId)

fun doSomething() {
    useCases.useCase1()
}
```

### 3단계: 네비게이션 통합

**Before:**
```kotlin
// 네비게이션 로직이 없거나 분산됨
```

**After:**
```kotlin
fun navigateToChannel(channelId: String) {
    navigationManger.navigateToChat(channelId)
}

fun navigateBack() {
    navigationManger.navigateBack()
}
```

## 📋 Provider가 제공하는 UseCase 목록

### ProjectUseCases (특정 프로젝트용)
```kotlin
data class ProjectUseCases(
    // 기본 프로젝트 관리
    val createProjectUseCase: CreateProjectUseCase,
    val deleteProjectUseCase: DeleteProjectUseCase,
    val joinProjectWithCodeUseCase: JoinProjectWithCodeUseCase,
    val joinProjectWithTokenUseCase: JoinProjectWithTokenUseCase,
    val getProjectDetailsStreamUseCase: GetProjectDetailsStreamUseCase,
    
    // 프로젝트 멤버 관리
    val getProjectMemberDetailsUseCase: GetProjectMemberDetailsUseCase,
    val deleteProjectMemberUseCase: DeleteProjectMemberUseCase,
    val observeProjectMembersUseCase: ObserveProjectMembersUseCase,
    val renameProjectUseCase: RenameProjectUseCase,
    
    // 채널 관리
    val createProjectChannelUseCase: CreateProjectChannelUseCase,
    val getProjectChannelUseCase: GetProjectChannelUseCase,
    val updateProjectChannelUseCase: UpdateProjectChannelUseCase,
    
    // 공통
    val authRepository: AuthRepository
)
```

### UserProjectUseCases (사용자별)
```kotlin
data class UserProjectUseCases(
    val getUserParticipatingProjectsUseCase: GetUserParticipatingProjectsUseCase,
    val getProjectDetailsStreamUseCase: GetProjectDetailsStreamUseCase,
    val authRepository: AuthRepository
)
```

## 🎯 실제 사용 사례

### Case 1: 프로젝트 상세 화면
```kotlin
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectUseCaseProvider: ProjectUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {
    
    private val projectId: String = savedStateHandle.get<String>(AppRoutes.Project.ARG_PROJECT_ID) 
        ?: error("프로젝트 ID가 필요합니다")
    
    private val useCases = projectUseCaseProvider.createForProject(projectId)
    
    init {
        loadProjectDetails()
    }
    
    private fun loadProjectDetails() {
        viewModelScope.launch {
            useCases.getProjectDetailsStreamUseCase(projectId).collect { result ->
                // 프로젝트 상세 정보 처리
            }
        }
    }
    
    fun createChannel(channelName: String) {
        viewModelScope.launch {
            val result = useCases.createProjectChannelUseCase(projectId, channelName, ...)
            if (result is CustomResult.Success) {
                navigationManger.navigateToChat(result.data.value)
            }
        }
    }
}
```

### Case 2: 프로젝트 추가 화면
```kotlin
@HiltViewModel
class AddProjectViewModel @Inject constructor(
    private val projectUseCaseProvider: ProjectUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {
    
    private var currentUserId: String? = null
    
    init {
        initializeUserContext()
    }
    
    private fun initializeUserContext() {
        viewModelScope.launch {
            val tempUserUseCases = projectUseCaseProvider.createForUser("temp")
            when (val session = tempUserUseCases.authRepository.getCurrentUserSession()) {
                is CustomResult.Success -> {
                    currentUserId = session.data.userId
                }
                else -> {
                    // 인증 오류 처리
                }
            }
        }
    }
    
    fun createProject(projectName: String) {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            val useCases = projectUseCaseProvider.createForProject("temp", userId)
            val result = useCases.createProjectUseCase(projectName)
            
            if (result is CustomResult.Success) {
                navigationManger.navigateBack()
            }
        }
    }
}
```

## 🚀 추가 확장 가능한 Provider들

필요에 따라 다른 영역의 Provider도 추가할 수 있습니다:

### ScheduleUseCaseProvider
```kotlin
@Singleton
class ScheduleUseCaseProvider @Inject constructor(
    // Schedule 관련 Repository Factory들
) {
    fun createForUser(userId: String): UserScheduleUseCases
    fun createForProject(projectId: String): ProjectScheduleUseCases
}
```

### UserUseCaseProvider
```kotlin
@Singleton
class UserUseCaseProvider @Inject constructor(
    // User 관련 Repository Factory들
) {
    fun createForUser(userId: String): UserUseCases
    fun createForFriend(userId: String, friendId: String): FriendUseCases
}
```

## ✅ 체크리스트

각 feature 모듈을 마이그레이션할 때 다음을 확인하세요:

### ViewModel 마이그레이션
- [ ] 개별 UseCase 주입 → Provider 주입으로 변경
- [ ] SavedStateHandle 추가 (Navigation 매개변수용)
- [ ] NavigationManger 주입
- [ ] UseCase 사용 방식 변경 (`useCases.someUseCase()`)

### 네비게이션 통합
- [ ] 네비게이션 메서드 추가
- [ ] Screen에서 ViewModel 네비게이션 메서드 사용
- [ ] 직접 NavigationManger 사용 제거

### 테스트 업데이트
- [ ] Provider Mock 생성
- [ ] UseCase Mock → Provider Mock으로 변경
- [ ] Navigation Mock 추가

## 🎯 Benefits

이 패턴을 적용하면:

1. **코드 간소화**: ViewModel당 평균 50-70% 코드 감소
2. **타입 안전성**: Navigation 매개변수와 Repository 경로 완벽 매칭
3. **재사용성**: Provider로 UseCase 그룹 재사용
4. **테스트 용이성**: Provider 단위로 Mock 생성
5. **유지보수성**: 중앙집중식 UseCase 관리

## 🔗 관련 파일들

- `domain/usecase/project/ProjectUseCaseProvider.kt` - 메인 Provider
- `feature/feature_add_project/viewmodel/AddProjectViewModel.kt` - 적용 예시 1
- `feature/feature_project_detail/viewmodel/ProjectDetailViewModel.kt` - 적용 예시 2

---

💡 **팁**: 새로운 feature 모듈을 만들 때는 처음부터 이 패턴을 사용하면 개발 속도가 훨씬 빨라집니다!