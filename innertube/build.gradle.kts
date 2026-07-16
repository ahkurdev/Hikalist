plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.encoding)
    implementation(libs.brotli)
    implementation("com.github.MetrolistGroup:MetrolistExtractor:f0a00f5") {
        exclude(group = "com.google.protobuf")
    }
    implementation(libs.slf4j)
    testImplementation(libs.junit)
}
