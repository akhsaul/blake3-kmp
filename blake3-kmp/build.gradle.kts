import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id("com.vanniktech.maven.publish.base")
    id("binary-compatibility-validator")
    id("com.jakewharton.test-distribution")
}

group = "com.akhsaul.blake3"
version = "0.1.0"

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
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
    }
    applyDefaultHierarchyTemplate()

    sourceSets {
        val jniMain = create("jniMain") {
            dependsOn(commonMain.get())
        }
        jvmMain.get().dependsOn(jniMain)
        androidMain.get().dependsOn(jniMain)
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Disable host-side unit tests. Testing is done with device instrumentation tests.
//androidComponents {
//    beforeVariants {
//        (it as HasUnitTestBuilder).enableUnitTest = false
//    }
//}
