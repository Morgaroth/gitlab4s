package io.gitlab.mateuszjaje.gitlabclient
package marshalling

import maintenance.MissingPropertiesLogger
import query.GitlabResponse

import cats.Monad
import cats.data.EitherT
import cats.syntax.either._
import io.circe._
import io.circe.parser.{decode, parse}
import io.circe.syntax.EncoderOps

import java.time.ZonedDateTime

trait Gitlab4SMarshalling {

  implicit val zonedDateTimeCodec: Codec[ZonedDateTime] = Codec.from(Decoder.decodeZonedDateTime, Encoder.encodeZonedDateTime)

  object MJson {
    def read[T: Decoder](str: String): Either[Error, T] = decode[T](str)

    def loggingDecode[A](input: String, id: RequestId)(implicit decoder: Decoder[A]): Either[Error, A] =
      parse(input)
        .leftMap(_.asInstanceOf[Error])
        .flatMap { data =>
          val cursor: HCursor = HCursor.fromJson(data)
          decoder match {
            case loggingDecoder: MissingPropertiesLogger[A] => loggingDecoder.apply(cursor, id)
            case _                                          => decoder.apply(cursor)
          }
        }

    def readT[F[_]: Monad, T: Decoder](str: String)(implicit requestId: RequestId): EitherT[F, GitlabError, T] =
      EitherT.fromEither(
        loggingDecode[T](str, requestId).leftMap[GitlabError](e => GitlabUnmarshallingError(e.getMessage, requestId.id, e)),
      )

    def write[T: Encoder](value: T): String       = Printer.noSpaces.copy(dropNullValues = true).print(value.asJson)
    def encode[T: Encoder](value: T): Json        = value.asJson
    def writePretty[T: Encoder](value: T): String = printer.print(value.asJson)
  }

  // keep all special settings with method write above
  implicit val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  implicit class unmarshallEitherT[F[_]](data: EitherT[F, GitlabError, String])(implicit m: Monad[F]) {
    def unmarshall[TargetType: Decoder](implicit rId: RequestId): EitherT[F, GitlabError, TargetType] =
      data.flatMap(MJson.readT[F, TargetType])

  }

  implicit class unmarshallEitherGitlabT[F[_]](data: EitherT[F, GitlabError, GitlabResponse[String]])(implicit m: Monad[F]) {
    def unmarshall[TargetType: Decoder](implicit rId: RequestId): EitherT[F, GitlabError, TargetType] =
      data.map(_.payload).flatMap(MJson.readT[F, TargetType])

  }

}

object Gitlab4SMarshalling extends Gitlab4SMarshalling
