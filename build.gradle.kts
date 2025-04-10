plugins {
    kotlin("jvm") version "2.1.10"
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

    // Portals
    implementation("com.github.MMarco94:klib-portal:0.1")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.5")
    implementation("ch.qos.logback:logback-classic:1.5.17")
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
    implementation("com.github.hypfvieh:dbus-java-core:5.1.0")
    implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:5.1.0")
    // Dominant color from image
    implementation("com.github.SvenWoltmann:color-thief-java:v1.1.2")

    val kotest = "5.9.1"
    testImplementation("io.kotest:kotest-runner-junit5:$kotest")
    testImplementation("io.kotest:kotest-assertions-core:$kotest")
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
