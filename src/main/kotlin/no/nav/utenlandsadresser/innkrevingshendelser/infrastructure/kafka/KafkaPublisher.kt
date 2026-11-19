package no.nav.utenlandsadresser.innkrevingshendelser.infrastructure.kafka

import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class KafkaPublisher(
    private val producer: Producer<String, String>,
    private val topic: KafkaTopic,
) : AutoCloseable by producer {
    suspend fun publish(
        key: KafkaKey,
        value: KafkaValue,
    ): RecordMetadata =
        suspendCancellableCoroutine { continuation ->
            val record = ProducerRecord(topic.value, key.value, value.value)
            val onCompletion =
                Callback { metadata, exception ->
                    if (exception != null) {
                        continuation.resumeWithException(exception)
                    } else {
                        continuation.resume(metadata)
                    }
                }

            val future = producer.send(record, onCompletion)

            continuation.invokeOnCancellation {
                future.cancel(true)
            }
        }
}

@JvmInline
value class KafkaTopic(
    val value: String,
)

@JvmInline
value class KafkaKey(
    val value: String,
)

@JvmInline
value class KafkaValue(
    val value: String,
)
