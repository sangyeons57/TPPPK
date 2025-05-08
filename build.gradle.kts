// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    //id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
    //kotlin("jvm") version "1.9.22" apply false

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false

    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.dagger.hilt) apply false
    alias(libs.plugins.ksp) apply false

    alias(libs.plugins.google.gms) apply false
    alias(libs.plugins.sentry) apply false
    alias(libs.plugins.kotlin.compose) apply false
}