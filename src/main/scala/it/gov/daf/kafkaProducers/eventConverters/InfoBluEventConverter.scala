package it.gov.daf.kafkaProducers.eventConverters

import java.net.{ HttpURLConnection, URL }
import java.nio.charset.CodingErrorAction
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

import com.typesafe.config.ConfigFactory
import it.gov.daf.iotingestion.event.Event
import org.slf4j.{ Logger, LoggerFactory }
import util.{ AvroConverter, JsonConverter }

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Await, Future }
import scala.io.{ BufferedSource, Codec }
import scala.util.{ Failure, Success, Try }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by fabiana on 10/04/17.
 */
class InfoBluEventConverter extends EventConverter {
  import InfoBluEventConverter._
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def convert(time: Map[String, Long] = Map()): (Map[String, Long], Option[Seq[Array[Byte]]]) = {

    implicit val codec = Codec("ISO-8859-1")
    codec.onMalformedInput(CodingErrorAction.REPLACE)
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

    val newMap = Map(url -> System.currentTimeMillis())
    val futureJson = Future(httpRequest(url))

    val fData = futureJson.map {
      case Failure(ex) =>
        logger.error(s"Error connection to $url \t ${ex.getMessage}")
        None

      case Success(body) =>
        val res = Try {
          val featureCollection = JsonConverter.fromJson[FeatureCollection](body.mkString)
          val avro = featureCollection
            .features
            .map(convertEvent(_))
            .filter { d =>
              time.get(url) match {
                case None => true
                case Some(t) => d.ts >= t
              }
            }
            .map(AvroConverter.convertEvent)
            .toSeq
          avro match {
            case Nil =>
              logger.debug(s"No datapoint extracted at ${newMap(url)}")
              None
            case _ => Some(avro)
          }
        }
        res match {
          case Success(x) => x
          case Failure(ex) =>
            logger.error(s"ERROR ${ex.getMessage}")
            logger.error(s"ROW data: $body")
            None
        }
    }
    val data = Await.result(fData, FiniteDuration(180, TimeUnit.SECONDS))
    (newMap, data)
  }

  private def httpRequest(url: String): Try[String] = Try {
    val u = new URL(url)
    val con = u.openConnection().asInstanceOf[HttpURLConnection]
    con.setRequestProperty("Accept-Encoding", "gzip")
    val reader = new GZIPInputStream(con.getInputStream)
    scala.io.Source.fromInputStream(reader).getLines().mkString
  }

  private def convertEvent(feature: Feature): Event = {

    val ts = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(feature.properties.start_time).getTime
    val prefixId = s"${this.getClass.getName}."

    val tagList = List("type", "road_direction_name").mkString(",")

    Event(
      id = prefixId + feature.properties.id,
      ts = ts,
      event_type_id = 1,
      location = feature.geometry.coordinates.mkString("-"),
      source = url,
      body = Some(JsonConverter.toJson[Feature](feature).getBytes()),
      event_annotation = Some(feature.properties.description),
      attributes = Map(
        road_direction_name -> feature.properties.road_direction_name,
        type_event -> feature.properties.`type`,
        update_time -> feature.properties.update_time,
        "tags" -> tagList
      )
    )
  }

}

object InfoBluEventConverter {
  case class FeatureCollection(`type`: String, features: Array[Feature], properties: Properties)
  case class Properties(abilitato: Boolean)
  case class Feature(geometry: Geometry, properties: DetailedData)
  case class Geometry(`type`: String, coordinates: Array[Double])

  // be sure that the id is really unique, otherwise the consumer will remove all events having the same ids
  case class DetailedData(description: String, road_direction_name: String, `type`: String, id: String, start_time: String, update_time: String, ico_url: String)
  val url = ConfigFactory.load().getString("daf-event-producers.urls.infobluEvents")
  val description = "description"
  val road_direction_name = "road_direction_name"
  val type_event = "type"
  val id = "id"
  val start_time = "start_type"
  val update_time = "update_time"
}
