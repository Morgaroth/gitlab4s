package io.morgaroth.gitlabclient.marshalling

import cats.Monad
import cats.data.EitherT
import cats.syntax.either._
import io.circe._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.morgaroth.gitlabclient._
import io.morgaroth.gitlabclient.query.GitlabResponse

import java.time.ZonedDateTime

trait Gitlab4SMarshalling {

  implicit class Extractable(value: JsonObject) {
    def extract[T](implicit decoder: Decoder[T]): Either[Error, T] = decode[T](value.toString)
  }

  implicit val zonedDateTimeCodec: Codec[ZonedDateTime] = Codec.from(Decoder.decodeZonedDateTime, Encoder.encodeZonedDateTime)

  object MJson {
    def read[T: Decoder](str: String): Either[Error, T] = decode[T](str)

    def readT[F[_]: Monad, T: Decoder](str: String)(implicit requestId: RequestId): EitherT[F, GitlabError, T] =
      EitherT.fromEither(read[T](str).leftMap[GitlabError](e => GitlabUnmarshallingError(e.getMessage, requestId.id, e)))

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
