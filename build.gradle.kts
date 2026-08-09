buildscript {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath(libs.mavenPublish.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
}

val envTag = System.getenv("GITHUB_REF_NAME")?.removePrefix("v")
val libraryVersion =
    providers
        .gradleProperty("version")
        .orNull
        ?.takeIf { it != "unspecified" }
        ?: envTag
        ?: "0.1.0-SNAPSHOT"

allprojects {
    group = "com.akhsaul.blake3"
    version = libraryVersion

    repositories {
        mavenCentral()
        google()
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint("1.8.0")
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**")
        ktlint("1.8.0")
    }
}
