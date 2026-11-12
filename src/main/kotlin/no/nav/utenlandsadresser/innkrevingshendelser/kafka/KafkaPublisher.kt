package no.nav.utenlandsadresser.innkrevingshendelser.kafka

import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.RecordMetadata

class KafkaPublisher(
    private val producer: Producer<String, String>,
    private val topic: String,
) : AutoCloseable {
    fun publish(key: String, value: String): RecordMetadata =
        TODO("Sett acks=all og enable.idempotence=true. Send med avgrenset timeout; timeout kan gi ukjent utfall. Ikke logg secrets eller key/value.")

    override fun close(): Unit =
        TODO("Lukk med avgrenset timeout, og avklar at denne klassen eier produsenten.")

    companion object {
        fun fromEnvironment(environment: Map<String, String>): KafkaPublisher =
            TODO("Les Nais TLS-innstillinger fra miljøet. Ikke logg secrets eller key/value, og ikke slå av TLS hostname verification.")
    }
}
