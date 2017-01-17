package com.local.publisher.gc

import java.util.UUID

import com.google.cloud.pubsub._
import marshalling.Marshaller

import scala.concurrent.Future
import spray.json._

import scala.collection.JavaConversions._

object ActivityMessage extends Marshaller {

  private val KEY_VERSION = "version"
  private val KEY_LOCAL_UUID = "local-uuid"

  implicit val activityMessagePropertiesJsonFormat: RootJsonFormat[ActivityMessageProperties] = jsonFormat3(ActivityMessageProperties)
  implicit val activityMessageJsonFormat: RootJsonFormat[ActivityMessage] = jsonFormat5(ActivityMessage.apply)

  val pubSub: PubSub = PubSubOptions.getDefaultInstance.getService

  private val messageAttributes: (String, String) => Map[String, String] = (version: String, uuid: String) => {
    Map(KEY_VERSION -> version, KEY_LOCAL_UUID -> uuid)
  }

  def apply(message: Message): (ActivityMessage, Map[String, String]) = {
    (message.getPayloadAsString.parseJson.convertTo[ActivityMessage], message.getAttributes.toMap)
  }

  def getTopic(topicName: String): Topic = {
    pubSub.getTopic(topicName)
  }

  def publish(message: ActivityMessage, topic: Topic, version: String = "1.0.0", uuid: String = UUID.randomUUID().toString): Future[String] = {
    val messageJsonString = message.toJson.prettyPrint
    val pubSubMessage = Message.newBuilder(messageJsonString).clearAttributes.setAttributes(messageAttributes.apply(version, uuid)).build()
    Future {
      pubSub.publish(topic.getName, pubSubMessage)
    }
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