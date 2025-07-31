plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
}

android {
    namespace = "com.example.usecase"
}

dependencies {

    implementation(project(":core:core_common"))
    implementation(project(":domain:domain"))
    implementation(project(":domain:domain_repository"))

    // Clean Architecture에서 Domain 레이어는 데이터 소스에 의존하지 않고,
    // 비즈니스 로직과 엔티티, 리포지토리 인터페이스만 포함
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.firebase.auth)


    //firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
}