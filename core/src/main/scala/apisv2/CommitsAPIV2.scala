package io.gitlab.mateuszjaje.gitlabclient
package apisv2

import models.*

import java.time.ZonedDateTime

trait CommitsAPIV2[F[_]] extends CommitsRawAPIV2[F] {
  this: GitlabRestBaseV2[F] =>

  def getCommit(projectId: EntityId, ref: String): F[Either[GitlabError, Commit]] = getCommitRaw[Commit](projectId, ref)

  def getCommitRefs(projectId: EntityId, commitId: String): F[Either[GitlabError, Vector[RefSimpleInfo]]] =
    getCommitRefsRaw[RefSimpleInfo](projectId, commitId)

  def getCommits(
      projectId: EntityId,
      path: String = null,
      ref: String = null,
      since: ZonedDateTime = null,
      until: ZonedDateTime = null,
      paging: Paging = AllPages,
  ): F[Either[GitlabError, Vector[CommitSimple]]] = getCommitsRaw[CommitSimple](projectId, path, ref, since, until, paging)

  def getDiffOfACommit(projectId: EntityId, ref: String): F[Either[GitlabError, Vector[FileDiff]]] =
    getDiffOfACommitRaw[FileDiff](projectId, ref)

  def getMergeRequestsOfCommit(projectId: EntityId, commitSha: String): F[Either[GitlabError, Vector[MergeRequestInfo]]] =
    getMergeRequestsOfCommitRaw[MergeRequestInfo](projectId, commitSha)

  def getCommitsReferences(projectId: EntityId, commitSha: String): F[Either[GitlabError, Vector[CommitReference]]] =
    getCommitsReferencesRaw[CommitReference](projectId, commitSha)

}
