plugins {
    id("com.android.library") // 버전을 적지 않습니다.
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    alias(libs.plugins.hilt.android) // 라이브러리도 플러그인이 필요합니다.
    id("kotlin-kapt")
}

android {
    compileSdk = 35
    namespace = "org.withus.app.data.remote"

    defaultConfig {
        minSdk = 26       // 최소 API 레벨을 24로 설정 (DataStore와 Compose 권장 사양)
        targetSdk = 35    // 컴파일 버전과 맞춤

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    compileOptions {
        // Java 컴파일러 버전을 17로 설정 (또는 21)
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        // Kotlin 컴파일러의 JVM 타겟을 17로 설정 (또는 21)
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}