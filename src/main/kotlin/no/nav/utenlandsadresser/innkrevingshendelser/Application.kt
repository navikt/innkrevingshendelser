package no.nav.utenlandsadresser.innkrevingshendelser

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.utenlandsadresser.innkrevingshendelser.Application")

fun main() {
    val server = embeddedServer(Netty, port = 8080) {
        module()
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Stopping application")
        server.stop(gracePeriodMillis = 5_000, timeoutMillis = 10_000)
    })

    logger.info("Starting application")
    server.start(wait = true)
}

fun Application.module() {
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val jvmGcMetrics = JvmGcMetrics()
    listOf(
        ClassLoaderMetrics(),
        jvmGcMetrics,
        JvmMemoryMetrics(),
        JvmThreadMetrics(),
        ProcessorMetrics(),
    ).forEach { it.bindTo(meterRegistry) }

    install(MicrometerMetrics) {
        registry = meterRegistry
    }

    monitor.subscribe(ApplicationStopped) {
        jvmGcMetrics.close()
        meterRegistry.close()
    }

    routing {
        get("/isalive") {
            call.respondText("Alive")
        }
        get("/isready") {
            call.respondText("Ready")
        }
        get("/metrics") {
            call.respondText(
                text = meterRegistry.scrape(),
                contentType = ContentType.parse("text/plain; version=0.0.4; charset=utf-8"),
            )
        }
    }
}
