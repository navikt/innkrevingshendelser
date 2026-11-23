package no.nav.utenlandsadresser.innkrevingshendelser

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*

class ApplicationTest : StringSpec({
    "health endpoints respond with status and text" {
        testApplication {
            application { module() }

            client.get("/isalive").apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "Alive"
            }
            client.get("/isready").apply {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "Ready"
            }
        }
    }

    "metrics endpoint responds with JVM metrics in Prometheus format" {
        testApplication {
            application { module() }

            client.get("/metrics").apply {
                status shouldBe HttpStatusCode.OK
                headers["Content-Type"]?.startsWith("text/plain") shouldBe true
                headers["Content-Type"]?.contains("version=0.0.4") shouldBe true
                bodyAsText().contains("jvm_memory_used_bytes") shouldBe true
            }
        }
    }

    "unknown route responds with 404" {
        testApplication {
            application { module() }

            client.get("/unknown").status shouldBe HttpStatusCode.NotFound
        }
    }
})
