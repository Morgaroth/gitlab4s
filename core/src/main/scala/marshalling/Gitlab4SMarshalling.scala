package io.gitlab.mateuszjaje.gitlabclient
package marshalling

import apisv2.ThisMonad
import maintenance.MissingPropertiesLogger
import query.GitlabResponse

import cats.Functor
import cats.data.EitherT
import io.circe._
import io.circe.parser.{decode, parse}
import io.circe.syntax.EncoderOps

import java.time.ZonedDateTime

trait Gitlab4SMarshalling {

  implicit val zonedDateTimeCodec: Codec[ZonedDateTime] = Codec.from(Decoder.decodeZonedDateTime, Encoder.encodeZonedDateTime)

  object MJson {
    def read[T: Decoder](str: String): Either[Error, T] = decode[T](str)

    def loggingDecode[A](input: String)(implicit id: RequestId, decoder: Decoder[A]): Either[Error, A] = {
      import cats.syntax.either._
      parse(input)
        .leftMap(_.asInstanceOf[Error])
        .flatMap { data =>
          val cursor: HCursor = HCursor.fromJson(data)
          decoder match {
            case loggingDecoder: MissingPropertiesLogger[A] => loggingDecoder.apply(cursor)
            case _                                          => decoder.apply(cursor)
          }
        }
    }

    def readE[T: Decoder](str: String)(implicit requestId: RequestId): Either[GitlabError, T] = {
      import cats.syntax.either._
      loggingDecode[T](str).leftMap[GitlabError](e => GitlabUnmarshallingError(e.getMessage, requestId.id, e))
    }

    def write[T: Encoder](value: T): String       = Printer.noSpaces.copy(dropNullValues = true).print(value.asJson)
    def encode[T: Encoder](value: T): Json        = value.asJson
    def writePretty[T: Encoder](value: T): String = printer.print(value.asJson)
  }

  // keep all special settings with method write above
  implicit val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  implicit class unmarshallEitherT[F[_]](data: EitherT[F, GitlabError, String])(implicit m: Functor[F]) {
    def unmarshall[TargetType: Decoder](implicit rId: RequestId): EitherT[F, GitlabError, TargetType] =
      data.subflatMap(MJson.readE[TargetType])

  }

  implicit class unmarshallEitherGitlabT[F[_]](data: EitherT[F, GitlabError, GitlabResponse[String]])(implicit m: Functor[F]) {
    def unmarshall[TargetType: Decoder](implicit rId: RequestId): EitherT[F, GitlabError, TargetType] =
      data.map(_.payload).subflatMap(MJson.readE[TargetType])

  }

  implicit class unmarshallF[F[_]](data: F[Either[GitlabError, GitlabResponse[String]]])(implicit m: ThisMonad[F]) {
    def unmarshall[TargetType: Decoder](implicit rId: RequestId): F[Either[GitlabError, TargetType]] = {
      val value: F[Either[GitlabError, String]] = ThisMonad.syntax.toOps(data).map(_.payload)
      ThisMonad.syntax.toOps(value).subFlatMap(MJson.readE[TargetType](_))
    }

  }

  implicit class unmarshallFF[F[_]](data: F[Either[GitlabError, String]])(implicit m: ThisMonad[F]) {
    def unmarshall[TargetType: Decoder](implicit rId: RequestId): F[Either[GitlabError, TargetType]] =
      ThisMonad.syntax.toOps(data).subFlatMap(MJson.readE[TargetType](_))

  }

}

object Gitlab4SMarshalling extends Gitlab4SMarshalling
