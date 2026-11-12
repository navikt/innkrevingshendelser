plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "no.nav.utenlandsadresser"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.micrometer)
    implementation(libs.kafka.clients)
    implementation(libs.micrometer.registry.prometheus)

    runtimeOnly(libs.logback.classic)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.testcontainers.kafka)
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "no.nav.utenlandsadresser.innkrevingshendelser.ApplicationKt"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
