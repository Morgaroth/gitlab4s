package io.gitlab.mateuszjaje.gitlabclient
package apisv2

import apis.RawResponse.stringContentTypes
import apisv2.GitlabApiT.syntax.*
import models.JobFullInfo
import query.GitlabResponse

trait JobsAPIV2[F[_]] {
  this: GitlabRestAPIV2[F] =>

  // @see: https://docs.gitlab.com/ee/api/jobs.html#get-a-single-job
  def getJob(projectId: EntityId, jobId: BigInt): F[Either[GitlabError, JobFullInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("get-pipeline-job-by-id")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/jobs/$jobId").withProjectId(projectId)
    invokeRequest(req).unmarshall[JobFullInfo]
  }

  // @see: https://docs.gitlab.com/ee/api/jobs.html#cancel-a-job
  def cancelJob(projectId: EntityId, jobId: BigInt): F[Either[GitlabError, JobFullInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("cancel-pipeline-job")
    val req                     = reqGen.post(s"$API/projects/${projectId.toStringId}/jobs/$jobId/cancel").withProjectId(projectId)
    invokeRequest(req).unmarshall[JobFullInfo]
  }

  // @see: https://docs.gitlab.com/ee/api/jobs.html#cancel-a-job
  def retryJob(projectId: EntityId, jobId: BigInt): F[Either[GitlabError, JobFullInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("retry-pipeline-job")
    val req                     = reqGen.post(s"$API/projects/${projectId.toStringId}/jobs/$jobId/retry").withProjectId(projectId)
    invokeRequest(req).unmarshall[JobFullInfo]
  }

  // @see: https://docs.gitlab.com/ee/api/jobs.html#download-a-single-artifact-file-by-job-id
  def downloadSingleFileFromJobArtifact(
      projectId: EntityId,
      jobId: BigInt,
      artifactPath: String,
  ): F[Either[GitlabError, RawResponse]] = {
    implicit val rId: RequestId = RequestId.newOne("get-single-artifact")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/jobs/$jobId/artifacts/$artifactPath").withProjectId(projectId)
    byteRequest(req).map(RawResponse.from)
  }

  // @see: https://docs.gitlab.com/ee/api/job_artifacts.html#get-job-artifacts
  def downloadJobArtifacts(
      projectId: EntityId,
      jobId: BigInt,
  ): F[Either[GitlabError, RawResponse]] = {
    implicit val rId: RequestId = RequestId.newOne("get-job-artifacts")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/jobs/$jobId/artifacts").withProjectId(projectId)
    byteRequest(req).map(RawResponse.from)
  }

  // @see: https://docs.gitlab.com/ee/api/jobs.html#get-a-log-file
  def downloadJobLog(
      projectId: EntityId,
      jobId: BigInt,
  ): F[Either[GitlabError, String]] = {
    implicit val rId: RequestId = RequestId.newOne("get-job-log")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/jobs/$jobId/trace").withProjectId(projectId)
    invokeRequest(req)
  }

}

object RawResponse {
  // to be extended on demand
  val stringContentTypes = Set("application/json", "application/xml", "text/xml", "text/html", "text/plain")

  def from(resp: GitlabResponse[Array[Byte]]) = {
    val contentType     = resp.headers("Content-Type")
    val contentEncoding = resp.headers.get("Content-Encoding")
    val fileName = resp.headers
      .get("Content-Disposition")
      .flatMap(
        _.split(";").map(_.trim).find(_.startsWith("filename=")).map(_.stripPrefix("filename=").stripPrefix(""""""").stripSuffix(""""""")),
      )
    RawResponse(contentType, contentEncoding, resp.payload, fileName)
  }

}

case class RawResponse(contentType: String, contentEncoding: Option[String], payload: Array[Byte], filename: Option[String]) {
  def asString: Either[IllegalArgumentException, String] =
    Either.cond(
      stringContentTypes(contentType),
      new String(payload, contentEncoding.getOrElse("utf-8")),
      new IllegalArgumentException(s"$contentType is not a string one"),
    )

}
