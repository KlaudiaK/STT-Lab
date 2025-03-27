import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "STT Lab"
include(":app")
include(":core")
include(":domain")
include(":audioplayer")
include(":vosk-stt")
include(":vosk_models")
include(":sherpa-ncnn")
include(":sherpa-onnx")
include(":benchmark")
