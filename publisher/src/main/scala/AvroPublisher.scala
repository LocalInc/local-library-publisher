package com.spotsinc.publisher.avro

import java.io.{ByteArrayOutputStream, File, FileOutputStream}
import java.util.UUID

import com.sksamuel.avro4s._
import com.spotify.google.cloud.pubsub.client.Message.encode
import com.spotify.google.cloud.pubsub.client.{Message, Publisher, Pubsub}
import org.apache.avro.Schema

import scala.collection.JavaConversions._

class AvroPublisher[T] {

  private val KEY_VERSION = "version"
  private val KEY_UUID = "uuid"

  private val messageAttributes: (String, String) => Map[String, String] = (version: String, uuid: String) => {
    Map(KEY_VERSION -> version, KEY_UUID -> uuid)
  }

  val pubsub: Pubsub = Pubsub.builder()
    .build()

  val publisher: Publisher = Publisher.builder()
    .pubsub(pubsub)
    .project(sys.env.getOrElse("ENV_GCC_PROJECT", ""))
    .concurrency(128)
    .build()


  def createAvroSchema(message: T)(implicit schema: SchemaFor[T]): Schema = {
    AvroSchema[T]
  }

  def serializeBinaryAvroMessage(message: Seq[T])(implicit schema: SchemaFor[T], toRecord: ToRecord[T]): Array[Byte] = {
    val binarySerializer = new ByteArrayOutputStream()
    val output = AvroOutputStream.data[T](binarySerializer)
    output.write(message)
    output.close()
    binarySerializer.toByteArray
  }

  def deserializeBinaryAvroMessage(message: Array[Byte])(implicit schema: SchemaFor[T], fromRecord: FromRecord[T]): Seq[T] = {
    val input = AvroInputStream.data[T](message)
    val results = input.iterator.toSeq
    input.close()
    results
  }


  def publishBinary(message: Array[Byte], topic: String, version: String = "1.0.0", uuid: String = UUID.randomUUID().toString): String = {
    val messageToPublish = Message.builder().attributes(messageAttributes(version, uuid))
      .data(encode(message))
      .build()
    publisher.publish(topic, messageToPublish).get()
  }
}
