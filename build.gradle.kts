buildscript {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath(libs.binary.compatibility.validator.gradle.plugin)
        classpath(libs.mavenPublish.gradle.plugin)
        classpath(libs.shadowJar.gradle.plugin)
        classpath(libs.testDistributionGradlePlugin)
    }
}

plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
}

allprojects {
    group = "com.akhsaul.blake3"
    version = "0.1.0"

    repositories {
        mavenCentral()
        google()
    }
}