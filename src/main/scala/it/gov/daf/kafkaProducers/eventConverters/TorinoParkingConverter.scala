package it.gov.daf.kafkaProducers.eventConverters

import java.text.SimpleDateFormat

import it.gov.daf.iotingestion.event.Event
import org.slf4j.{ Logger, LoggerFactory }
import util.AvroConverter
import com.typesafe.config.ConfigFactory

import scala.collection.immutable.Seq
import scala.util.{ Failure, Success, Try }
import scala.xml.{ NodeSeq, XML }

/**
 * Created by fabiana on 14/03/17.
 */
class TorinoParkingConverter extends EventConverter {

  import TorinoParkingConverter._
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def convert(timeMap: Map[String, Long] = Map()): (Map[String, Long], Option[Seq[Array[Byte]]]) = {

    Try(XML.load(url)) match {
      case Failure(ex) =>
        logger.error(s"Error connection")
        (timeMap, None)
      case Success(xml) =>
        Try {
          val time = timeMap.getOrElse(url, 1L)
          val traffic_data: NodeSeq = xml \\ "traffic_data"
          val pk_data = traffic_data \\ "PK_data"
          val generationTimeString = (traffic_data \\ "@generation_time").text
          val generationTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(generationTimeString).getTime

          if (generationTimestamp > time) {
            val tags = for {
              tag <- pk_data
            } yield convertEvent(tag, generationTimestamp)

            val avro = tags.map(x => AvroConverter.convertEvent(x))
            val newTimeMap = Map(url -> generationTimestamp)

            (newTimeMap, Some(avro))
          } else {
            (timeMap, None)
          }
        } match {
          case Success(x) => x
          case Failure(ex) =>
            logger.error(s"Exception in data ${xml.toString}")
            (timeMap, None)
        }
    }

  }

  private def convertEvent(ftd_data: NodeSeq, generationTimestamp: Long): Event = {

    val freeParking = (ftd_data \ "@Free").text
    val free = freeParking match {
      case "" =>
        "0"
      case s =>
        s
    }

    val name = getOrElse((ftd_data \ "@Name").text)
    val status = getOrElse((ftd_data \ "@status").text)
    val tendence = getOrElse((ftd_data \ "@tendence").text)
    val lat = getOrElse((ftd_data \ "@lat").text)
    val lon = getOrElse((ftd_data \ "@lng").text)
    val latLon = s"$lat-$lon"

    val tagList = List(att_name, att_status, att_tendence).mkString(",")

    val attributes: Map[String, String] = Map(
      att_name -> name,
      att_status -> status,
      att_tendence -> tendence,
      "metric" -> "free",
      "value" -> free,
      "tags" -> tagList
    )

    val id = s"${this.getClass}.$generationTimestamp.${convertString(name)}"

    val point = new Event(
      id = id,
      ts = generationTimestamp,
      event_type_id = 0,
      location = latLon,
      source = url,
      body = Some(ftd_data.toString().getBytes()),
      attributes = attributes
    )
    point

  }

  def convertString(string: String): String = string.replaceAll("[^a-zA-Z0-9]+", "")

  def getOrElse(s: String, default: String = "NAN"): String = {
    if (s == null || s.length == 0)
      default
    else
      s
  }

}
object TorinoParkingConverter {

  val url = ConfigFactory.load().getString("daf-event-producers.urls.torinoParking")
  val host = ConfigFactory.load().getString("daf-event-producers.hosts.torino")

  val att_name = "name"
  val att_status = "status"
  val att_free = "free"
  val att_tendence = "tendence"
  val att_latLon = "coordinate"

  val measure = "opendata.torino.get_pk"

}
