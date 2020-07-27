package io.morgaroth.gitlabclient.sttpbackend

import cats.Monad
import cats.data.EitherT
import cats.instances.future.catsStdInstancesForFuture
import cats.syntax.either._
import com.typesafe.scalalogging.{LazyLogging, Logger}
import io.morgaroth.gitlabclient._
import io.morgaroth.gitlabclient.query.{GitlabRequest, GitlabResponse}
import org.slf4j.LoggerFactory
import sttp.client._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SttpGitlabAPI(val config: GitlabConfig, apiConfig: GitlabRestAPIConfig)(implicit ex: ExecutionContext) extends GitlabRestAPI[Future] with LazyLogging {

  override implicit val m: Monad[Future] = implicitly[Monad[Future]]
  if (config.ignoreSslErrors) {
    TrustAllCerts.configure()
  }

  implicit val backend: SttpBackend[Try, Nothing, NothingT] = TryHttpURLConnectionBackend()
  private val requestsLogger = Logger(LoggerFactory.getLogger(getClass.getPackage.getName + ".requests"))

  override def invokeRequestRaw(requestData: GitlabRequest)(implicit requestId: RequestId): EitherT[Future, GitlabError, GitlabResponse] = {
    val u = requestData.render
    val requestWithoutPayload = basicRequest.method(requestData.method, uri"$u")
      .header("Private-Token", config.privateToken)
      .header("Accept", "application/json")

    val request = requestData.payload.map { rawPayload =>
      requestWithoutPayload.body(rawPayload).contentType("application/json")
    }.getOrElse(requestWithoutPayload)

    if (apiConfig.debug) logger.debug(s"request to send: $request")
    requestsLogger.info(s"Request ID {}, request: {}, payload:\n{}", requestId, request.body("removed for log"), request.body)

    val response: Either[GitlabError, GitlabResponse] =
      request
        .send()
        .toEither.leftMap[GitlabError](GitlabRequestingError("try-http-backend-left", requestId.id, _))
        .flatMap { response =>
          if (apiConfig.debug) logger.debug(s"received response: $response")
          val responseContentType = response.header("Content-Type")
          val responseBodyForLog = if (responseContentType.contains("application/json")) response.body.fold(identity, identity) else response.body.fold(identity, x => s"Response has content type $responseContentType and has ${x.length} size")
          requestsLogger.info(s"Response ID {}, response: {}, payload:\n{}", requestId, response.copy(body = response.body.bimap(_ => "There is an error body", _ => "There is a success body")), responseBodyForLog)
          val headers = response.headers.map(x => x.name -> x.value)
          response
            .body
            .leftMap(error => GitlabHttpError(response.code.code, "http-response-error", requestId.id, requestId.kind, Some(error)))
            .map(payload => GitlabResponse(headers.toMap, payload))
        }

    EitherT.fromEither[Future](response)
  }
}
