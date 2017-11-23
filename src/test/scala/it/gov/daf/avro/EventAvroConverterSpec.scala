package it.gov.daf.avro

import it.gov.daf.iotingestion.event.Event
import org.json4s._
import org.json4s.native.JsonMethods._
import org.scalatest.FunSuite
import util.AvroConverter

/**
 * Created by Team Digitale.
 */
class EventAvroConverterSpec extends FunSuite {

  test("An event should be correctly serialized and deserialized") {

    val data =
      """{"version": 1,
        |"id": "1500547661573_0",
        |"ts": 1500547620000,
        |"temporal_granularity": null,
        |"event_certainty": 1.0,
        |"event_type_id": 0,
        |"event_annotation": null,
        |"source": "http://opendata.5t.torino.it/get_fdt",
        |"location": "45.06766-7.66662",
        |"body": {"bytes": "<FDT_data period=\"5\" accuracy=\"100\" lng=\"7.66662\" lat=\"45.06766\" direction=\"positive\" offset=\"55\" Road_name=\"Corso Vinzaglio(TO)\" Road_LCD=\"40201\" lcd1=\"40202\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.5t.torino.it/simone/ns/traffic_data\">\n    <speedflow speed=\"21.51\" flow=\"240.00\"/>\n  </FDT_data>"},
        |"attributes": {"period": "5", "offset": "55", "metric": "flow", "Road_name": "Corso Vinzaglio(TO)", "Road_LCD": "40201", "accuracy": "100", "FDT_data": "40202", "value": "240.00", "tags": "FDT_data,Road_LCD,Road_name,offset,direction,accuracy,period", "direction": "positive"}}""".stripMargin

    implicit val formats = DefaultFormats

    val event = parse(data, true).extract[Event]
    val avro = AvroConverter.convertEvent(event)

    assert(event.id == "1500547661573_0")
    assert(avro.length > 0)
  }

}
