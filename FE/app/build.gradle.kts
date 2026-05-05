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
        minSdk = 24
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.kakao.sdk:v2-user:2.23.4")
    implementation("com.navercorp.nid:oauth:5.9.1")
    ksp("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
