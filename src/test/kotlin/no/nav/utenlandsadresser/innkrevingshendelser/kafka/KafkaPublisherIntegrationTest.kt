package no.nav.utenlandsadresser.innkrevingshendelser.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.producer.RecordMetadata
import org.testcontainers.kafka.KafkaContainer

class KafkaPublisherIntegrationTest : FunSpec({
    test("TODO: publishes a record that can be read back; disabled until fixtures are implemented").config(enabled = false) {
        KafkaContainer("apache/kafka-native:3.9.1").use { broker ->
            broker.start()

            val topic = "publisher-roundtrip"
            val key = "synthetic-key"
            val value = "synthetic-value"
            val publisher: KafkaPublisher = TODO("Opprett publisher for testbroker og topic")

            publisher.use {
                val metadata = it.publish(key, value)
                metadata.topic() shouldBe topic

                val records: ConsumerRecords<String, String> =
                    TODO("Lag testkonsument, les fra topic og returner records")
                records.count() shouldBe 1
                records.first().key() shouldBe key
                records.first().value() shouldBe value
            }
        }
    }

    test("TODO: publish failure behavior; disabled until its contract is agreed").config(enabled = false) {
        val failure: Throwable = TODO("Avklar testoppsett og feilkontrakt for publish")
        failure::class shouldBe TODO("Avklar forventet feiltype uten å anta produktkontrakt")
    }

    test("TODO: publish timeout and unknown outcome; disabled until its contract is agreed").config(enabled = false) {
        val result: Result<RecordMetadata> = TODO("Avklar hvordan timeout og ukjent utfall skal testes")
        result shouldBe TODO("Avklar forventet resultat uten å anta produktkontrakt")
    }

    test("TODO: close behavior; disabled until bounded close and ownership are agreed").config(enabled = false) {
        val result: Result<Unit> = TODO("Avklar lukketest, bounded close og eierskap")
        result shouldBe TODO("Avklar forventet close-resultat uten å anta produktkontrakt")
    }
})
