pluginManagement {
    repositories {
        google()
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

rootProject.name = "helm"

include(":app")
include(":sdk")
include(":core")
include(":widgets")
include(":themes")
include(":audio")
include(":navigation")
include(":bluetooth")
include(":carplay")
include(":radio")
include(":settings")
