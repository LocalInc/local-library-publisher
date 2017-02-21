package com.local.publisher.gc

import java.time.ZonedDateTime
import java.util.UUID

import akka.NotUsed
import akka.actor.ActorRef
import marshalling.Marshaller
import com.qubit.pubsub.akka.attributes._
import com.typesafe.scalalogging.LazyLogging
import akka.stream._
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.qubit.pubsub.akka.PubSubSink
import com.qubit.pubsub.client.{PubSubMessage, PubSubTopic}
import com.qubit.pubsub.client.grpc.PubSubGrpcClient
import com.qubit.pubsub.client.retry.RetryingPubSubClient

import scala.concurrent.duration._
import spray.json._

object ActivityMessage extends Marshaller with LazyLogging {

  private val KEY_VERSION = "version"
  private val KEY_LOCAL_UUID = "local-uuid"

  implicit val activityMessagePropertiesJsonFormat: RootJsonFormat[ActivityMessageProperties] = jsonFormat3(ActivityMessageProperties)
  implicit val activityMessageJsonFormat: RootJsonFormat[ActivityMessage] = jsonFormat5(ActivityMessage.apply)

  val client = RetryingPubSubClient(PubSubGrpcClient())
  val pubSubSink = new PubSubSink(getTopic, 1.milliseconds)

  val attributes = Attributes(List(
    PubSubStageBufferSizeAttribute(sys.env.getOrElse("ENV_QUEUE_BUFFER_SIZE", "").toInt),
    PubSubStageMaxRetriesAttribute(100),
    PubSubStageRetryJitterAttribute(1, 5),
    PubSubPublishTimeoutAttribute(60.seconds)))

  val sink: Sink[PubSubMessage, NotUsed] = Sink.fromGraph(pubSubSink).withAttributes(attributes)

  val overflowStrategy: OverflowStrategy = akka.stream.OverflowStrategy.fail
  val bufferSize: Int = sys.env.getOrElse("ENV_QUEUE_BUFFER_SIZE", "").toInt

  val source: Source[PubSubMessage, ActorRef] = Source.actorRef[PubSubMessage](bufferSize, overflowStrategy)

  val publisher: ActorRef = Flow[PubSubMessage].to(sink).runWith(source)

  private val messageAttributes: (String, String) => Map[String, String] = (version: String, uuid: String) => {
    Map(KEY_VERSION -> version, KEY_LOCAL_UUID -> uuid)
  }

  def apply(message: PubSubMessage): (ActivityMessage, Map[String, String]) = {
    (new String(message.payload).parseJson.convertTo[ActivityMessage], message.attributes.getOrElse(Map()))
  }

  def getTopic: PubSubTopic = {
    PubSubTopic(sys.env.getOrElse("ENV_PROJECT", ""), sys.env.getOrElse("ENV_QUEUE_TOPIC", ""))
  }

  def publish(message: ActivityMessage, topic: PubSubTopic, version: String = "1.0.0", uuid: String = UUID.randomUUID().toString): Unit = {
    val messageJsonString = message.toJson.prettyPrint
    val pubSubMessage = PubSubMessage(messageJsonString.getBytes, Some(uuid), Some(ZonedDateTime.now()), Some(messageAttributes.apply(version, uuid)))
    publisher ! pubSubMessage
  }
}

case class ActivityMessage(
                            subject: String,
                            verb: String,
                            directObject: String,
                            indirectObject: String,
                            properties: ActivityMessageProperties
                          )
case class ActivityMessageProperties(
                                    subject: Map[String, String],
                                    directObject: Map[String, String],
                                    verb: Map[String, String]
                                  )