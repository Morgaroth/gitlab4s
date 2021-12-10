package io.gitlab.mateuszjaje.gitlabclient
package sttptrybackend

import query.{GitlabRequest, GitlabResponse}

import cats.Monad
import cats.data.EitherT
import com.typesafe.scalalogging.{LazyLogging, Logger}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.Try

class SttpGitlabAPITryFuture(val config: GitlabConfig, apiConfig: GitlabRestAPIConfig)(implicit val m: Monad[Future])
    extends GitlabRestAPI[Future]
    with LazyLogging {

  val tryBackend = new SttpGitlabAPITry(config, apiConfig) {
    override val requestsLogger = Logger(LoggerFactory.getLogger(getClass.getPackage.getName + ".requests"))
  }

  override def byteRequest(
      requestData: GitlabRequest,
  )(implicit requestId: RequestId): EitherT[Future, GitlabError, GitlabResponse[Array[Byte]]] =
    EitherT(Try(tryBackend.byteRequest(requestData).value).toEither.fold(Future.failed, Future.successful))

  override def invokeRequestRaw(
      requestData: GitlabRequest,
  )(implicit requestId: RequestId): EitherT[Future, GitlabError, GitlabResponse[String]] =
    EitherT(Try(tryBackend.invokeRequestRaw(requestData).value).toEither.fold(Future.failed, Future.successful))

}
