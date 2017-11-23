package it.gov.daf.kafkaProducers

import java.io.Serializable
import java.util.Properties

import it.gov.daf.kafkaProducers.eventConverters.EventConverter
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerRecord }
import org.slf4j.{ Logger, LoggerFactory }

import scala.reflect.{ ClassTag, classTag }
import scala.util.{ Failure, Try }

/**
 * Created with <3 by Team Digitale
 *
 * It sends events to a kafka queue
 */
class KafkaEventProducer[T <: EventConverter: ClassTag](props: Properties, topic: String) {

  val producer = new KafkaProducer[Array[Byte], Array[Byte]](props)
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val converter: T = classTag[T].runtimeClass.newInstance().asInstanceOf[T with Serializable]

  def run(timeMap: Map[String, Long] = Map.empty): Map[String, Long] = {

    val tryRes = converter.convert(timeMap)
    val (newTimeMap, avro) = converter.convert(timeMap)

    avro.getOrElse(Seq.empty[Array[Byte]]).foreach { data =>
      exec(data) match {
        case Failure(ex) => logger.error(s"${ex.getStackTrace}")
        case _ =>
      }
    }

    logger.info(s"Converted ${avro.getOrElse(List()).size} events")

    if (avro.nonEmpty) {
      producer.flush()
    }
    newTimeMap
  }

  def exec(bytes: Array[Byte]): Try[Unit] = Try {
    val message = new ProducerRecord[Array[Byte], Array[Byte]](topic, bytes)
    producer.send(message)
    ()
  }

  def close(): Unit = producer.close()

}
