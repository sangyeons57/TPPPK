// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false

    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.dagger.hilt) apply false
    alias(libs.plugins.ksp) apply false

    alias(libs.plugins.google.gms) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Common configuration for all subprojects
subprojects {
    // Apply JDK 17 toolchain to all Java modules
    plugins.withId("java") {
        the<JavaPluginExtension>().toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    // Apply JVM toolchain to all Kotlin modules
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>("kotlin") {
            jvmToolchain(17)
        }
    }
    
    plugins.withId("org.jetbrains.kotlin.android") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension>("kotlin") {
            jvmToolchain(17)
        }
    }

    // Common Android configuration for all Android library modules
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.gradle.LibraryExtension>("android") {
            compileSdk = 36

            defaultConfig {
                minSdk = 29
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }

            buildTypes {
                release {
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }


            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }
        }
    }

    // Common Android configuration for application modules
    plugins.withId("com.android.application") {
        extensions.configure<com.android.build.gradle.internal.dsl.BaseAppModuleExtension>("android") {
            compileSdk = 36

            defaultConfig {
                minSdk = 29
                targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            buildTypes {
                release {
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }


            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }
        }
    }

    // Common dependencies for all Android modules
    plugins.withId("com.android.library") {
        dependencies {
            // Core Android dependencies
            add("implementation", libs.androidx.core.ktx)
            add("implementation", libs.androidx.appcompat)
            add("implementation", libs.material)

            // Compose BOM and core dependencies
            add("implementation", platform(libs.androidx.compose.bom))
            add("implementation", libs.androidx.compose.ui)
            add("implementation", libs.androidx.compose.ui.tooling.preview)
            add("implementation", libs.androidx.compose.material3)
            add("implementation", libs.androidx.compose.material.icons.extended)
            add("implementation", libs.androidx.compose.runtime)
            add("implementation", libs.androidx.compose.lifecycle.viewmodel)
            add("implementation", libs.androidx.compose.lifecycle.runtime)

            // Common test dependencies
            add("testImplementation", libs.junit)
            add("androidTestImplementation", libs.androidx.junit)
            add("androidTestImplementation", libs.androidx.espresso.core)
            add("androidTestImplementation", platform(libs.androidx.compose.bom))
            add("androidTestImplementation", libs.androidx.compose.ui.test.junit4)
            add("debugImplementation", libs.androidx.compose.ui.tooling)
            add("debugImplementation", libs.androidx.compose.ui.test.manifest)
        }
    }

    // Hilt dependencies when both Hilt and KSP plugins are applied
    plugins.withId("com.google.devtools.ksp") {
        plugins.withId("dagger.hilt.android.plugin") {
            dependencies {
                add("implementation", libs.hilt.android)
                add("ksp", libs.hilt.compiler)
                add("implementation", libs.androidx.compose.hilt.navigation)
            }
        }
    }

    // Enable Compose build features when Kotlin Compose plugin is applied
    plugins.withId("org.jetbrains.kotlin.plugin.compose") {
        plugins.withId("com.android.library") {
            extensions.configure<com.android.build.gradle.LibraryExtension>("android") {
                buildFeatures {
                    compose = true
                }
            }
        }
        plugins.withId("com.android.application") {
            extensions.configure<com.android.build.gradle.internal.dsl.BaseAppModuleExtension>("android") {
                buildFeatures {
                    compose = true
                }
            }
        }
    }
}