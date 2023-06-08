import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "com.example"
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

                // ffmpeg-based audio decoder
                implementation("com.tagtraum:ffsampledsp-complete:0.9.50")
                // music metadata reader
                implementation("net.jthink:jaudiotagger:3.0.1")


                // File picker
                implementation("com.darkrockstudios:mpfilepicker:1.1.0")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "music-player"
            packageVersion = "1.0.0"
        }
    }
}
