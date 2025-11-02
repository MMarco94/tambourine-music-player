import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("jvm") version "2.2.21"
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.github.ben-manes.versions") version "0.53.0"
}

group = "io.github.mmarco94"
version = "1.1.2"
val debugBuild = false

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.foundation)
    implementation(compose.desktop.currentOs)
    api(compose.materialIconsExtended)
    api(compose.material3)
    val coroutine = "1.10.2"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutine")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutine")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$coroutine")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

    // Portals
    implementation("com.github.MMarco94:klib-portal:0.1")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.20")
    implementation("org.slf4j:jul-to-slf4j:2.0.17")

    // ffmpeg-based audio decoder
    // Artifacts:
    // - ffsampledsp-complete
    // - ffsampledsp-x86_64-macos
    // - ffsampledsp-aarch64-macos
    // - ffsampledsp-x86_64-linux
    // - ffsampledsp-i386-win
    // - ffsampledsp-x86_64-win
    implementation("com.tagtraum:ffsampledsp-complete:0.9.53")
    // music metadata reader
    implementation("net.jthink:jaudiotagger:3.0.1")
    // DBUS APIs
    implementation("com.github.hypfvieh:dbus-java-core:5.1.1")
    implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:5.1.1")
    // Dominant color from image
    implementation("com.github.SvenWoltmann:color-thief-java:1.1.2")

    val kotest = "6.0.4"
    testImplementation("io.kotest:kotest-runner-junit5:$kotest")
    testImplementation("io.kotest:kotest-assertions-core:$kotest")
}

tasks.named<KotlinCompilationTask<*>>("compileKotlin").configure {
    compilerOptions.optIn.add("kotlin.time.ExperimentalTime")
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "io.github.mmarco94.tambourine.MainKt"
        if (debugBuild) {
            jvmArgs += listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
        }
        nativeDistributions {
            packageName = "tambourine"
            packageVersion = version.toString()

            modules("java.naming", "jdk.security.auth", "jdk.unsupported")
            if (debugBuild) {
                modules.add("jdk.jdwp.agent")
            }
        }
        buildTypes.release.proguard {
            version.set("7.6.0")
            optimize = false
            configurationFiles.from(project.file("compose-desktop.pro"))
        }
    }
}

afterEvaluate {
    tasks.named("createReleaseDistributable") {
        dependsOn(tasks.test)
    }
}
