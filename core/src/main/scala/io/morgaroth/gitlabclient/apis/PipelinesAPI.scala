package io.morgaroth.gitlabclient.apis

import cats.data.EitherT
import io.morgaroth.gitlabclient._
import io.morgaroth.gitlabclient.helpers.CustomDateTimeFormatter._
import io.morgaroth.gitlabclient.models._
import io.morgaroth.gitlabclient.query.ParamQuery._

import java.time.ZonedDateTime

trait PipelinesAPI[F[_]] {
  this: GitlabRestAPI[F] =>

  // @see: https://docs.gitlab.com/ee/api/pipelines.html#list-project-pipelines
  def getProjectPipelines(
      projectId: EntityId,
      ref: String = null,
      sha: String = null,
      scope: PipelineScope = null,
      status: PipelineStatus = null,
      yamlErrors: Option[Boolean] = None,
      name: String = null,
      username: String = null,
      updatedAfter: ZonedDateTime = null,
      updatedBefore: ZonedDateTime = null,
      sort: Sorting[PipelinesSort] = null,
      paging: Paging = AllPages,
  ): EitherT[F, GitlabError, Vector[PipelineShort]] = {

    val params = Vector(
      wrap(ref).map("ref".eqParam(_)),
      wrap(sha).map("sha".eqParam(_)),
      wrap(name).map("name".eqParam(_)),
      wrap(username).map("username".eqParam(_)),
      yamlErrors.map("yamlErrors".eqParam(_)).toList,
      wrap(scope).map(sc => "scope".eqParam(sc.name)),
      wrap(status).map(st => "status".eqParam(st.name)),
      wrap(updatedAfter).map(_.toISO8601UTC).map("updated_after".eqParam(_)),
      wrap(updatedBefore).map(_.toISO8601UTC).map("updated_before".eqParam(_)),
      wrap(sort).flatMap(s => List("order_by".eqParam(s.field.property), "sort".eqParam(s.direction.toString))),
    ).flatten
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/pipelines", params: _*)
    getAllPaginatedResponse[PipelineShort](req, "get-pipelines-of-project", paging)
  }

  // @see: https://docs.gitlab.com/ee/api/pipelines.html#get-a-single-pipeline
  def getPipeline(projectId: EntityId, pipelineId: BigInt): EitherT[F, GitlabError, PipelineFullInfo] = {
    implicit val rId: RequestId = RequestId.newOne("get-pipeline-by-id")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/pipelines/$pipelineId")
    invokeRequest(req).unmarshall[PipelineFullInfo]
  }

  // @see: https://docs.gitlab.com/ee/api/jobs.html#list-pipeline-jobs
  def getPipelineJobs(
      projectId: EntityId,
      pipelineId: BigInt,
      scope: Set[JobScope] = null,
  ): EitherT[F, GitlabError, Vector[JobFullInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("get-pipeline-jobs")
    val params                  = wrap(scope).flatMap(_.map(sc => "scope[]".eqParam(sc.name)))
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/pipelines/$pipelineId/jobs", params)
    invokeRequest(req).unmarshall[Vector[JobFullInfo]]
  }
}
