package io.epiphanous.sse2dlog

import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import akka.stream.alpakka.sse.scaladsl.EventSource
import akka.{Done, NotUsed}
import akka.kafka.scaladsl.Producer
import akka.stream.alpakka.kinesis.KinesisFlowSettings
import akka.stream.alpakka.kinesis.scaladsl.KinesisFlow
import akka.stream.scaladsl.Source
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClientBuilder
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import io.circe.parser._
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._

/**
 * Simple akka flow to read from an event source and write to a distributed log like kafka or kinesis
 */
object Ingest extends LazyLogging {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer.create(system)
    implicit val config: Config = system.settings.config

    val url = config.getString("event.source.url")
    val send: HttpRequest ⇒ Future[HttpResponse] = { req: HttpRequest ⇒
      {
        logger.debug(s"opening sse stream $url")
        Http().singleRequest(req)
      }
    }

    val sha1 = java.security.MessageDigest.getInstance("SHA-1")

    val idField = config.getString("event.source.payload.id")
    val payloadType = config.getString("event.source.payload.type")

    // Read in the event source, filter out just the data events and extract the id and the payload
    val eventSource: Source[(String, String), NotUsed] =
      EventSource(
        uri = Uri(url),
        send
      ).filter(_.eventType.contains(payloadType))
        .map(e ⇒ {
          val data = e.data
          val id = parse(data)
            .flatMap(json ⇒ json.hcursor.downField(idField).as[String])
            .getOrElse(sha1.digest(data.getBytes("UTF-8")).map("%02x".format(_)).mkString)
          logger.debug(s"$id = $data")
          (id, data)
        })

    logger.info(BuildInfo.toString)
    logger.info(s"event source $url")

    val events = config.getString("event.sink.type") match {
      case "kafka" ⇒ toKafka(eventSource)
      case "kinesis" ⇒ toKinesis(eventSource)
    }

    implicit val ec: ExecutionContextExecutor = system.dispatcher
    events.onComplete(_ ⇒ system.terminate())
  }

  /**
   * Sends a stream of events ((id,data) tuples) to a kafka topic.
   *
   * @param source the source stream of events
   * @param system implicit actor system
   * @param materializer implicit actor materializer
   * @param config implicit config
   * @return Future[Done]
   */
  def toKafka(
    source: Source[(String, String), NotUsed]
  )(implicit system: ActorSystem, materializer: ActorMaterializer, config: Config): Future[Done] = {

    val k = config.getConfig("event.sink.kafka")

    val topic = k.getString("topic")
    val bootstrapServers = k.getString("bootstrap.servers")

    logger.info(s"writing to kafka://$bootstrapServers/$topic")

    val producerSettings = ProducerSettings[String, String](system, None, None).withBootstrapServers(bootstrapServers)

    source
      .map(e ⇒ new ProducerRecord[String, String](topic, e._1, e._2))
      .runWith(Producer.plainSink(producerSettings))
  }

  /**
   * Sends a stream of events ((id,data) tuples) to a kinesis stream.
   *
   * @param source the source stream of events
   * @param system implicit actor system
   * @param materializer implicit actor materializer
   * @param config implicit config
   * @return Future[Done]
   */
  def toKinesis(
    source: Source[(String, String), NotUsed]
  )(implicit system: ActorSystem, materializer: ActorMaterializer, config: Config) = {

    val k = config.getConfig("event.sink.kinesis")
    val endpoint = k.getString("endpoint")
    val stream = k.getString("stream")

    logger.info(s"writing to kinesis://$endpoint/$stream")

    val endpointConfiguration = new EndpointConfiguration(
      endpoint,
      k.getString("region")
    )
    implicit val kinesisClient: com.amazonaws.services.kinesis.AmazonKinesisAsync =
      AmazonKinesisAsyncClientBuilder
        .standard()
        .withEndpointConfiguration(endpointConfiguration)
        .build()
    system.registerOnTermination(kinesisClient.shutdown())

    val f = k.getConfig("flow")
    val flowSettings = KinesisFlowSettings(
      parallelism = f.getInt("parallelism"),
      maxBatchSize = f.getInt("max.batch.size"),
      maxRecordsPerSecond = f.getInt("max.records.per.second"),
      maxBytesPerSecond = f.getInt("max.bytes.per.second"),
      maxRetries = f.getInt("max.retries"),
      backoffStrategy =
        if (f.getString("backoff.strategy").equalsIgnoreCase("exponential"))
          KinesisFlowSettings.Exponential
        else KinesisFlowSettings.Linear,
      retryInitialTimeout = f.getLong("retry.initial.timeout").millis
    )

    source
      .map(
        e ⇒ (e._1, ByteBuffer.wrap(e._2.getBytes(StandardCharsets.UTF_8)))
      )
      .via(KinesisFlow.byPartitionAndData(stream, flowSettings))
      .runForeach(r ⇒ ()) // todo: maybe add stats later
  }
}
