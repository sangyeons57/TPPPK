plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
}
android {
    namespace = "com.example.mapper"
}

dependencies {
    implementation(project(":core:core_common"))
    implementation(project(":domain"))
    implementation(project(":data:data_model"))

    // Clean Architecture에서 Domain 레이어는 데이터 소스에 의존하지 않고,
    // 비즈니스 로직과 엔티티, 리포지토리 인터페이스만 포함
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.firebase.auth.ktx)

    // 테스트 라이브러리
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk) // MockK 의존성 추가
    testImplementation(project(":feature:feature_chat")) // Added to resolve ChatViewModel in tests

    //firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
}