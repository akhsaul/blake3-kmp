import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

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
        commonMain {
            kotlin.srcDir("../blake3-kmp/src/commonMain/kotlin")
        }
        val jniMain =
            create("jniMain") {
                dependsOn(commonMain.get())
                kotlin.srcDir("../blake3-kmp/src/jniMain/kotlin")
            }
        jvmMain.get().apply {
            dependsOn(jniMain)
            kotlin.srcDir("../blake3-kmp/src/jvmMain/kotlin")
            kotlin.exclude("**/JvmBlake3.kt")
            kotlin.srcDir("src/jvmMain/kotlin")
            resources.srcDir("../blake3-kmp/src/jvmMain/resources")
        }
        androidMain.get().apply {
            dependsOn(jniMain)
            kotlin.srcDir("../blake3-kmp/src/androidMain/kotlin")
        }
        commonTest {
            kotlin.srcDir("../blake3-kmp/src/commonTest/kotlin")
            resources.srcDir("../blake3-kmp/src/commonTest/resources")
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidDeviceTest = findByName("androidDeviceTest")
        androidDeviceTest?.apply {
            kotlin.srcDir("../blake3-kmp/src/androidDeviceTest/kotlin")
            resources.srcDir("../blake3-kmp/src/androidDeviceTest/resources")
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.ext.junit)
            }
        }
    }
}

val hostJvmNativeResources =
    listOf(
        file("src/jvmMain/resources/jni/aarch64/libblake3-kmp.so"),
        file("src/jvmMain/resources/jni/amd64/libblake3-kmp.so"),
        file("src/jvmMain/resources/jni/amd64/libblake3-kmp.dylib"),
    )

tasks.configureEach {
    if (name == "bundleAndroidMainAar") {
        inputs.files(hostJvmNativeResources).withPropertyName("hostJvmNativeResources")
        doLast {
            val archiveFile = outputs.files.singleFile
            val aarEntries = linkedMapOf<String, Pair<ByteArray, Long>>()
            ZipFile(archiveFile).use { input ->
                val entries = input.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val bytes = input.getInputStream(entry).use { it.readBytes() }
                    aarEntries[entry.name] = bytes to entry.time
                }
            }

            // Keep host payloads out of classes.jar because its resources leak into consuming APKs.
            hostJvmNativeResources.forEach { nativeLibrary ->
                check(nativeLibrary.isFile) { "Missing host JNI resource: $nativeLibrary" }
                val resourcePath = nativeLibrary.relativeTo(file("src/jvmMain/resources")).invariantSeparatorsPath
                val bytes = nativeLibrary.readBytes()
                val hostPath = resourcePath.removePrefix("jni/")
                aarEntries["host-native/$hostPath"] = bytes to 315532800000L
            }

            val temporaryArchive = archiveFile.resolveSibling("${archiveFile.name}.tmp")
            temporaryArchive.outputStream().buffered().use { output ->
                ZipOutputStream(output).use { zip ->
                    aarEntries.forEach { (name, payload) ->
                        val entry = ZipEntry(name)
                        if (payload.second > 0L) entry.time = payload.second
                        zip.putNextEntry(entry)
                        zip.write(payload.first)
                        zip.closeEntry()
                    }
                }
            }
            check(
                temporaryArchive.renameTo(archiveFile) ||
                    temporaryArchive.copyTo(archiveFile, overwrite = true).let { temporaryArchive.delete() },
            ) {
                "Could not replace Android-test AAR: $archiveFile"
            }
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
            javadocJar = JavadocJar.Empty(),
            sourcesJar = SourcesJar.None(),
            androidVariantsToPublish = listOf("release"),
        ),
    )
    coordinates(group.toString(), "blake3-kmp-android-test", version.toString())
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
        name.set("Blake3 KMP Android Test")
        description.set("Blake3 KMP Android and desktop JVM variants for tests")
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
