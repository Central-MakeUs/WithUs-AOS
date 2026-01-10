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

    plugins {
        id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" 
    }

}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven { url = java.net.URI("https://devrepo.kakao.com/nexus/content/groups/public/") }

    }
}

rootProject.name = "CmcDemoApp"
include(":app")
include(":data:datasource:remote")
