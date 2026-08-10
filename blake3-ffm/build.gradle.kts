import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.vanniktech.maven.publish.base")
    id("com.jakewharton.test-distribution")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        // FFM (stable) available in Java 22+
        // but also available in Java 21 (preview)
        jvmTarget.set(JvmTarget.JVM_22)
    }
}

// kotlin jvmToolchain auto-set to 25 while the kotlin is changed to 22
// which makes incompatible java code and kotlin code
tasks.withType<JavaCompile>().configureEach {
    options.release.set(22)
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.named<Jar>("jarTest") {
    from(sourceSets["test"].output)
}

mavenPublishing {
    configure(
        KotlinJvm(
            // configures the - Javadoc artifact, possible values:
            // - `JavadocJar.None()` don't publish this artifact
            // - `JavadocJar.Empty()` publish an empty jar
            // - `JavadocJar.Javadoc()` to publish standard Javadoc
            // - `JavadocJar.Dokka("dokkaHtml")` when using Kotlin with Dokka, where `dokkaHtml` is the name of the Dokka task that should be used as input
            javadocJar = JavadocJar.Javadoc(),
            // configures the -sources artifact, possible values:
            // - `SourcesJar.None()` don't publish this artifact
            // - `SourcesJar.Empty()` publish an empty jar
            // - `SourcesJar.Sources()` publish the sources
            sourcesJar = SourcesJar.None(),
        ),
    )
    coordinates(group.toString(), "blake3-ffm", version.toString())
    pom {
        name.set("Blake3 FFM")
        description.set("Blake3 ffm for jvm 22+")
        inceptionYear.set("2026")
        url.set("https://github.com/akhsaul/blake3-kmp/")
        licenses {
            license {
                // name.set("The Apache License, Version 2.0")
                // url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                // distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
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

dependencies {
    testImplementation(kotlin("test"))
}
repositories {
    mavenCentral()
}
