package it.gov.daf.kafkaProducers.eventConverters

import java.nio.ByteBuffer
import java.text.SimpleDateFormat

import it.gov.daf.iotingestion.event.Event
import org.slf4j.{ Logger, LoggerFactory }
import util.AvroConverter
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.util.{ Failure, Success, Try }
import scala.xml.{ NodeSeq, XML }

/**
 * Created with <3 by Team Digitale.
 */
class TorinoTrafficConverter extends EventConverter {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  import TorinoTrafficConverter._

  def convert(timeMap: Map[String, Long] = Map()): (Map[String, Long], Option[Seq[Array[Byte]]]) = {
    val time = timeMap.getOrElse(url, -1L)

    Try(XML.load(url)) match {
      case Failure(ex) =>
        logger.error(s"Connection exception")
        (timeMap, None)
      case Success(xml) =>
        Try {

          val traffic_data: NodeSeq = xml \\ "traffic_data"
          val ftd_data = traffic_data \\ "FDT_data"
          val generationTimeString = (traffic_data \\ "@generation_time").text
          val generationTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(generationTimeString).getTime

          if (generationTimestamp > time) {
            val tags = for {
              tag <- ftd_data
            } yield convertEvent(tag, generationTimestamp)

            val avro = tags.flatten.map(x => AvroConverter.convertEvent(x))
            //avro.foreach(println(_))
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

  private def convertEvent(ftd_data: NodeSeq, generationTimestamp: Long): List[Event] = {

    val lcd1 = (ftd_data \ "@lcd1").text
    val road_LCD = (ftd_data \ "@Road_LCD").text
    val road_name = (ftd_data \ "@Road_name").text
    val offset = (ftd_data \ "@offset").text
    val lat = (ftd_data \ "@lat").text
    val lon = (ftd_data \ "@lng").text
    val latLon = s"$lat-$lon"
    val direction = (ftd_data \ "@direction").text
    val accuracy = (ftd_data \ "@accuracy").text
    val period = (ftd_data \ "@period").text
    val flow = (ftd_data \\ "speedflow" \ "@flow").text
    val speed = (ftd_data \\ "speedflow" \ "@speed").text

    val tagList = List(att_lcd1, att_road_LCD, att_road_name, att_offset, att_direction, att_accuracy, att_period).mkString(",")

    val attributes: Map[String, String] = Map(
      att_lcd1 -> lcd1,
      att_road_LCD -> road_LCD,
      att_road_name -> road_name,
      att_offset -> offset,
      att_direction -> direction,
      att_accuracy -> accuracy,
      att_period -> period,
      "tags" -> tagList
    )

    val idFlow = s"${TorinoTrafficConverter.getClass.getName}.Flow.$generationTimestamp.$road_LCD"
    val idSpeed = s"${TorinoTrafficConverter.getClass.getName}.Speed.$generationTimestamp.$road_LCD"

    val eventFlow = new Event(
      id = idFlow,
      ts = generationTimestamp,
      event_type_id = 0,
      location = latLon,
      source = url,
      body = Some(ftd_data.toString().getBytes()),
      attributes = attributes ++ Map("metric" -> "flow", "value" -> flow)
    )

    val eventSpeed = new Event(
      id = idSpeed,
      ts = generationTimestamp,
      event_type_id = 0,
      location = latLon,
      source = url,
      body = Some(ftd_data.toString().getBytes()),
      attributes = attributes ++ Map("metric" -> "speed", "value" -> speed)
    )

    List(eventFlow, eventSpeed)
  }

}

object TorinoTrafficConverter {

  val att_lcd1 = "FDT_data"
  val att_road_LCD = "Road_LCD"
  val att_road_name = "Road_name"
  val att_offset = "offset"
  val att_direction = "direction"
  val att_accuracy = "accuracy"
  val att_period = "period"
  val att_flow = "flow"
  val att_speed = "speed"
  val measure = "opendata.torino.get_fdt"
  val url = ConfigFactory.load().getString("daf-event-producers.urls.torinoTraffic")
  val host = ConfigFactory.load().getString("daf-event-producers.hosts.torino")

}
