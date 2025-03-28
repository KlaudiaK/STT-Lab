plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.android.klaudiak.benchmark"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
        targetSdk = 35

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // This benchmark buildType is used for benchmarking, and should function like your
        // release build (for example, with minification on). It"s signed with a debug key
        // for easy local/CI testing.
        create("benchmark") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

}

dependencies {
    implementation(project(":domain"))
    implementation(project(":audioplayer"))
    implementation(project(":sherpa_ncnn"))
    //   implementation(project(":sherpa-onnx"))
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.ui.test.junit4.android)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.rules)
    implementation(libs.androidx.benchmark.junit4)
    debugImplementation(libs.ui.test.manifest)
}

androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}