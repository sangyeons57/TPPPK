plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.feature_chat"
}

dependencies {
    // --- 모듈 의존성 ---
    implementation(project(":domain"))
    implementation(project(":core:core_common"))
    implementation(project(":core:core_navigation"))
    implementation(project(":core:core_ui")) // 공통 유틸리티 사용
    // ★ 중요: 현재 ChatViewModel이 Repository 구현체(ChatRepositoryImpl)가 제공하는
    //    ChatRepository 인터페이스를 직접 주입받으므로 :data 모듈 의존성이 필요합니다.
    //    (이상적으로는 ViewModel은 UseCase를 주입받고 UseCase가 Repository 인터페이스 사용)
    implementation(project(":data"))

    // Module-specific dependencies
    // Coil (Image Loading)
    implementation(libs.androidx.compose.coil) // 버전 통일

    // WebSocket dependencies
    implementation(libs.okhttp.websocket)
    implementation(libs.kotlinx.serialization.json)

    // Paging dependencies
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // 기타 ChatScreen, ChatViewModel에서 사용하는 라이브러리 (예: Activity Result API)
    implementation(libs.androidx.compose.activity)

    // 테스트 의존성
    testImplementation(libs.mockk) // MockK 의존성 추가
    testImplementation(libs.kotlinx.coroutines.test) // 코루틴 테스트 의존성 추가
}