import sbt._
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.config.ConfigFactory

name := "daf-event-producers"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

val assemblyName = "daf-event-producers"

scalariformSettings

scalastyleFailOnError := false

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8", // yes, this is 2 args
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-dead-code",
  "-Xfuture"
)

val kafkaVersion = "0.10.1.1"
val scalaxmlVersion = "1.0.6"
val apacheLog4jVersion = "2.7"
val scalaTestVersion = "3.0.0"
val json4sVersion = "3.5.0"

resolvers ++= Seq(
  Resolver.mavenLocal,
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)

lazy val root = (project in file(".")).
  enablePlugins(JavaAppPackaging, AssemblyPlugin).
  settings(
    libraryDependencies ++= Seq(
      "org.apache.logging.log4j" % "log4j-core" % "2.9.0",
      "org.apache.logging.log4j" % "log4j-api" % "2.9.0",
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.9.0",
      "it.gov.daf" %% "daf-iot-ingestion-manager-common" % "1.0-SNAPSHOT",
      "org.scala-lang.modules" %% "scala-xml" % scalaxmlVersion % "compile",
      //typesafe dependencies
      "com.typesafe" % "config" % "1.3.0",
      //avro dependencies
      "org.apache.avro" % "avro" % "1.8.1",
      "com.twitter" %% "bijection-avro" % "0.9.2",
      "com.twitter" %% "bijection-core" % "0.9.2",
      //influxdb dependencies
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.7",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.7",
      "org.mapdb" % "mapdb" % "3.0.3",
      "org.apache.kafka" %% "kafka" % kafkaVersion % "compile"
        exclude("org.apache.logging.log4j", "log4j-slf4j-impl")
        exclude("org.slf4j", "slf4j-log4j12")
        exclude("com.sun.jdmk", "jmxtools")
        exclude("com.sun.jmx", "jmxri")
        exclude("javax.jms", "jms"),
      kafkaExcludes("org.apache.kafka" %% "kafka" % kafkaVersion % "test" classifier "test"),
      kafkaExcludes("org.apache.kafka" % "kafka-clients" % kafkaVersion % "compile"),
      kafkaExcludes("org.apache.kafka" % "kafka-clients" % kafkaVersion % "test" classifier "test"),
      "org.apache.commons" % "commons-io" % "1.3.2" % "test",

      //Test Dependencies
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
      "org.json4s" %% "json4s-native" % json4sVersion % "test")
      .map(x => x.exclude("org.scalactic", "scalactic"))
  )

val kafkaExcludes = (moduleId: ModuleID) => moduleId.excludeAll(ExclusionRule(organization = "org.json4s"))

//val config = ConfigFactory.load()
//val producerName = config.getString("spark-dataIngestion-example.args.producerName")
//val duration = config.getLong("spark-dataIngestion-example.args.period")
//val nameMainClass = "it.gov.daf.kafkaProducers.main.MainProducer"
//
//packageName in Docker := System.getProperty("docker.name")
mainClass in Compile := Some("it.gov.daf.kafkaProducers.main.MainProducer")

dockerBaseImage := "anapsix/alpine-java:8_jdk_unlimited"
//dockerCommands := dockerCommands.value.flatMap {
//  case cmd@Cmd("FROM", _) => List(cmd,
//    Cmd("RUN", "apk update && apk add bash krb5-libs krb5"),
//    Cmd("RUN", "ln -sf /etc/krb5.conf /opt/jdk/jre/lib/security/krb5.conf")
//  )
//  case other => List(other)
//}
dockerEntrypoint := Seq(s"bin/${name.value}", "-Dconfig.file=conf/production.conf")
dockerCmd := Seq("-jvm-debug", "5005")
dockerRepository := Option("10.98.74.120:5000")

publishTo in ThisBuild := {
  val nexus = "http://nexus.default.svc.cluster.local:8081/repository/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "maven-snapshots/")
  else
    Some("releases" at nexus + "maven-releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")


