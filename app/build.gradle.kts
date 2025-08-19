plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
}

android {
    namespace = "com.android.klaudiak.sttlab"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.android.klaudiak.sttlab"
        minSdk = 28
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
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
    }
    sourceSets.named("main") {
        jniLibs.srcDirs("src/main/jniLibs")
    }

    /*
    packaging {
         jniLibs.pickFirsts.add("lib/arm64-v8a/libkaldi-native-fbank-core.so")
         jniLibs.pickFirsts.add("lib/arm64-v7a/libkaldi-native-fbank-core.so")
         jniLibs.pickFirsts.add("lib/armeabi-v7a/libkaldi-native-fbank-core.so")
         jniLibs.pickFirsts.add("lib/x86/libkaldi-native-fbank-core.so")
         jniLibs.pickFirsts.add("lib/x86_64/libkaldi-native-fbank-core.so")
     }*/
}

dependencies {
    implementation(project(":sherpa_ncnn"))
 //   implementation(project(":sherpa_onnx"))
    implementation(project(":core"))
    implementation(project(":audioplayer"))
    implementation(project(":vosk-stt"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

}