package util

import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentMap

import org.mapdb.{ DB, DBMaker, Serializer }
import org.slf4j.{ Logger, LoggerFactory }

/**
 * Created by fabiana on 20/04/17.
 */
object InfoBluDecoder {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val file = getClass.getResourceAsStream("/teamdigitale-Coordinate.csv")
  val srcFile = "./teamdigitale-Coordinate-Source.db"
  val dstFile = "./teamdigitale-Coordinate-End.db"

  val (mapSource, dbSource) = getMap(srcFile)
  val (mapEnd, dbEnd) = getMap(dstFile)

  def getMap(file: String): (ConcurrentMap[String, String], DB) = {

    val f = new File(file)
    if (f.exists())
      f.delete()

    val db: DB = DBMaker
      .fileDB(file)
      .fileMmapEnable()
      .make()

    val map: ConcurrentMap[String, String] = db
      .hashMap("map", Serializer.STRING, Serializer.STRING)
      .createOrOpen()
    (map, db)
  }

  def closeAll(): Unit = {
    dbSource.close()
    dbEnd.close()
  }

  def run(): (ConcurrentMap[String, String], ConcurrentMap[String, String]) = {

    //remove headers
    val lines = scala.io.Source.fromInputStream(file).getLines().drop(3)
    lines.foreach { l =>
      val tokens = l.split(";")
      val sourceKey = tokens(1)
      val sourceLat = tokens(2).replace(",", ".").toDouble
      val sourceLon = tokens(3).replace(",", ".").toDouble
      val sourceLatLon = s"$sourceLat-$sourceLon"

      val endKey = tokens(4)
      val endLat = tokens(5).replace(",", ".").toDouble
      val endLon = tokens(6).replace(",", ".").toDouble
      val endLatLon = s"$endLat-$endLon"
      mapSource.put(sourceKey, sourceLatLon)
      mapEnd.put(endKey, endLatLon)
    }

    //dbSource.close()
    //dbEnd.close()
    println(s"DBs generated into \n \t $srcFile \n \t $dstFile")
    (mapSource, mapEnd)
  }

  def main(args: Array[String]): Unit = {
    val (a, b) = run()
  }
}
