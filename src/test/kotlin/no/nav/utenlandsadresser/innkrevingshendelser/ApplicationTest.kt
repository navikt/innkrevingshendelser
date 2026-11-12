package no.nav.utenlandsadresser.innkrevingshendelser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication

class ApplicationTest : FunSpec({
    test("health-endepunktene svarer med status og tekst") {
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

    test("metrics-endepunktet svarer med JVM-metrikker i Prometheus-format") {
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

    test("ukjent rute svarer med 404") {
        testApplication {
            application { module() }

            client.get("/unknown").status shouldBe HttpStatusCode.NotFound
        }
    }
})
