package kotest

import io.kotest.core.extensions.install
import io.kotest.core.spec.Spec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import no.nav.utenlandsadresser.innkrevingshendelser.infrastructure.kafka.KafkaTopic
import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun Spec.installKafka(): KafkaTestClients {
    val imageName = DockerImageName.parse("apache/kafka-native:4.4.0")
    val kafkaContainer = install(TestContainerSpecExtension(KafkaContainer(imageName)))

    val producerProperties =
        Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        }
    val consumerProperties =
        Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.GROUP_ID_CONFIG, "test-group")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }
    val adminProperties =
        Properties().apply {
            put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
        }

    return KafkaTestClients(
        KafkaProducer(producerProperties),
        KafkaConsumer(consumerProperties),
        Admin.create(adminProperties)
    )
}

@OptIn(ExperimentalUuidApi::class)
fun createRandomTopic(admin: Admin): KafkaTopic {
    val topic = Uuid.generateV4().toString()
    admin.createTopics(listOf(NewTopic(topic, 1, 1))).all().get(5, TimeUnit.SECONDS)
    admin.close(5.seconds.toJavaDuration())
    return KafkaTopic(topic)
}

data class KafkaTestClients(
    val producer: KafkaProducer<String, String>,
    val consumer: KafkaConsumer<String, String>,
    val admin: Admin,
)
