package io.morgaroth.gitlabclient.marshalling

import java.time.ZonedDateTime

import cats.Monad
import cats.data.EitherT
import cats.syntax.either._
import io.circe._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.morgaroth.gitlabclient._
import io.morgaroth.gitlabclient.query.GitlabResponse

trait Gitlab4SMarshalling {

  implicit class Extractable(value: JsonObject) {
    def extract[T](implicit decoder: Decoder[T]): Either[Error, T] = decode[T](value.toString)
  }

  implicit val zonedDateTimeCodec: Codec[ZonedDateTime] = Codec.from(Decoder.decodeZonedDateTime, Encoder.encodeZonedDateTime)

  object MJson {
    def read[T](str: String)(implicit d: Decoder[T]): Either[Error, T] = decode[T](str)

    def readT[F[_], T](str: String)(implicit d: Decoder[T], m: Monad[F], requestId: RequestId): EitherT[F, GitlabError, T] =
      EitherT.fromEither(read[T](str).leftMap[GitlabError](e => GitlabUnmarshallingError(e.getMessage, requestId.id, e)))

    def write[T](value: T)(implicit d: Encoder[T]): String = Printer.noSpaces.copy(dropNullValues = true).print(value.asJson)

    def writePretty[T](value: T)(implicit d: Encoder[T]): String = printer.print(value.asJson)
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
