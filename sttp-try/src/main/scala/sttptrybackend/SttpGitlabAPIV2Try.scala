package io.gitlab.mateuszjaje.gitlabclient
package sttptrybackend

import apisv2.GitlabApiT
import query.Methods.Get
import query.{GitlabRequest, GitlabResponse}

import cats.Id
import cats.syntax.either.*
import com.typesafe.scalalogging.{LazyLogging, Logger}
import org.slf4j.LoggerFactory
import sttp.client3.*

import scala.util.Try

class SttpGitlabAPIV2Try(val config: GitlabConfig, apiConfig: GitlabRestAPIConfig) extends GitlabRestAPIV2[cats.Id] with LazyLogging {

  if (config.ignoreSslErrors) {
    TrustAllCerts.configure()
  }

  implicit override def m: GitlabApiT[Id] = GitlabApiT.fromCats[cats.Id]

  val backend: SttpBackend[Try, Any] = TryHttpURLConnectionBackend()
  val requestsLogger                 = Logger(LoggerFactory.getLogger(getClass.getPackage.getName + ".requests"))

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
  )(implicit requestId: RequestId): Either[GitlabError, (Map[String, String], T)] = {
    request
      .send(backend)
      .toEither
      .leftMap[GitlabError](GitlabRequestingError("try-http-backend-left", requestId.id, _))
      .flatMap { response =>
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
  )(implicit requestId: RequestId): Id[Either[GitlabError, GitlabResponse[Array[Byte]]]] = {
    val request = createReq(requestData).response(asByteArray)
    logRequest(request, requestData)
    val response = execReq(request).map(x => GitlabResponse(x._1, x._2))
    response
  }

  override def invokeRequestRaw(
      requestData: GitlabRequest,
  )(implicit requestId: RequestId): Id[Either[GitlabError, GitlabResponse[String]]] = {
    val request = createReq(requestData)
      .header("Accept", "application/json")

    logRequest(request, requestData)

    val response = execReq(request).map(x => GitlabResponse(x._1, x._2))
    response
  }

}
