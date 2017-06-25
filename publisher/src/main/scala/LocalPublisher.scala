package com.spotsinc.publisher.gc

import java.util.UUID

import marshalling.Marshaller
import com.spotify.google.cloud.pubsub.client.{Message, Publisher, Pubsub}

import scala.collection.JavaConversions._
import spray.json._
import com.spotify.google.cloud.pubsub.client.Message.encode

object ActivityMessage extends Marshaller {

  private val KEY_VERSION = "version"
  private val KEY_SPOTS_UUID = "spots-uuid"

  implicit val activityMessagePropertiesJsonFormat: RootJsonFormat[ActivityMessageProperties] = jsonFormat3(ActivityMessageProperties)
  implicit val activityMessageJsonFormat: RootJsonFormat[ActivityMessage] = jsonFormat5(ActivityMessage.apply)

  val pubsub: Pubsub = Pubsub.builder()
    .build()

  val publisher: Publisher = Publisher.builder()
    .pubsub(pubsub)
    .project(sys.env.getOrElse("ENV_PROJECT", ""))
    .concurrency(128)
    .build()

  private val messageAttributes: (String, String) => Map[String, String] = (version: String, uuid: String) => {
    Map(KEY_VERSION -> version, KEY_SPOTS_UUID -> uuid)
  }

  def apply(message: Message): (ActivityMessage, Map[String, String]) = {
    (message.data().parseJson.convertTo[ActivityMessage], message.attributes.toMap)
  }

  def getTopic: String = {
    sys.env.getOrElse("ENV_QUEUE_TOPIC", "")
  }

  def publish(message: ActivityMessage, topic: String, version: String = "1.0.0", uuid: String = UUID.randomUUID().toString): String = {
    val messageJsonString = message.toJson.toString.getBytes
    val messageToPublish = Message.builder().attributes(messageAttributes(version, uuid))
      .data(encode(messageJsonString))
      .build()
    publisher.publish(topic, messageToPublish).get()
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