package io.morgaroth.gitlabclient.sttpbackend

import cats.Monad
import cats.data.EitherT
import cats.syntax.either._
import com.typesafe.scalalogging.{LazyLogging, Logger}
import io.morgaroth.gitlabclient._
import io.morgaroth.gitlabclient.query.Methods.Get
import io.morgaroth.gitlabclient.query.{GitlabRequest, GitlabResponse}
import org.slf4j.LoggerFactory
import sttp.client3._
import sttp.client3.httpclient.HttpClientFutureBackend

import scala.concurrent.{ExecutionContext, Future}

class SttpGitlabAPIFuture(val config: GitlabConfig, apiConfig: GitlabRestAPIConfig)(implicit ex: ExecutionContext)
    extends GitlabRestAPI[Future]
    with LazyLogging {

  override val m: Monad[Future] = cats.instances.future.catsStdInstancesForFuture
  if (config.ignoreSslErrors) {
    TrustAllCerts.configure()
  }

  val backend: SttpBackend[Future, _] = HttpClientFutureBackend()
  private val requestsLogger          = Logger(LoggerFactory.getLogger(getClass.getPackage.getName + ".requests"))

  private def logRequest[T](request: RequestT[Identity, Either[String, T], Nothing], requestData: GitlabRequest)(implicit
      requestId: RequestId,
  ): Unit = {
    if (apiConfig.debug) logger.debug(s"request to send: $request")
    if (requestData.method == Get) {
      requestsLogger.info(s"Request ID {}, request: {}", requestId, request)
    } else {
      requestsLogger.info(s"Request ID {}, request: {}, payload:\n{}", requestId, request.body("removed for log"), request.body)
    }
  }

  private def execReq[T](
      request: RequestT[Identity, Either[String, T], Any],
  )(implicit requestId: RequestId): EitherT[Future, GitlabError, (Map[String, String], T)] = {

    EitherT(request.send(backend).map(_.asRight).recover { case t: Throwable => t.asLeft })
      .leftMap[GitlabError](GitlabRequestingError("try-http-backend-left", requestId.id, _))
      .subflatMap { response =>
        if (apiConfig.debug) logger.debug(s"received response: $response")
        val responseContentType = response.header("Content-Type")
        val responseLength      = response.header("Content-Length").map(_.toInt)
        val responseBodyForLog =
          if (responseContentType.contains("application/json")) response.body.fold(identity, identity)
          else response.body.fold(identity, _ => s"Response has content type $responseContentType and has $responseLength bytes length")
        requestsLogger.info(
          s"Response ID {}, response: {}, payload:\n{}",
          requestId,
          response.copy(body = response.body.bimap(_ => "There is an error body", _ => "There is a success body")),
          responseBodyForLog,
        )
        val headers = response.headers.map(x => x.name -> x.value)
        response.body
          .leftMap(error => GitlabHttpError(response.code.code, "http-response-error", requestId.id, requestId.kind, Some(error)))
          .map(payload => headers.toMap -> payload)
      }
  }

  private def createReq(requestData: GitlabRequest) = {
    val u = requestData.render
    val requestWithoutPayload = basicRequest
      .method(requestData.method, uri"$u")
      .header(AuthHeaderName, authHeader(requestData))
    requestData.payload
      .map(rawPayload => requestWithoutPayload.body(rawPayload).contentType("application/json"))
      .getOrElse(requestWithoutPayload)
  }

  override def byteRequest(
      requestData: GitlabRequest,
  )(implicit requestId: RequestId): EitherT[Future, GitlabError, GitlabResponse[Array[Byte]]] = {
    val request = createReq(requestData).response(asByteArray)
    logRequest(request, requestData)
    val response = execReq(request).map(x => GitlabResponse(x._1, x._2))
    response
  }

  override def invokeRequestRaw(
      requestData: GitlabRequest,
  )(implicit requestId: RequestId): EitherT[Future, GitlabError, GitlabResponse[String]] = {
    val request = createReq(requestData)
      .header("Accept", "application/json")

    logRequest(request, requestData)

    val response = execReq(request).map(x => GitlabResponse(x._1, x._2))
    response
  }

}
