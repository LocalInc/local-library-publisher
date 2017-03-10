package com.local.publisher.gc.marshalling

import java.net.URL
import java.text.{ParseException, SimpleDateFormat}
import java.util.{Date, UUID}

import org.joda.time.DateTime
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives
import spray.json._


trait Marshaller extends Directives with SprayJsonSupport with DefaultJsonProtocol{

  implicit object UUIDFormat extends JsonFormat[UUID] {
    def write(uuid: UUID) = JsString(uuid.toString)
    def read(value: JsValue) = {
      value match {
        case JsString(uuid) => UUID.fromString(uuid)
        case _              => throw new DeserializationException("Expected hexadecimal UUID string")
      }
    }
  }

  implicit object URLFormat extends JsonFormat[URL] {
    def write(url: URL) = JsString(url.toString)
    def read(value: JsValue) = {
      value match {
        case JsString(url) => new URL(url)
        case _              => throw new DeserializationException("Expected hexadecimal UUID string")
      }
    }
  }

  implicit object DateTimeFormat extends JsonFormat[DateTime] {
    override def write(obj: DateTime) : JsValue = JsString(obj.toString("yyyy-MM-dd"))

    override def read(json: JsValue) : DateTime = json match {
      case JsString(rawDate) => {
        try {
          DateTime.parse(rawDate)
        } catch {
          case iae: IllegalArgumentException => deserializationError("Invalid date format")
          case _: Exception => None
        }
      }
      match {
        case jodaDate: DateTime => jodaDate
        case None => deserializationError(s"Couldn't parse JodaDate, got $rawDate")
      }

    }
  }

  def parseIsoDateString(date: String): Option[Date] = {
    if (date.length != 28) None
    else try Some(localIsoDateFormatter.get().parse(date))
    catch {
      case p: ParseException => None
    }
  }

  private val localIsoDateFormatter = new ThreadLocal[SimpleDateFormat] {
    override def initialValue() = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  }

  def dateToIsoString(date: Date) = localIsoDateFormatter.get().format(date)

  implicit object DateFormat extends JsonFormat[Date] {

    def write(date : Date) : JsValue = JsString(dateToIsoString(date))

    def read(json: JsValue) : Date = json match {

      case JsString(rawDate) => parseIsoDateString(rawDate) match {
        case None => deserializationError(s"Expected ISO Date format, got $rawDate")
        case Some(isoDate) => isoDate
      }

      case unknown => deserializationError(s"Expected JsString, got $unknown")
    }
  }

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

}