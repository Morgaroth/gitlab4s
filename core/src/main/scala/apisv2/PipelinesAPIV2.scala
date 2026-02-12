package io.gitlab.mateuszjaje.gitlabclient
package apisv2

import models.*

import java.time.ZonedDateTime

trait PipelinesAPIV2[F[_]] extends PipelinesRawAPIV2[F] {
  this: GitlabRestBaseV2[F] =>

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
    getProjectPipelinesRaw[PipelineShort](
      projectId,
      ref,
      sha,
      scope,
      status,
      yamlErrors,
      name,
      username,
      updatedAfter,
      updatedBefore,
      sort,
      paging,
    )
  }

  def getPipeline(projectId: EntityId, pipelineId: BigInt): F[Either[GitlabError, PipelineFullInfo]] =
    getPipelineRaw[PipelineFullInfo](projectId, pipelineId)

  def getPipelineJobs(
      projectId: EntityId,
      pipelineId: BigInt,
      scope: Set[JobScope] = null,
      paging: Paging = AllPages,
  ): F[Either[GitlabError, Vector[JobFullInfo]]] = getPipelineJobsRaw[JobFullInfo](projectId, pipelineId, scope, paging)

  def triggerPipeline(
      projectId: EntityId,
      branch: String,
      vars: Vector[PipelineVar] = Vector.empty,
  ): F[Either[GitlabError, PipelineFullInfo]] = triggerPipelineRaw[PipelineFullInfo](projectId, branch, vars)

  def getPipelineVariables(projectId: EntityId, pipelineId: BigInt): F[Either[GitlabError, Vector[PipelineVar]]] =
    getPipelineVariablesRaw[PipelineVar](projectId, pipelineId)

}
