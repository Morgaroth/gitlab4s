package io.morgaroth.gitlabclient.apis

import cats.data.EitherT
import io.morgaroth.gitlabclient.apis.RawResponse.stringContentTypes
import io.morgaroth.gitlabclient.query.GitlabResponse
import io.morgaroth.gitlabclient.{EntityId, GitlabError, GitlabRestAPI, RequestId}

trait JobsAPI[F[_]] {
  this: GitlabRestAPI[F] =>

  // @see: https://docs.gitlab.com/ee/api/jobs.html#download-a-single-artifact-file-by-job-id
  def downloadSingleFileFromJobArtifact(
                                         projectId: EntityId,
                                         jobId: BigInt,
                                         artifactPath: String,
                                       ): EitherT[F, GitlabError, RawResponse] = {
    implicit val rId: RequestId = RequestId.newOne("get-single-artifact")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/jobs/$jobId/artifacts/$artifactPath")
    byteRequest(req).map(RawResponse.from)
  }

  // @see: https://docs.gitlab.com/ee/api/job_artifacts.html#get-job-artifacts
  def downloadJobArtifacts(
                            projectId: EntityId,
                            jobId: BigInt,
                          ): EitherT[F, GitlabError, RawResponse] = {
    implicit val rId: RequestId = RequestId.newOne("get-job-artifacts")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/jobs/$jobId/artifacts")
    byteRequest(req).map(RawResponse.from)
  }

}

object RawResponse {
  // to be extended on demand
  val stringContentTypes = Set("application/json", "application/xml", "text/xml", "text/html", "text/plain")

  def from(resp: GitlabResponse[Array[Byte]]) = {
    val contentType = resp.headers("Content-Type")
    val contentEncoding = resp.headers.get("Content-Encoding")
    val fileName = resp.headers.get("Content-Disposition").flatMap(_.split(";").map(_.trim).find(_.startsWith("filename=")).map(_.stripPrefix("filename=").stripPrefix(""""""").stripSuffix(""""""")))
    RawResponse(contentType, contentEncoding, resp.payload, fileName)
  }
}

case class RawResponse(contentType: String, contentEncoding: Option[String], payload: Array[Byte], filename: Option[String]) {
  def asString: Either[IllegalArgumentException, String] =
    Either.cond(stringContentTypes(contentType), new String(payload, contentEncoding.getOrElse("utf-8")), new IllegalArgumentException(s"$contentType is not a string one"))
}