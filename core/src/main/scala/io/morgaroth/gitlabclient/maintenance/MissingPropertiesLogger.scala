package io.morgaroth.gitlabclient.maintenance

import com.typesafe.scalalogging.Logger
import io.circe._
import io.morgaroth.gitlabclient.marshalling.Gitlab4SMarshalling.MJson
import org.slf4j.LoggerFactory

import scala.reflect.ClassTag

object MissingPropertiesLogger {
  val logger = Logger(LoggerFactory.getLogger(getClass.getPackageName + ".MissingPropertiesLogger"))

  private def calculateMissingFields[T: ClassTag](inputData: Json, value: Decoder.Result[T])(implicit enc: Encoder[T]): Unit = {
    val ct = implicitly[ClassTag[T]].runtimeClass
    value
      .map(MJson.encode(_))
      .map {
        case obj if obj.isObject =>
          val encodedKeys = obj.asObject.get.keys.toSet
          val inputKeys   = inputData.asObject.get.keys.toSet

          inputKeys
            .diff(encodedKeys)
            .toList
            .map(x => s"$x needs to be covered in ${ct.getCanonicalName} (${inputData.asObject.get.apply(x).get})")
        case _ =>
          Nil
      }
      .fold(
        err => List(s"${err.getMessage()} catched during calculating missing keys in ${ct.getCanonicalName}"),
        x => x,
      )
      .foreach(x => MissingPropertiesLogger.logger.info(x))
  }

  def loggingCodec[T: ClassTag](underlying: Codec[T]) = new Codec[T] {
    override def apply(a: T) = underlying.apply(a)

    override def apply(c: HCursor) = {
      val result                           = underlying.apply(c)
      implicit val implicitCodec: Codec[T] = underlying
      calculateMissingFields(c.value, result)
      result
    }

  }

}
