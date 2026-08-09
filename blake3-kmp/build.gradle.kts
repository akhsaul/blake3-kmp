import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id("com.vanniktech.maven.publish.base")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        withSourcesJar(false)
    }
    android {
        namespace = "com.akhsaul.blake3"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            execution = "HOST"
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        withSourcesJar(false)
    }
    applyDefaultHierarchyTemplate()

    withSourcesJar(false)

    sourceSets {
        val jniMain =
            create("jniMain") {
                dependsOn(commonMain.get())
            }
        jvmMain.get().dependsOn(jniMain)
        androidMain.get().dependsOn(jniMain)
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val androidDeviceTest = findByName("androidDeviceTest")
        androidDeviceTest?.dependencies {
            implementation(kotlin("test"))
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
        }
    }
}
