package io.gitlab.mateuszjaje.gitlabclient
package sttpbackend

import apisv2.ThisMonad
import apisv2.ThisMonad.AAA
import query.Methods.Get
import query.{GitlabRequest, GitlabResponse}

import com.typesafe.scalalogging.{LazyLogging, Logger}
import org.slf4j.LoggerFactory
import sttp.capabilities
import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.{Task, UIO, ZIO}

import scala.concurrent.ExecutionContext

class ZSttpGitlabAPI(val config: GitlabConfig, apiConfig: GitlabRestAPIConfig)(implicit ex: ExecutionContext)
    extends GitlabRestAPIV2[UIO]
    with LazyLogging {

  implicit override def m: ThisMonad[UIO] = new ThisMonad[UIO] {
    override def subFlatMap[A, B](fa: UIO[Either[GitlabError, A]])(f: A => Either[GitlabError, B]): AAA[UIO, B] =
      fa.flatMap(x => ZIO.succeed(x.flatMap(f)))

    override def pure[A](x: A): UIO[Either[GitlabError, A]] = ZIO.right(x)

    override def flatMap[A, B](fa: AAA[UIO, A])(f: A => AAA[UIO, B]): AAA[UIO, B] = {
      fa.flatMap { (data: Either[GitlabError, A]) =>
        data
          .map(f)
          .fold(
            err => ZIO.left(err),
            identity,
          )
      }
    }

    override def tailRecM[A, B](a: A)(f: A => AAA[UIO, Either[A, B]]): AAA[UIO, B] = {
      flatMap(f(a)) {
        case Left(a)  => tailRecM(a)(f)
        case Right(b) => pure(b)
      }
    }

    override def sequence[A](x: Vector[UIO[Either[GitlabError, A]]]): AAA[UIO, Vector[A]] = {
      ZIO.foreach(x)(identity).map {
        _.foldLeft[Either[GitlabError, Vector[A]]](Right(Vector.empty[A])) {
          case (e @ Left(_), _)       => e
          case (Right(acc), Right(e)) => Right(acc :+ e)
          case (_, Left(e))           => Left(e)
        }
      }
    }

  }

  if (config.ignoreSslErrors) {
    TrustAllCerts.configure()
  }

  val backend: Task[SttpBackend[Task, ZioStreams & capabilities.WebSockets]] = HttpClientZioBackend()
  private val requestsLogger = Logger(LoggerFactory.getLogger(getClass.getPackage.getName + ".requests"))

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
  )(implicit requestId: RequestId): UIO[Either[GitlabError, (Map[String, String], T)]] = {

    backend
      .flatMap(request.send(_))
      .mapError[GitlabError](GitlabRequestingError("zio-http-backend-left", requestId.id, _))
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
          response.copy(body = response.body match {
            case Left(_)  => "There is an error body"
            case Right(_) => "There is a success body"
          }),
          responseBodyForLog,
        )
        val headers = response.headers.map(x => x.name -> x.value)
        ZIO
          .fromEither(response.body)
          .mapError(error => GitlabHttpError(response.code.code, "http-response-error", requestId.id, requestId.kind, Some(error)))
          .map(payload => headers.toMap -> payload)
      }
      .either
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
  )(implicit requestId: RequestId): UIO[Either[GitlabError, GitlabResponse[Array[Byte]]]] = {
    val request = createReq(requestData).response(asByteArray)
    logRequest(request, requestData)
    execReq(request).map(_.map(x => GitlabResponse(x._1, x._2)))
  }

  override def invokeRequestRaw(
      requestData: GitlabRequest,
  )(implicit requestId: RequestId): UIO[Either[GitlabError, GitlabResponse[String]]] = {
    val request = createReq(requestData)
      .header("Accept", "application/json")
    logRequest(request, requestData)
    execReq(request).map(_.map(x => GitlabResponse(x._1, x._2)))
  }

}
