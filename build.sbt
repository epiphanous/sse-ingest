lazy val akkaVersion = "2.5.20"
lazy val akkaHttpVersion = "10.1.7"

lazy val akkaDeps = Seq(
  "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion
)

lazy val alpakkaDeps = Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-sse"     % "1.0-M1",
  "com.lightbend.akka" %% "akka-stream-alpakka-kinesis" % "1.0-M2",
  "com.typesafe.akka"  %% "akka-stream-kafka"           % "1.0-RC1"
)

lazy val kafkaDeps = Seq(
  "org.apache.kafka" % "kafka-clients" % "2.1.0"
)

lazy val kinesisDeps = Seq(
  "com.amazonaws" % "aws-java-sdk-kinesis" % "1.11.476"
)

lazy val jsonDeps = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-parser"
).map(a ⇒ a % "0.11.1")

lazy val loggingDeps = Seq(
  "ch.qos.logback"             % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.2"
)

lazy val testDeps = Seq(
  "com.typesafe.akka" %% "akka-http-testkit"   % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-testkit"        % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "org.scalatest"     %% "scalatest"           % "3.0.1"
).map(m ⇒ m % Test)

lazy val sse_ingest = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "io.epiphanous",
      scalaVersion := "2.12.8"
    )
  ),
  name := "sse-ingest",
  libraryDependencies ++= akkaDeps ++ alpakkaDeps ++ kafkaDeps ++ kinesisDeps ++ jsonDeps ++ loggingDeps ++ testDeps
)
