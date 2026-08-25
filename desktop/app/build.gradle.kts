plugins {
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.javafx)
    alias(libs.plugins.kotlin.serialization)
    kotlin("jvm")
}

javafx {
    version = "21.0.6"
    modules = mutableListOf("javafx.media", "javafx.base", "javafx.graphics", "javafx.swing")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Project library modules (pure JVM)
    implementation(project(":innertube"))
    implementation(project(":kugou"))
    implementation(project(":lrclib"))
    implementation(project(":lastfm"))
    implementation(project(":shazamkit"))
    implementation(project(":betterlyrics"))
    implementation(project(":paxsenix"))

    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // DI
    implementation(libs.koin.core)
    implementation(libs.koin.compose)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.sqlite.jdbc)

    // Audio — JavaFX (download & play with proper auth)
    // JavaFX plugin handles the runtime deps

    // HTTP (same stack as Android)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.serialization.json)

    // Supabase sync (publishable client only; no service-role secrets)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)

    // Image loading
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    // Theme
    implementation(libs.materialKolor)

    // JSON utils
    implementation(libs.json)

    // Logging
    implementation(libs.slf4j)
    implementation(libs.logback.classic)

    // Windows system media overlay and global media-key callbacks.
    implementation("io.github.selemba1000:JavaMediaTransportControls:0.0.3")

    // Testing
    testImplementation(libs.junit)
}

compose.desktop {
    application {
        mainClass = "com.metrolist.desktop.MainKt"

        // JavaFX jars are on classpath via Maven deps, no --add-modules needed for classpath mode

        nativeDistributions {
            modules(
                "java.instrument",
                "java.management",
                "java.naming",
                "java.net.http",
                "java.prefs",
                "java.sql",
                "jdk.httpserver",
                "jdk.security.auth",
                "jdk.unsupported",
            )
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "Hikalist"
            packageVersion = "1.0.2"
            description = "A calm YouTube Music client for Windows"
            vendor = "Allan4u"

            windows {
                upgradeUuid = "3a8e4f2c-1d6b-4a9e-8c7f-5d2e3a1b0c4d"
                menuGroup = "Hikalist"
                dirChooser = true
                shortcut = true
                perUserInstall = true
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }

            }
    }
}

// Audio playback requires ffmpeg.exe in src/main/resources/
// Download from: https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip
