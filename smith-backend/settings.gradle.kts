rootProject.name = "smith-backend"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("domain", "api", "infrastructure", "application", "api-impl")
