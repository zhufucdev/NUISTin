import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

group = "com.zhufucdev"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
    sourceSets {
        val ktorVersion = "2.1.2"
        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation("org.apache.commons:commons-lang3:3.12.0")
                implementation("com.github.tkuenneth:nativeparameterstoreaccess:0.1.2")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            modules("jdk.charsets")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            packageName = "NUISTin"
            packageVersion = "1.0.0"
            description = "南信大校园网自动化登录"
            copyright = "©2022 zhufucdev. All rights reserved."
            vendor = "zhufucdev"

            macOS {
                iconFile.set(project.file("icon/macos.icns"))
            }
            windows {
                iconFile.set(project.file("icon/windows.ico"))
            }
            linux {
                iconFile.set(project.file("icon/linux.svg"))
            }
        }
    }
}
