# FCM 모듈 (Firebase Cloud Messaging)

이 모듈은 안드로이드 앱에서 Firebase Cloud Messaging을 쉽게 구현하기 위한 기능을 제공합니다.

## 기능

- FCM 서비스 설정 (알림 수신 및 처리)
- FCM 토큰 관리 (생성, 저장, 조회)
- 알림 채널 생성 및 관리
- 토픽 구독 관리
- 알림 권한 요청 및 처리 (Android 13+)

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
    lateinit var fcmInitializer: FCMInitializer
    
    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    override fun onCreate() {
        super.onCreate()
        
        // FCM 초기화
        fcmInitializer.initialize()
        
        // 알림 채널 생성
        notificationHelper.createNotificationChannels()
    }
}
```

## 사용 예시

### 알림 권한 요청 (Android 13+)

```kotlin
// 액티비티 또는 프래그먼트에서
private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PermissionUtils.requestNotificationPermission(
            this,
            onGranted = {
                // 권한이 허용된 경우의 처리
                Toast.makeText(this, "알림 권한이 허용되었습니다", Toast.LENGTH_SHORT).show()
            },
            onDenied = {
                // 권한이 거부된 경우의 처리
                Toast.makeText(this, "알림 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
```

### 토픽 구독

```kotlin
@Inject
lateinit var fcmTopicManager: FCMTopicManager

// 토픽 구독
fun subscribeToNotifications() {
    fcmTopicManager.subscribeTopic(FCMTopicManager.TOPIC_GENERAL) { success ->
        if (success) {
            Toast.makeText(context, "알림 구독이 완료되었습니다", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "알림 구독에 실패했습니다", Toast.LENGTH_SHORT).show()
        }
    }
}

// 여러 토픽 구독
fun subscribeToMultipleTopics() {
    val topics = listOf(
        FCMTopicManager.TOPIC_GENERAL,
        FCMTopicManager.TOPIC_UPDATES
    )
    
    fcmTopicManager.subscribeToTopics(topics) { successCount ->
        Toast.makeText(
            context, 
            "$successCount/${topics.size} 토픽 구독 완료", 
            Toast.LENGTH_SHORT
        ).show()
    }
}
```

### FCM 토큰 가져오기

```kotlin
@Inject
lateinit var fcmInitializer: FCMInitializer

@Inject
lateinit var fcmTokenManager: FCMTokenManager

// FCM 토큰 가져오기 (비동기)
fun getFCMToken() {
    fcmInitializer.getToken()
}

// 저장된 토큰 가져오기
fun getStoredToken(): String? {
    return fcmTokenManager.getToken()
}
```

## 사용자 정의 알림 처리

FCM 메시지 처리를 사용자 정의하려면 다음과 같이 커스텀 확장 함수를 구현할 수 있습니다:

```kotlin
// 특정 타입의 알림을 처리하는 확장 함수
fun FCMService.handleCustomNotification(data: Map<String, String>) {
    // data["type"], data["title"], data["message"] 등의 정보를 활용해 처리
}
```

## 주의사항

1. Android 13(API 33) 이상에서는 알림 권한을 런타임에 요청해야 합니다.
2. FCM 메시지는 앱의 상태(포그라운드/백그라운드)에 따라 다르게 처리됩니다.
3. 백그라운드 메시지 처리는 20초로 제한되므로 장기 실행 작업은 WorkManager를 사용하세요. 