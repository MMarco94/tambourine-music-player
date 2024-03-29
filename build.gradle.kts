plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "io.github.mmarco94"
version = "1.0.1"

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
                implementation(compose.foundation)
                implementation(compose.desktop.currentOs)
                api(compose.materialIconsExtended)
                api(compose.material3)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

                // Portals
                implementation("com.github.MMarco94:klib-portal:0.1")

                // Logging
                implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
                implementation("ch.qos.logback:logback-classic:1.4.14")
                implementation("org.slf4j:jul-to-slf4j:2.0.10")

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
                // FFT
                implementation("com.tambapps.fft4j:fft4j:2.0")
                // DBUS APIs
                implementation("com.github.hypfvieh:dbus-java-core:4.3.1")
                implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:4.3.1")
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
            packageName = "tambourine"
            packageVersion = version.toString()

            modules("java.naming", "jdk.security.auth", "jdk.unsupported")
        }
        buildTypes.release.proguard {
            configurationFiles.from(project.file("compose-desktop.pro"))
        }
    }
}
