package io.gitlab.mateuszjaje.gitlabclient
package apis

import helpers.CustomDateTimeFormatter.RichZonedDateTime
import models.{Commit, CommitReference, CommitSimple, FileDiff, MergeRequestInfo, RefSimpleInfo}
import query.ParamQuery.*

import cats.data.EitherT

import java.time.ZonedDateTime

trait CommitsAPI[F[_]] {
  this: GitlabRestAPI[F] =>

  // @see: https://docs.gitlab.com/ee/api/commits.html#get-a-single-commit
  def getCommit(projectId: EntityId, ref: String): EitherT[F, GitlabError, Commit] = {
    implicit val rId: RequestId = RequestId.newOne("get-single-commit")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$ref", projectId)
    invokeRequest(req).unmarshall[Commit]
  }

  // @see: https://docs.gitlab.com/ee/api/commits.html#get-references-a-commit-is-pushed-to
  def getCommitRefs(projectId: EntityId, commitId: String): EitherT[F, GitlabError, Vector[RefSimpleInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("get-refs-of-a-commit")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$commitId/refs", projectId)
    invokeRequest(req).unmarshall[Vector[RefSimpleInfo]]
  }

  def getCommits(
      projectId: EntityId,
      path: String = null,
      ref: String = null,
      since: ZonedDateTime = null,
      until: ZonedDateTime = null,
      paging: Paging = AllPages,
  ): EitherT[F, GitlabError, Vector[CommitSimple]] = {
    val params = Vector(
      wrap(ref).map("ref_name".eqParam(_)),
      wrap(path).map("path".eqParam(_)),
      wrap(since).map(_.toISO8601UTC).map("since".eqParam(_)),
      wrap(until).map(_.toISO8601UTC).map("until".eqParam(_)),
    ).flatten
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits", projectId, params *)
    getAllPaginatedResponse[CommitSimple](req, "get-commits", paging)
  }

  // @see: https://docs.gitlab.com/ee/api/commits.html#get-the-diff-of-a-commit
  def getDiffOfACommit(projectId: EntityId, ref: String): EitherT[F, GitlabError, Vector[FileDiff]] = {
    implicit val rId: RequestId = RequestId.newOne("get-commits-diff")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$ref/diff", projectId)
    invokeRequest(req).unmarshall[Vector[FileDiff]]
  }

  // @see: https://docs.gitlab.com/ee/api/commits.html#list-merge-requests-associated-with-a-commit
  def getMergeRequestsOfCommit(projectId: EntityId, commitSha: String): EitherT[F, GitlabError, Vector[MergeRequestInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("get-commit-merge-requests")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$commitSha/merge_requests", projectId)
    invokeRequest(req).unmarshall[Vector[MergeRequestInfo]]
  }

  // @see: https://docs.gitlab.com/ee/api/commits.html#get-references-a-commit-is-pushed-to
  def getCommitsReferences(projectId: EntityId, commitSha: String): EitherT[F, GitlabError, Vector[CommitReference]] = {
    implicit val rId: RequestId = RequestId.newOne("get-commit-references")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$commitSha/refs", projectId)
    invokeRequest(req).unmarshall[Vector[CommitReference]]
  }

}
