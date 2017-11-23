package it.gov.daf.eventConverters

import com.twitter.bijection.Injection
import com.twitter.bijection.avro.SpecificAvroCodecs
import it.gov.daf.iotingestion.event.Event
import it.gov.daf.kafkaProducers.eventConverters.InfoBluEventConverter
import org.scalatest.FunSuite

import scala.util.{ Success, Try }

/**
 * Created by fabiana on 10/04/17.
 */
class InfoBluEventConverterSpec extends FunSuite {
  val specificAvroBinaryInjection: Injection[Event, Array[Byte]] = SpecificAvroCodecs.toBinary[Event]
  val infoBlu = new InfoBluEventConverter()

  test("the first execution should extract some data") {

    val data: Seq[Try[Event]] = infoBlu.convert()._2.getOrElse(Seq()).map { x =>
      specificAvroBinaryInjection.invert(x)
    }
    val Success(head) = data.head
    assert(data.nonEmpty)

    assert(head.ts != -1)
    assert(head.source.length > 1)
    assert(head.body.nonEmpty)
    val stringBody = new String(head.body.get, "UTF-8")

    assert(stringBody.length > 1)
    assert(head.location.split("-").size == 2)

    //check if all elements in attributes("tags") are correctly stored
    head.attributes("tags").split(",").toList.foreach(k => assert(head.attributes.contains(k)))

    println(head)

    //data.flatMap{_.toOption}.foreach(x => println(x.id))

  }

  test { "Running two consecutive times convert method no update should be returned" } {
    val firstRun = infoBlu.convert()
    val secondRun = infoBlu.convert(firstRun._1)
    assert(secondRun._2.isEmpty)
    val map1 = firstRun._1
    val map2 = secondRun._1

    assert(secondRun._2.isEmpty)
    assert(firstRun._2.nonEmpty)
    map1.keys.foreach(k => assert(map1.get(k) != map2.get(k)))

  }

}
