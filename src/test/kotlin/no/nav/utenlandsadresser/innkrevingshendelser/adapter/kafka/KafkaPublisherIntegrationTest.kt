package no.nav.utenlandsadresser.innkrevingshendelser.adapter.kafka

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import kotest.createRandomTopic
import kotest.installKafka
import org.apache.kafka.clients.consumer.ConsumerRecords
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class KafkaPublisherIntegrationTest :
    WordSpec({
        val (producer, consumer, admin) = installKafka()

        "publish" should {
            "send a record that can be read back" {
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
        }
    })
