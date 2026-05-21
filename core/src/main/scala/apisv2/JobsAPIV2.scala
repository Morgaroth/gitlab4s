package io.gitlab.mateuszjaje.gitlabclient
package apisv2

import models.*

trait JobsAPIV2[F[_]] extends JobsRawAPIV2[F] {
  this: GitlabRestAPIV2[F] =>

  // @see: https://docs.gitlab.com/ee/api/jobs.html#get-a-single-job
  def getJob(projectId: EntityId, jobId: BigInt): F[Either[GitlabError, JobFullInfo]] =
    getJobRaw[JobFullInfo](projectId, jobId)

  // @see: https://docs.gitlab.com/ee/api/jobs.html#cancel-a-job
  def cancelJob(projectId: EntityId, jobId: BigInt): F[Either[GitlabError, JobFullInfo]] =
    cancelJobRaw[JobFullInfo](projectId, jobId)

  // @see: https://docs.gitlab.com/ee/api/jobs.html#cancel-a-job
  def retryJob(projectId: EntityId, jobId: BigInt): F[Either[GitlabError, JobFullInfo]] =
    retryJobRaw[JobFullInfo](projectId, jobId)

  // @see: https://docs.gitlab.com/api/jobs/#run-a-job
  def runJob(projectId: EntityId, jobId: BigInt, params: TriggerJobParam*): F[Either[GitlabError, JobFullInfo]] =
    runJobRaw[JobFullInfo](projectId, jobId, params: _*)

  // @see: https://docs.gitlab.com/ee/api/jobs.html#list-pipeline-bridges
  def getPipelineBridges(
      projectId: EntityId,
      pipelineId: BigInt,
      scopes: Seq[PipelineStatus] = Seq.empty,
  ): F[Either[GitlabError, Vector[PipelineBridgeJob]]] =
    getPipelineBridgesRaw[PipelineBridgeJob](projectId, pipelineId, scopes)

}
