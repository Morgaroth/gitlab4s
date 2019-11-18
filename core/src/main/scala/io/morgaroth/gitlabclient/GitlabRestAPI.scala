package io.morgaroth.gitlabclient

import cats.Monad
import cats.data.EitherT
import com.typesafe.scalalogging.LazyLogging
import io.morgaroth.gitlabclient.marshalling.Gitlab4SMarshalling
import io.morgaroth.gitlabclient.query.{GitlabRequest, RequestGenerator}

import scala.language.{higherKinds, postfixOps}

trait GitlabRestAPI[F[_]] extends LazyLogging with Gitlab4SMarshalling {
  implicit def m: Monad[F]

  val API = "/api/v4"

  def config: GitlabConfig

  private val reqGen = RequestGenerator(config)

  protected def invokeRequest(request: GitlabRequest)(implicit requestId: RequestId): EitherT[F, GitlabError, String]
}