package io.gitlab.mateuszjaje.gitlabclient
package apisv2

import helpers.CustomDateTimeFormatter.*
import models.*
import query.ParamQuery.*

import java.time.ZonedDateTime

trait PipelinesAPIV2[F[_]] {
  this: GitlabRestAPIV2[F] =>

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
  ): F[Either[GitlabError, Vector[PipelineShort]]] = {

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
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/pipelines", projectId, params *)
    getAllPaginatedResponse[PipelineShort](req, "get-pipelines-of-project", paging)
  }

  // @see: https://docs.gitlab.com/ee/api/pipelines.html#get-a-single-pipeline
  def getPipeline(projectId: EntityId, pipelineId: BigInt): F[Either[GitlabError, PipelineFullInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("get-pipeline-by-id")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/pipelines/$pipelineId", projectId)
    invokeRequest(req).unmarshall[PipelineFullInfo]
  }

  // @see: https://docs.gitlab.com/ee/api/jobs.html#list-pipeline-jobs
  def getPipelineJobs(
      projectId: EntityId,
      pipelineId: BigInt,
      scope: Set[JobScope] = null,
  ): F[Either[GitlabError, Vector[JobFullInfo]]] = {
    implicit val rId: RequestId = RequestId.newOne("get-pipeline-jobs")
    val params                  = wrap(scope).flatMap(_.map(sc => "scope[]".eqParam(sc.name)))
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/pipelines/$pipelineId/jobs", projectId, params)
    invokeRequest(req).unmarshall[Vector[JobFullInfo]]
  }

  // @see: https://docs.gitlab.com/ee/api/pipelines.html#create-a-new-pipeline
  def triggerPipeline(
      projectId: EntityId,
      branch: String,
      vars: Vector[PipelineVar] = Vector.empty,
  ): F[Either[GitlabError, PipelineFullInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("trigger-pipeline")
    val req = reqGen.post(s"$API/projects/${projectId.toStringId}/pipeline", MJson.write(TriggerPipelineRequest(branch, vars)), projectId)
    invokeRequest(req).unmarshall[PipelineFullInfo]
  }

  // @see: https://docs.gitlab.com/ee/api/pipelines.html#get-variables-of-a-pipeline
  def getPipelineVariables(
      projectId: EntityId,
      pipelineId: BigInt,
  ): F[Either[GitlabError, Vector[PipelineVar]]] = {
    implicit val rId: RequestId = RequestId.newOne("get-pipeline-variables")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/pipelines/$pipelineId/variables", projectId)
    invokeRequest(req).unmarshall[Vector[PipelineVar]]
  }

}
