pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()   // 必须保留，解析 Kotlin/KSP 等非 Google 插件
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MeetingRecoAI"
include(":app")
