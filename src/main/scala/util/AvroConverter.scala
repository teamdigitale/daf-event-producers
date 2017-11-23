package util

import com.twitter.bijection.Injection
import com.twitter.bijection.avro.SpecificAvroCodecs
import it.gov.daf.iotingestion.event.Event

/**
 * Created with <3 by Team Digitale.
 * It converts a standard Event into an avro format
 */
object AvroConverter {
  implicit private val specificEventAvroBinaryInjection: Injection[Event, Array[Byte]] = SpecificAvroCodecs.toBinary[Event]
  def convertEvent(event: Event): Array[Byte] = specificEventAvroBinaryInjection(event)

}
