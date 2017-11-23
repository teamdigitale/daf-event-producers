package it.gov.daf.kafkaProducers.eventConverters

import scala.util.Try

/**
 * Created with <3 by Team Digitale.
 */
trait EventConverter {
  def convert(time: Map[String, Long]): (Map[String, Long], Option[Seq[Array[Byte]]])
}
