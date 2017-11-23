package it.gov.daf.kafkaProducers.eventConverters

import java.net.{ HttpURLConnection, URL }
import java.text.SimpleDateFormat
import java.util.zip.GZIPInputStream

import com.typesafe.config.ConfigFactory
import it.gov.daf.iotingestion.event.Event
import org.slf4j.{ Logger, LoggerFactory }
import util.{ AvroConverter, InfoBluDecoder }

import scala.util.{ Failure, Success, Try }
import scala.xml.{ NodeSeq, XML }

/**
 * Created by fabiana on 20/04/17.
 */
class InfoBluTrafficConverter extends EventConverter {
  import InfoBluTrafficConverter._
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val (srcMap, dstMap) = InfoBluDecoder.run()

  override def convert(time: Map[String, Long] = Map()): (Map[String, Long], Option[Seq[Array[Byte]]]) = {
    Try(XML.load(url)) match {

      case Failure(ex) =>
        logger.error(s"Error connection")
        (time, None)

      case Success(xml) =>
        Try {

          val traffic_data: NodeSeq = xml \\ "TRAFFIC"
          val generationTime = getOrElse((traffic_data \\ "TIMESTAMP").text, System.currentTimeMillis.toString)

          val ts = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(generationTime).getTime
          val oldTime = time.getOrElse(url, -1L)

          if (oldTime < ts) {
            val data = traffic_data \\ "SP"

            val dp = data.indices.map(i => convertDataPoint(i, data(i), ts))

            val avro = dp.map(x => AvroConverter.convertEvent(x))
            (Map(url -> ts), Some(avro))
          } else {
            (time, None)
          }

        } match {
          case Failure(ex) =>
            logger.error(s"Exception during conversion step for data ${xml.toString}")
            (time, None)
          case Success(x) => x
        }

    }
  }

  private def convertDataPoint(index: Int, n: NodeSeq, generationTimestamp: Long): Event = {
    val source = getOrElse((n \ sourceCode).text)
    val end = getOrElse((n \ endCode).text)
    val offs = getOrElse((n \ offset).text)
    val speed = getOrElse((n \ velocity).text)

    val tagList = List("sourceCode", "endCode", "offset").mkString(",")

    val attributes = Map(
      "sourceCode" -> source,
      "endCode" -> end,
      "offset" -> offs,
      "srcCoordinates" -> srcMap.getOrDefault(source, "-1"),
      "dstCoordinates" -> dstMap.getOrDefault(end, "-1"),
      "metric" -> "speed",
      "value" -> speed,
      "tags" -> tagList
    )
    //val values = Map("speed" -> speed.toDouble)
    val latLon = srcMap.getOrDefault(source, "")

    val id = s"${this.getClass.getName}.$generationTimestamp.$index"

    val event = new Event(
      id = id,
      ts = generationTimestamp,
      event_type_id = 0,
      location = latLon,
      source = url,
      body = Some(n.toString().getBytes()),
      attributes = attributes
    )
    event
  }

  private def getOrElse(s: String, default: String = "NAN"): String = {
    if (s == null || s.length == 0)
      default
    else
      s
  }

}

object InfoBluTrafficConverter {
  val url = ConfigFactory.load().getString("daf-event-producers.urls.infobluTraffic")
  val sourceCode = "@S"
  val endCode = "@E"
  val offset = "@P"
  val velocity = "@V"

  val measure = "opendata.infoblu.speed"
}