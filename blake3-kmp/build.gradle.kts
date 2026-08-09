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

//    android {
//        namespace = "com.akhsaul.blake3"
//        compileSdk = libs.versions.compileSdk.get().toInt()
//
//        defaultConfig {
//            minSdk = libs.versions.minSdk.get().toInt()
//            multiDexEnabled = true
//            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//
//            ndk {
//                abiFilters += listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
//            }
//
//            externalNativeBuild {
//                cmake {
//                    arguments("-DANDROID_TOOLCHAIN=clang", "-DANDROID_STL=c++_static")
//                    cFlags("-fstrict-aliasing")
//                    cppFlags("-fstrict-aliasing")
//                    targets("blake3-kmp")
//                }
//            }
//        }
//
//        compileOptions {
//            sourceCompatibility = JavaVersion.VERSION_11
//            targetCompatibility = JavaVersion.VERSION_11
//        }
//
//        externalNativeBuild {
//            cmake {
//                path = file("src/androidMain/CMakeLists.txt")
//            }
//        }
//    }
    sourceSets {
        val commonMain by getting
        val jniMain by creating {
            dependsOn(commonMain)
        }
        jvmMain {
            dependsOn(jniMain)
        }
        androidMain {
            dependsOn(jniMain)
        }
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
