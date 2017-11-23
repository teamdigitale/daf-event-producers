import com.fasterxml.jackson.databind.{ DeserializationFeature, ObjectMapper }
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

/**
 * Created by fabiana on 10/04/17.
 */
package object util {

  object JsonConverter {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    def fromJson[T](json: String)(implicit m: Manifest[T]): T = {

      mapper.registerModule(DefaultScalaModule)
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      mapper.readValue[T](json)
    }

    def toJson[T](value: T): String = {
      mapper.registerModule(DefaultScalaModule)
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      mapper.writeValueAsString(value)
    }
  }

}
