plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.data_model"
}

dependencies {
    implementation(project(":core:core_common"))
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)

    // Room Database 의존성 추가
    implementation(libs.androidx.room.runtime) // 또는 implementation "androidx.room:room-runtime:2.6.1"
    implementation(libs.androidx.room.ktx)      // 또는 implementation "androidx.room:room-ktx:2.6.1"
    ksp(libs.androidx.room.compiler)            // 또는 ksp "androidx.room:room-compiler:2.6.1"
    androidTestImplementation(libs.androidx.room.testing) // Room 테스트 의존성 추가
}