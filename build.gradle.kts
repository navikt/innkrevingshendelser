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
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.coroutines.core)

    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.logstash.logback.encoder)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.extensions.testcontainers)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.testcontainers.kafka)

    // Overstyrer sårbare transitive versjoner funnet av Trivy.
    // Fjern når kafka-clients og logstash-logback-encoder henter inn rettede versjoner selv.
    runtimeOnly(platform(libs.jackson.bom))
    constraints {
        implementation(libs.lz4.java) {
            because("CVE-2026-106451 i 1.11.2 fra kafka-clients 4.4.0")
        }
        implementation(libs.snappy.java) {
            because("CVE-2026-108106 og CVE-2026-93451 i 1.1.10.7 fra kafka-clients 4.4.0")
        }
    }
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass = "no.nav.utenlandsadresser.innkrevingshendelser.ApplicationKt"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
