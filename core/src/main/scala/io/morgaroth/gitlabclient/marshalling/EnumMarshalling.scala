package io.morgaroth.gitlabclient.marshalling

import io.circe.{Codec, Decoder, Encoder}

import scala.reflect.ClassTag

object EnumMarshalling {
  def stringEnumCodecFor[A: ClassTag](possibleValues: Map[String, A])(encode: A => String): Codec[A] = {
    val decoder: Decoder[A] = Decoder.decodeString.emap { rawValue =>
      possibleValues
        .get(rawValue)
        .toRight(s"$rawValue is not valid one for ${implicitly[ClassTag[A]].runtimeClass.getCanonicalName}")
    }
    val encoder: Encoder[A] = Encoder.encodeString.contramap[A](encode)
    Codec.from(decoder, encoder)
  }

  def stringEnumCodecFor[A: ClassTag](possibleValues: Seq[A])(encode: A => String): Codec[A] = {
    stringEnumCodecFor[A](possibleValues.map(x => encode(x) -> x).toMap)(encode)
  }

  def stringEnumCodecOf[A: ClassTag](handler: EnumMarshallingGlue[A]): Codec[A] = {
    stringEnumCodecFor[A](handler.byName)(handler.rawValue)
  }
}

trait EnumMarshallingGlue[T] {
  def rawValue: T => String

  def byName: Map[String, T]
}
