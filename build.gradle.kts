import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "io.github.mmarco94"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                api(compose.materialIconsExtended)
                api(compose.material3)
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

                // File picker
                implementation("com.darkrockstudios:mpfilepicker:1.1.0")

                // Logging
                implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
                implementation("ch.qos.logback:logback-classic:1.4.7")
                implementation("org.slf4j:jul-to-slf4j:2.0.7")

                // ffmpeg-based audio decoder
                implementation("com.tagtraum:ffsampledsp-complete:0.9.50")
                // music metadata reader
                implementation("net.jthink:jaudiotagger:3.0.1")
                // FFT
                implementation("com.tambapps.fft4j:fft4j:2.0")
                // DBUS APIs
                implementation("com.github.hypfvieh:dbus-java-core:4.3.0")
                implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:4.3.0")
                // Dominant color from image
                implementation("com.github.SvenWoltmann:color-thief-java:v1.1.2")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "io.github.mmarco94.tambourine.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Rpm)
            packageName = "tambourine"
            packageVersion = "1.0.0"
        }
    }
}
