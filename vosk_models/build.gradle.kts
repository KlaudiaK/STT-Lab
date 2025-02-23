import java.util.UUID

plugins {
    id("com.android.library")
}

android {
    namespace = "org.vosk.models"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    buildFeatures {
        buildConfig = false
    }

    sourceSets {
        named("main") {
            assets.srcDirs("build/generated/assets")
        }
    }
}

tasks.register("genUUID") {
    doLast {
        val uuid = UUID.randomUUID().toString()
        val odir = file("${buildDir}/generated/assets/model-en-us")
        val ofile = file("$odir/uuid")
        mkdir(odir)
        ofile.writeText(uuid)
    }
}

tasks.named("preBuild") {
    dependsOn("genUUID")
}
