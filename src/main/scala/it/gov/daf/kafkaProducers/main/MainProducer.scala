package it.gov.daf.kafkaProducers.main

import java.util.Properties
import java.util.concurrent.{ Executors, ScheduledFuture, TimeUnit }

import com.typesafe.config.ConfigFactory
import it.gov.daf.kafkaProducers.KafkaEventProducer
import it.gov.daf.kafkaProducers.eventConverters._
import kafka.utils.{ ZKStringSerializer, ZkUtils }
import kafka.admin.AdminUtils
import org.I0Itec.zkclient.{ ZkClient, ZkConnection }
import org.apache.kafka.clients.producer.ProducerConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.reflect.{ ClassTag, classTag }
import scala.util.{ Failure, Success, Try }

/**
 * Created by fabiana on 30/03/17.
 */
object MainProducer {
  val logger = LoggerFactory.getLogger(this.getClass)

  var config = ConfigFactory.load()
  val serializer = config.getString("daf-event-producers.kafka.serializer")
  val brokers = config.getString("daf-event-producers.kafka.bootstrapServers")
  val topic = config.getString("daf-event-producers.kafka.topic")
  val zookeepers = config.getString("daf-event-producers.kafka.zookeeperServers")

  val props: Properties = new Properties()

  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, serializer)
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serializer)
  //props.put("enable.auto.commit", "true")
  //props.put("enable.zookeeper", "true")
  //props.put("zookeeper.connect", zookeepers)
  //props.put("auto.offset.reset", "earliest")

  logger.info(s"Kafka Bootstrap Servers $brokers, topic $topic")

  val ex = Executors.newScheduledThreadPool(1)
  var lastGeneratedTimeMap: Option[Map[String, Long]] = None

  def main(args: Array[String]): Unit = {

    val producerType = ConfigFactory.load().getString("daf-event-producers.args.producerName")
    val period = ConfigFactory.load().getLong("daf-event-producers.args.period")

    val producer = Producer.withName(producerType)
    val kafkaEventProducer: KafkaEventProducer[_ <: EventConverter] = producer match {
      case Producer.TorinoParking => new KafkaEventProducer[TorinoParkingConverter](props, topic)
      case Producer.TorinoTraffic => new KafkaEventProducer[TorinoTrafficConverter](props, topic)
      case Producer.InfoBluEvent => new KafkaEventProducer[InfoBluEventConverter](props, topic)
      case Producer.InfoBluTraffic => new KafkaEventProducer[InfoBluTrafficConverter](props, topic)
    }

    val timeStamp = System.currentTimeMillis()
    val thread = run(period.toLong, kafkaEventProducer)
    logger.info(s"Execution at $timeStamp isFinished: ${thread.isDone}")

  }

  def run[T <: EventConverter](period: Long, kafkaEventClient: KafkaEventProducer[T]): ScheduledFuture[_] = {
    val task = new Runnable {
      def run(): Unit = {

        if (!KafkaIsAlive) {
          System.err.println("Kafka server not available")
          System.exit(1)

        }

        //        val times = lastGeneratedTimeMap match {
        //          case None =>
        //            kafkaEventClient.run()
        //          case Some(t) =>
        //            kafkaEventClient.run(t)
        //        }

        val times = lastGeneratedTimeMap match {
          case None =>
            val res = Try(kafkaEventClient.run())
            res match {
              case Success(d) => d
              case Failure(ex) =>
                logger.error(
                  s"""unhandled exception.
                     | ERROR message: ${ex.getMessage}
                     | ERROR stackTrace: ${ex.getStackTrace.mkString("\n")}
                     | Print last extracted information ${lastGeneratedTimeMap}""".stripMargin
                )
                Map.empty[String, Long]
            }
          case Some(t) =>
            kafkaEventClient.run(t)
        }

        times.keys.foreach(url => logger.info(s"Data analyzed for the url $url at time ${times(url)}"))

        lastGeneratedTimeMap = Some(times)
      }
    }
    ex.scheduleAtFixedRate(task, 0, period, TimeUnit.SECONDS)
  }

  private def getMap(resutlRun: Try[Map[String, Long]]) = {

  }

  private def KafkaIsAlive: Boolean = {
    val sessionTimeOutInMs = 15 * 1000
    val connectionTimeOutInMs = 15 * 1000
    Try {
      val zkClient = ZkUtils.createZkClient(zookeepers, sessionTimeOutInMs, connectionTimeOutInMs)
      val zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeepers), false)
      (zkClient, zkUtils)
    } match {
      case Success((zkClient, zkUtils)) =>
        val brokers = zkUtils.getAllBrokersInCluster()
        brokers.nonEmpty
      case Failure(ex) =>
        logger.info(ex.getMessage)
        false
    }

  }
}

object Producer extends Enumeration {
  type Producer = Value
  val TorinoTraffic, TorinoParking, InfoBluEvent, InfoBluTraffic = Value
}

