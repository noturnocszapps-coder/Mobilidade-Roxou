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

rootProject.name = "RoxouMobilidade"

include(":app")
include(":core-domain")
include(":core-licenca")
include(":core-data")
include(":core-ui")
include(":feature-trips")
include(":feature-goals")
include(":feature-rental")
include(":feature-finance")
include(":overlay")
