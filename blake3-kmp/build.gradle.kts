import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id("com.vanniktech.maven.publish.base")
    id("com.jakewharton.test-distribution")
}

kotlin {
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
            // defaults to "release" when using the main plugin and nothing for the base plugin
            androidVariantsToPublish = listOf("release"),
        ),
    )
    coordinates(group.toString(), "blake3-kmp", version.toString())
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
