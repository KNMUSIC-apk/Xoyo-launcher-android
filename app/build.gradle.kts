plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Client ID đăng ký trên Azure (public client, không có secret).
// Đặt trong ~/.gradle/gradle.properties hoặc truyền bằng -PXOYO_MS_CLIENT_ID=...
val xoyoMsClientId: String = (project.findProperty("XOYO_MS_CLIENT_ID") as? String).orEmpty()

android {
    namespace = "com.xoyo.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xoyo.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "XOYO_MS_CLIENT_ID", "\"$xoyoMsClientId\"")
    }

    buildFeatures {
        buildConfig = true
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)
}
