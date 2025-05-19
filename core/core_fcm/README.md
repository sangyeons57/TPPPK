# FCM 모듈 (Firebase Cloud Messaging)

이 모듈은 안드로이드 앱에서 Firebase Cloud Messaging을 쉽게 구현하기 위한 기능을 제공합니다.

## 기능

- FCM 서비스 설정 (알림 수신 및 처리)
- FCM 토큰 관리 (생성, 저장, 조회)
- 토큰 변경 리스너 지원
- 서버 토큰 등록 관리
- WorkManager를 통한 주기적 토큰 갱신
- 알림 채널 생성 및 관리
- 토픽 구독 관리
- 알림 권한 요청 및 처리 (Android 13+)
- 메시지 타입별 처리 로직
- 장기 실행 작업 처리
- 자체 테스트 기능 (Compose UI)

## 설정 방법

### 1. 앱 수준 build.gradle에 의존성 추가

```kotlin
dependencies {
    implementation(project(":core:core_fcm"))
}
```

### 2. Firebase 프로젝트 설정

1. [Firebase 콘솔](https://console.firebase.google.com/)에서 프로젝트를 생성하거나 기존 프로젝트를 선택합니다.
2. '안드로이드 앱 추가'를 선택하고 패키지 이름 등 필요한 정보를 입력합니다.
3. `google-services.json` 파일을 다운로드하여 앱의 루트 디렉토리에 추가합니다.

### 3. 앱에서 FCM 초기화

```kotlin
// Application 클래스에서
@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var fcmManager: FCMManager
    
    override fun onCreate() {
        super.onCreate()
        
        // FCM 초기화
        fcmManager.initialize()
    }
}
```

## 사용 방법

### 토큰 관리

```kotlin
// FCM 토큰 가져오기
val token = fcmManager.getCurrentToken()

// 토큰 변경 리스너 등록
tokenManager.addTokenChangeListener(object : FCMTokenManager.TokenChangeListener {
    override fun onTokenChanged(oldToken: String?, newToken: String?) {
        // 토큰 변경 시 처리
    }
})
```

### 서버에 토큰 등록

```kotlin
// 사용자 로그인 성공 후 토큰 등록
fcmManager.registerTokenWithServer(userId) { success, message ->
    if (success) {
        // 성공 처리
    } else {
        // 실패 처리
    }
}
```

### 토픽 구독

```kotlin
// 특정 토픽 구독
fcmManager.subscribeTopic("news") { success ->
    if (success) {
        // 구독 성공
    } else {
        // 구독 실패
    }
}

// 토픽 구독 취소
fcmManager.unsubscribeTopic("news") { success ->
    // 처리 로직
}
```

### 알림 권한 처리 (Android 13+)

```kotlin
// 권한 확인
val hasPermission = fcmManager.hasNotificationPermission()

// 권한 요청
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    // ActivityResultLauncher를 사용한 권한 요청
    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
}
```

### 로그아웃 시 FCM 비활성화

```kotlin
// 사용자 로그아웃 시 FCM 비활성화
fcmManager.deactivate(
    userId = currentUserId,
    clearLocalToken = true,
    disableAutoInit = true
)
```

## 커스텀 알림 처리

FCM에서 받은 메시지를 처리하려면 메시지 유형에 따라 `FCMService` 클래스의 `handleNow()` 메서드에서 적절한 처리를 구현하세요:

```kotlin
private fun handleNow(data: Map<String, String>) {
    when (data["type"]) {
        "chat" -> handleChatMessage(data)
        "notification" -> handleNotificationMessage(data)
        // 추가 유형 처리
        else -> handleDefaultMessage(data)
    }
}
```

## 장기 실행 작업 처리

데이터 동기화와 같은 장기 실행 작업을 위해 WorkManager를 사용합니다:

```kotlin
// FCMService에서
private fun needsToBeScheduled(): Boolean {
    // 작업의 복잡성에 따라 WorkManager 사용 여부 결정
    return data["operation"] == "sync_data"
}
```

## 테스트 방법

### Compose 기반 테스트 화면 사용

모듈에는 기능 테스트를 위한 Compose 기반 테스트 화면이 포함되어 있습니다. 다음 단계에 따라 테스트 화면에 액세스할 수 있습니다:

1. AppRoutes.kt 파일에 FCM 테스트 경로가 정의되어 있는지 확인:
   ```kotlin
   object FCM {
       const val TEST = "fcm/test"
   }
   ```

2. AppNavigationGraph.kt 파일에 FCM 테스트 화면이 추가되어 있는지 확인:
   ```kotlin
   composable(AppRoutes.FCM.TEST) {
       FCMTestScreen(navigationHandler = navigationHandler)
   }
   ```

3. 테스트 화면으로 이동하기 위해 다음 코드를 사용:
   ```kotlin
   // 버튼 클릭 등의 이벤트에서
   navigationHandler.navigate(AppRoutes.FCM.TEST)
   ```

또는 앱 실행 시 테스트 화면부터 시작하려면 AppNavigationGraph에서 startDestination을 변경:

```kotlin
@Composable
fun AppNavigationGraph(
    navController: NavHostController,
    navigationHandler: ComposeNavigationHandler,
    startDestination: String = AppRoutes.FCM.TEST // 테스트용으로 변경
) {
    // ...
}
```

## 파이어베이스 콘솔에서 테스트 메시지 전송

1. [Firebase 콘솔](https://console.firebase.google.com/)에서 프로젝트 선택
2. 왼쪽 메뉴에서 '메시징(Cloud Messaging)' 선택
3. '첫 번째 메시지 전송' 또는 '메시지 보내기' 버튼 클릭
4. 알림 제목, 본문 등 입력
5. 데이터 메시지를 추가하려면 '사용자 지정 데이터' 섹션에 키-값 쌍 추가
   - 예: `type` - `chat`, `message` - `안녕하세요`
6. 대상 지정 후 메시지 보내기 