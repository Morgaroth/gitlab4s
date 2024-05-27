package io.gitlab.mateuszjaje.gitlabclient
package marshalling

import cats.syntax.option.*
import com.typesafe.scalalogging.Logger
import io.circe.{Codec, Decoder, Encoder}
import org.slf4j.LoggerFactory

import scala.reflect.ClassTag

object EnumMarshalling {
  private val logger = Logger(LoggerFactory.getLogger(getClass.getPackage.getName + ".MissingPropertiesLogger"))

  def stringEnumCodecFor[A: ClassTag](possibleValues: Map[String, A])(encode: A => String, defaultDecode: Option[String => A]): Codec[A] = {
    val decoder: Decoder[A] = Decoder.decodeString.emap { rawValue =>
      possibleValues
        .get(rawValue)
        .orElse {
          defaultDecode.map { fun =>
            logger.warn(s"$rawValue is not explicit one for ${implicitly[ClassTag[A]].runtimeClass.getCanonicalName}")
            fun(rawValue)
          }
        }
        .toRight(s"$rawValue is not valid one for ${implicitly[ClassTag[A]].runtimeClass.getCanonicalName}")
    }
    val encoder: Encoder[A] = Encoder.encodeString.contramap[A](encode)
    Codec.from(decoder, encoder)
  }

  def stringEnumCodecFor[A: ClassTag](possibleValues: Seq[A])(encode: A => String): Codec[A] =
    stringEnumCodecFor[A](possibleValues.map(x => encode(x) -> x).toMap)(encode, none)

  def stringEnumCodecOf[A: ClassTag](handler: EnumMarshallingGlue[A]): Codec[A] =
    stringEnumCodecFor[A](handler.byName)(handler.rawValue, none)

  def unrestrictedStringEnumCodecOf[A: ClassTag](handler: EnumMarshallingGlue[A] & DefaultEnumMarshalling[A]): Codec[A] =
    stringEnumCodecFor[A](handler.byName)(handler.rawValue, (handler.wrapUnknown _).some)

}

trait EnumMarshallingGlue[T] {
  def rawValue: T => String

  def byName: Map[String, T]
}

trait DefaultEnumMarshalling[T] {
  this: EnumMarshallingGlue[T] =>
  def wrapUnknown(value: String): T
}
