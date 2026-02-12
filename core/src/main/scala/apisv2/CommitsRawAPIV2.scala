package io.gitlab.mateuszjaje.gitlabclient
package apisv2

import helpers.CustomDateTimeFormatter.RichZonedDateTime
import query.ParamQuery.*

import io.circe.Decoder

import java.time.ZonedDateTime

trait CommitsRawAPIV2[F[_]] {
  this: GitlabRestBaseV2[F] =>

  // @see: https://docs.gitlab.com/ee/api/commits.html#get-a-single-commit
  def getCommitRaw[T: Decoder](projectId: EntityId, ref: String): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("get-single-commit")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$ref", projectId)
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/commits.html#get-references-a-commit-is-pushed-to
  def getCommitRefsRaw[T: Decoder](projectId: EntityId, commitId: String): F[Either[GitlabError, Vector[T]]] = {
    implicit val rId: RequestId = RequestId.newOne("get-refs-of-a-commit")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$commitId/refs", projectId)
    invokeRequest(req).unmarshall[Vector[T]]
  }

  def getCommitsRaw[T: Decoder](
      projectId: EntityId,
      path: String = null,
      ref: String = null,
      since: ZonedDateTime = null,
      until: ZonedDateTime = null,
      paging: Paging = AllPages,
  ): F[Either[GitlabError, Vector[T]]] = {
    val params = Vector(
      wrap(ref).map("ref_name".eqParam(_)),
      wrap(path).map("path".eqParam(_)),
      wrap(since).map(_.toISO8601UTC).map("since".eqParam(_)),
      wrap(until).map(_.toISO8601UTC).map("until".eqParam(_)),
    ).flatten
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits", projectId, params *)
    getAllPaginatedResponse[T](req, "get-commits", paging)
  }

  // @see: https://docs.gitlab.com/ee/api/commits.html#get-the-diff-of-a-commit
  def getDiffOfACommitRaw[T: Decoder](projectId: EntityId, ref: String): F[Either[GitlabError, Vector[T]]] = {
    implicit val rId: RequestId = RequestId.newOne("get-commits-diff")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$ref/diff", projectId)
    invokeRequest(req).unmarshall[Vector[T]]
  }

  // @see: https://docs.gitlab.com/ee/api/commits.html#list-merge-requests-associated-with-a-commit
  def getMergeRequestsOfCommitRaw[T: Decoder](projectId: EntityId, commitSha: String): F[Either[GitlabError, Vector[T]]] = {
    implicit val rId: RequestId = RequestId.newOne("get-commit-merge-requests")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$commitSha/merge_requests", projectId)
    invokeRequest(req).unmarshall[Vector[T]]
  }

  // @see: https://docs.gitlab.com/ee/api/commits.html#get-references-a-commit-is-pushed-to
  def getCommitsReferencesRaw[T: Decoder](projectId: EntityId, commitSha: String): F[Either[GitlabError, Vector[T]]] = {
    implicit val rId: RequestId = RequestId.newOne("get-commit-references")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$commitSha/refs", projectId)
    invokeRequest(req).unmarshall[Vector[T]]
  }

}
