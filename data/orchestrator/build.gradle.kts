plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.org.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.orchestrator"
}

dependencies {

    implementation(project(":core:core_common"))
    implementation(project(":domain:domain"))
    implementation(project(":domain:domain_repository"))
    implementation(project(":data:data_model"))
    implementation(project(":data:data_datasource"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Room Database 의존성 추가
    implementation(libs.androidx.room.runtime) // 또는 implementation "androidx.room:room-runtime:2.6.1"
    implementation(libs.androidx.room.ktx)      // 또는 implementation "androidx.room:room-ktx:2.6.1"
    ksp(libs.androidx.room.compiler)            // 또는 ksp "androidx.room:room-compiler:2.6.1"
    androidTestImplementation(libs.androidx.room.testing) // Room 테스트 의존성 추가
}