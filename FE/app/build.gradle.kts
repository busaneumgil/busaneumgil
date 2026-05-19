import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.android")
}

fun quoted(value: String) = "\"$value\""

val appLocalProperties =
    Properties().apply {
        val localFile = rootProject.file("app.local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use(::load)
        }
    }

fun appProperty(name: String, defaultValue: String): String {
    return providers.gradleProperty(name).orNull
        ?: appLocalProperties.getProperty(name)
        ?: defaultValue
}

val defaultBaseUrl = "https://api.dev.busaneumgil.com/"
val debugBaseUrl = appProperty("app.debug.baseUrl", defaultBaseUrl)
val debugMockMode = appProperty("app.debug.mockMode", "false")
val debugDemoMode = appProperty("app.debug.demoMode", "false")
val debugForceLowVisionTermsGuide = appProperty("app.debug.forceLowVisionTermsGuide", "false")
val debugKakaoNativeAppKey = appProperty("app.debug.kakaoNativeAppKey", "")
val debugNaverClientId = appProperty("app.debug.naverClientId", "")
val debugNaverClientSecret = appProperty("app.debug.naverClientSecret", "")
val debugNaverClientName = appProperty("app.debug.naverClientName", "BusanEumGil")
val releaseBaseUrl = appProperty("app.release.baseUrl", debugBaseUrl)
val releaseKakaoNativeAppKey = appProperty("app.release.kakaoNativeAppKey", debugKakaoNativeAppKey)
val releaseNaverClientId = appProperty("app.release.naverClientId", debugNaverClientId)
val releaseNaverClientSecret = appProperty("app.release.naverClientSecret", debugNaverClientSecret)
val releaseNaverClientName = appProperty("app.release.naverClientName", debugNaverClientName)

android {
    namespace = "com.ssafy.e102.eumgil"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ssafy.e102.eumgil"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", quoted(debugBaseUrl))
            buildConfigField("boolean", "IS_MOCK_MODE", debugMockMode)
            buildConfigField("boolean", "IS_DEMO_MODE", debugDemoMode)
            buildConfigField("boolean", "FORCE_LOW_VISION_TERMS_GUIDE", debugForceLowVisionTermsGuide)
            buildConfigField("String", "KAKAO_NATIVE_APP_KEY", quoted(debugKakaoNativeAppKey))
            buildConfigField("String", "NAVER_CLIENT_ID", quoted(debugNaverClientId))
            buildConfigField("String", "NAVER_CLIENT_SECRET", quoted(debugNaverClientSecret))
            buildConfigField("String", "NAVER_CLIENT_NAME", quoted(debugNaverClientName))
            manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = debugKakaoNativeAppKey
            manifestPlaceholders["USES_CLEARTEXT_TRAFFIC"] = "true"
        }

        release {
            isMinifyEnabled = false
            buildConfigField("String", "BASE_URL", quoted(releaseBaseUrl))
            buildConfigField("boolean", "IS_MOCK_MODE", "false")
            buildConfigField("boolean", "IS_DEMO_MODE", "false")
            buildConfigField("boolean", "FORCE_LOW_VISION_TERMS_GUIDE", "false")
            buildConfigField("String", "KAKAO_NATIVE_APP_KEY", quoted(releaseKakaoNativeAppKey))
            buildConfigField("String", "NAVER_CLIENT_ID", quoted(releaseNaverClientId))
            buildConfigField("String", "NAVER_CLIENT_SECRET", quoted(releaseNaverClientSecret))
            buildConfigField("String", "NAVER_CLIENT_NAME", quoted(releaseNaverClientName))
            manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = releaseKakaoNativeAppKey
            manifestPlaceholders["USES_CLEARTEXT_TRAFFIC"] = "false"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // sherpa-onnx AAR 내 .so 중복 방지
            pickFirsts += listOf("lib/x86/libonnxruntime.so", "lib/x86_64/libonnxruntime.so", "lib/armeabi-v7a/libonnxruntime.so", "lib/arm64-v8a/libonnxruntime.so")
        }
    }
}

tasks.register("testClasses") {
    group = "verification"
    description = "Compatibility task for IDE runners that expect Java plugin-style testClasses."
    dependsOn("compileDebugUnitTestSources")
}

dependencies {
    // sherpa-onnx on-device STT (AAR at app/libs/)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.kakao.maps.open:android:2.13.1")
    implementation("com.kakao.sdk:v2-user:2.23.4")
    implementation("com.google.android.gms:play-services-auth:21.5.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.navercorp.nid:oauth:5.9.1")
    // Coil: 제보 첨부 사진 / 마이페이지 내역 카드 썸네일 렌더링. Compose 통합 라이브러리.
    // 2.7.0은 Compose BOM 2024.02.x와 호환된 안정 버전.
    implementation("io.coil-kt:coil-compose:2.7.0")
    ksp("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.robolectric:robolectric:4.11.1")

    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
