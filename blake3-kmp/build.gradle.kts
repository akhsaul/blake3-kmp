import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id("com.vanniktech.maven.publish.base")
    id("com.jakewharton.test-distribution")
}

kotlin {
    // let other module (ffm use JDK 25) set JDK version
    // jvmToolchain(11)
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    android {
        namespace = "com.akhsaul.blake3"
        compileSdk =
            libs.versions.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
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
        // Publish both build types from this single module:
        // - release AAR: production ABIs only (arm64-v8a, armeabi-v7a, x86)
        // - debug AAR: production ABIs + x86_64 for emulator tests +
        //   desktop JNI in classes.jar so host unit tests run with no setup.
        // macOS x86_64 desktop binaries live under the amd64 resource dir
        // (see blake3-kmp/build.zig); the loader tries both names.
        publishLibraryVariants("release", "debug")
    }
    applyDefaultHierarchyTemplate()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

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
        // Debug-only resources (desktop JNI for host tests) live in
        // src/androidDebug/resources, populated by CI (see build-package).
        // Declared explicitly so packaging does not depend on source-set
        // dir conventions; a missing dir contributes nothing.
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        invokeWhenCreated("androidDebug") {
            resources.srcDir("src/androidDebug/resources")
        }
    }
}

gradle.projectsEvaluated {
    listOf("androidHostTestCompileClasspath", "androidHostTestRuntimeClasspath").forEach { configurationName ->
        configurations.findByName(configurationName)?.attributes?.apply {
            attribute(
                Attribute.of("org.jetbrains.kotlin.platform.type", KotlinPlatformType::class.java),
                KotlinPlatformType.jvm,
            )
            attribute(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.STANDARD_JVM),
            )
        }
    }
}

mavenPublishing {
    configure(
        KotlinMultiplatform(
            // configures the - Javadoc artifact, possible values:
            // - `JavadocJar.None()` don't publish this artifact
            // - `JavadocJar.Empty()` publish an empty jar
            // - `JavadocJar.Dokka("dokkaHtml")` when using Kotlin with Dokka, where `dokkaHtml` is the name of the Dokka task that should be used as input
            // - Doesn't support `JavadocJar.Javadoc()` in KotlinMultiplatform
            javadocJar = JavadocJar.Empty(),
            // configures the -sources artifact, possible values:
            // - `SourcesJar.None()` don't publish this artifact
            // - `SourcesJar.Empty()` publish an empty jar
            // - `SourcesJar.Sources()` publish the sources
            sourcesJar = SourcesJar.None(),
            // configure which Android library variants to publish if this project has an Android target
            // release = production ABIs; debug = +x86_64 and desktop JNI for tests.
            // Must agree with kotlin.android.publishLibraryVariants above.
            androidVariantsToPublish = listOf("release", "debug"),
        ),
    )
    coordinates(group.toString(), "blake3-kmp", version.toString())
    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/akhsaul/blake3-kmp")
                credentials {
                    username =
                        providers.gradleProperty("gpr.user").orNull
                            ?: providers.environmentVariable("GITHUB_ACTOR").orNull
                    password =
                        providers.gradleProperty("gpr.key").orNull
                            ?: providers.environmentVariable("GITHUB_TOKEN").orNull
                }
            }
        }
    }
    pom {
        name.set("Blake3 KMP")
        description.set("Blake3 jni for jvm and android")
        inceptionYear.set("2026")
        url.set("https://github.com/akhsaul/blake3-kmp/")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("akhsaul")
                name.set("Ikhsan Maulana")
                url.set("https://github.com/akhsaul/")
            }
        }
        scm {
            url.set("https://github.com/akhsaul/blake3-kmp/")
            connection.set("scm:git:git://github.com/akhsaul/blake3-kmp.git")
            developerConnection.set("scm:git:ssh://git@github.com/akhsaul/blake3-kmp.git")
        }
    }
}
