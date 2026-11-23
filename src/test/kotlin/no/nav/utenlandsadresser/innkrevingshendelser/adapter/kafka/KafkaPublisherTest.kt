package no.nav.utenlandsadresser.innkrevingshendelser.adapter.kafka

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.producer.MockProducer
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class KafkaPublisherTest :
    WordSpec({
        "close" should {
            "wait at most the configured close timeout" {
                val producer = ClosingTimeoutRecordingProducer()
                val publisher = KafkaPublisher(producer, KafkaTopic("synthetic-topic"), closeTimeout = 3.seconds)

                publisher.close()

                producer.closeTimeouts shouldBe listOf(3.seconds.toJavaDuration())
                producer.closed() shouldBe true
            }
        }
    })

private class ClosingTimeoutRecordingProducer : MockProducer<String, String>() {
    val closeTimeouts = mutableListOf<java.time.Duration>()

    override fun close(timeout: java.time.Duration) {
        closeTimeouts += timeout
        super.close(timeout)
    }
}
