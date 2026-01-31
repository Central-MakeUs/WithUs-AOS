import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
    alias(libs.plugins.hilt.android) // Hilt 플러그인 적용
    id("kotlin-kapt") // 또는 id("com.google.devtools.ksp")
}

android {

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    namespace = "com.withus.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.withus.app3"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        //noinspection DataBindingWithoutKapt
        dataBinding = true
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.bundles.camerax)

    implementation(project(":data:datasource:remote"))

    implementation(libs.androidx.material3.android)
    implementation(libs.fragment.ktx)
    implementation(libs.androidx.ui)
    implementation(libs.play.services.location)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.ui)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.coroutines.rx3)
    implementation(libs.androidx.window)

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0-alpha13")


    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation(libs.timber)
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("com.kakao.sdk:v2-user:2.20.6")
    implementation("com.kakao.sdk:v2-share:2.20.6")

    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    implementation("io.coil-kt:coil-compose:2.4.0")


    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler) // 코드 생성을 위한 컴파일러
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.accompanist.permissions)

    implementation("androidx.datastore:datastore-preferences:1.0.0")
}