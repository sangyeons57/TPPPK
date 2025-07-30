plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.org.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.data_model"
}

dependencies {

    // Core modules
    implementation(project(":domain:domain"))
    implementation(project(":domain:domain_usecase"))
    implementation(project(":domain:domain_repository"))
    implementation(project(":core:core_common"))

    // Test dependencies
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)

    //firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.storage)
    // Task.await() 사용을 위한 의존성 추가
    implementation(libs.kotlinx.coroutines.play.services) // 버전은 libs.versions.toml 또는 직접 지정 (예: "1.7.3")

    // Also add the dependency for the Google Play services library and specify its version
    //implementation(libs.play.services.auth)
    //implementation(libs.play.services.base)

    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries
    // Firebase BoM (Bill of Materials) - Firebase 라이브러리 버전 관리를 위한 BOM

    // Room Database 의존성 추가
    implementation(libs.androidx.room.runtime) // 또는 implementation "androidx.room:room-runtime:2.6.1"
    implementation(libs.androidx.room.ktx)      // 또는 implementation "androidx.room:room-ktx:2.6.1"
    ksp(libs.androidx.room.compiler)            // 또는 ksp "androidx.room:room-compiler:2.6.1"
    androidTestImplementation(libs.androidx.room.testing) // Room 테스트 의존성 추가
}