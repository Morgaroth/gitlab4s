package io.morgaroth.gitlabclient.apis

import java.net.URLEncoder

import cats.data.EitherT
import io.morgaroth.gitlabclient.models._
import io.morgaroth.gitlabclient.{EntityId, GitlabError, GitlabRestAPI, RequestId}

trait JobsAPI[F[_]] {
  this: GitlabRestAPI[F] =>

  // @see: https://docs.gitlab.com/ee/api/jobs.html#download-the-artifacts-archive
  def downloadLastSuccessfulJobArtifact(
                                         projectId: EntityId,
                                         gitReference: String,
                                         jobName: String,
                                       ): EitherT[F, GitlabError, Vector[TagInfo]] = {

    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/jobs/artifacts/$gitReference/download?job=${URLEncoder.encode(jobName, "utf-8")}")
    ???
  }

  // @see: https://docs.gitlab.com/ee/api/jobs.html#download-a-single-artifact-file-by-job-id
  def downloadSingleFileFromJobArtifact(
                                         projectId: EntityId,
                                         jobId: BigInt,
                                         artifactPath: String,
                                       ): EitherT[F, GitlabError, String] = {
    implicit val rId: RequestId = RequestId.newOne("get-single-artifact")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/jobs/$jobId/artifacts/$artifactPath")
    invokeRequestRaw(req).map(_.payload)
  }

  // @see: https://docs.gitlab.com/ee/api/jobs.html#download-a-single-artifact-file-by-job-id
  def downloadJobArtifacts(
                            projectId: EntityId,
                            jobId: BigInt,
                          ): EitherT[F, GitlabError, ContentDisposition] = {
    implicit val rId: RequestId = RequestId.newOne("get-single-artifact")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/jobs/$jobId/artifacts")
    invokeRequestRaw(req).map { resp =>
      val str = resp.headers("Content-Disposition").split(";").map(_.trim).find(_.startsWith("filename=")).map(_.stripPrefix("filename=").stripPrefix(""""""").stripSuffix("""""""))
      ContentDisposition(resp.payload, str.getOrElse(s"$jobId-artifacts"))
    }
  }

}

case class ContentDisposition(payload: String, filename: String)