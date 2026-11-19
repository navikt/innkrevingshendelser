package no.nav.utenlandsadresser.innkrevingshendelser.kafka

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotest.createRandomTopic
import kotest.installKafka
import no.nav.utenlandsadresser.innkrevingshendelser.infrastructure.kafka.KafkaKey
import no.nav.utenlandsadresser.innkrevingshendelser.infrastructure.kafka.KafkaPublisher
import no.nav.utenlandsadresser.innkrevingshendelser.infrastructure.kafka.KafkaValue
import org.apache.kafka.clients.consumer.ConsumerRecords
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class KafkaPublisherIntegrationTest :
    StringSpec({
        val (producer, consumer, admin) = installKafka()

        "publishes a record that can be read back" {
            val topic = createRandomTopic(admin)
            val key = KafkaKey("synthetic-key")
            val value = KafkaValue("synthetic-value")

            val publisher = KafkaPublisher(producer, topic)

            publisher.use {
                val metadata = it.publish(key, value)
                metadata.topic() shouldBe topic.value
            }
            consumer.use {
                it.subscribe(listOf(topic.value))
                val records: ConsumerRecords<String, String> = it.poll(3.seconds.toJavaDuration())
                records.count() shouldBe 1
                records.first().key() shouldBe key.value
                records.first().value() shouldBe value.value
            }
        }
    })
