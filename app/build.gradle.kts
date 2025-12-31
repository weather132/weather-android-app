plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.github.yun531.weatherapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.yun531.weatherapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            // 로컬(HTTP) 서버 테스트 용도면 필요
        }
    }

    // 로컬 HTTP(10.0.2.2) 테스트용이면 manifest에서 cleartext 허용도 같이 설정
    buildFeatures {
        compose = true
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2025.12.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.navigation:navigation-compose:2.8.4") // stable line 참고
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")

    // Pager (foundation)
    implementation("androidx.compose.foundation:foundation")

    // DataStore :contentReference[oaicite:3]{index=3}
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // WorkManager :contentReference[oaicite:4]{index=4}
    implementation("androidx.work:work-runtime-ktx:2.11.0")

    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Firebase (BoM + messaging)
    // Firebase BoM은 릴리즈 노트 기준으로 34.7.0 같은 버전이 갱신됨
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    // KTX 모듈은 BoM v34.0.0부터 제외/중단 흐름 → main 모듈 사용
    implementation("com.google.firebase:firebase-messaging")

    // Task await (Firebase KTX가 아니라, Play Services Task용 코루틴 유틸)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // NotificationCompat
    implementation("androidx.core:core-ktx:1.17.0")

    implementation("com.google.android.material:material:1.13.0")
}