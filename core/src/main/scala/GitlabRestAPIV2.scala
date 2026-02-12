package io.gitlab.mateuszjaje.gitlabclient

import apisv2.*
import models.*
import models.SearchScope.MergeRequests
import query.*
import query.ParamQuery.*

import io.circe.Decoder

trait GitlabRestAPIV2[F[_]]
    extends GitlabRestBaseV2[F]
    with EmojiAwardsAPIV2[F]
    with TagsAPIV2[F]
    with MergeRequestsAPIV2[F]
    with EventsAPIV2[F]
    with DeploymentsAPIV2[F]
    with JobsAPIV2[F]
    with PipelinesAPIV2[F]
    with GroupsAPIV2[F]
    with ProjectsAPIV2[F]
    with CommitsAPIV2[F] {

  implicit def m: GitlabApiT[F]

  def config: GitlabConfig

  def getCurrentUser: F[Either[GitlabError, GitlabFullUser]] = {
    implicit val rId: RequestId = RequestId.newOne("get-current-user")
    val req                     = reqGen.get(API + "/user")
    invokeRequest(req).unmarshall[GitlabFullUser]
  }

  def getUserById(id: BigInt): F[Either[GitlabError, GitlabFullUser]] = {
    implicit val rId: RequestId = RequestId.newOne("get-user-by-id")
    val req                     = reqGen.get(API + s"/users/$id")
    invokeRequest(req).unmarshall[GitlabFullUser]
  }

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests
  def globalSearch[T: Decoder](scope: SearchScope, phrase: String): F[Either[GitlabError, Vector[T]]] = {
    val req = reqGen.get(s"$API/search", scope.toParam, search(phrase))
    getAllPaginatedResponse[T](req, s"global-search-${scope.name}", AllPages)
  }

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests-1
  def groupSearch[T: Decoder](groupId: EntityId, scope: SearchScope, phrase: Option[String]): F[Either[GitlabError, Vector[T]]] = {
    val req = reqGen.get(s"$API/groups/${groupId.toStringId}/search", scope.toParam, phrase.map(Search).getOrElse(NoParam))
    getAllPaginatedResponse[T](req, s"group-search-${scope.name}", AllPages)
  }

  def globalMRsSearchRaw[T: Decoder](phrase: String): F[Either[GitlabError, Vector[T]]] =
    globalSearch[T](MergeRequests, phrase)

  def groupSearchMrsRaw[T: Decoder](groupId: EntityId, phrase: String): F[Either[GitlabError, Vector[T]]] =
    groupSearch[T](groupId, SearchScope.MergeRequests, Some(phrase))

  def globalMRsSearch(phrase: String): F[Either[GitlabError, Vector[MergeRequestInfo]]] =
    globalMRsSearchRaw[MergeRequestInfo](phrase)

  def groupSearchMrs(groupId: EntityId, phrase: String): F[Either[GitlabError, Vector[MergeRequestInfo]]] =
    groupSearchMrsRaw[MergeRequestInfo](groupId, phrase)

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-commits-premium-1
  def groupSearchCommits(groupId: EntityId, phrase: String): F[Either[GitlabError, Vector[CommitSimple]]] =
    groupSearch[CommitSimple](groupId, SearchScope.Commits, Some(phrase))

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-commits-premium-1
  def groupSearchBlobs(groupId: EntityId, phrase: String): F[Either[GitlabError, Vector[BlobInfo]]] =
    groupSearch[BlobInfo](groupId, SearchScope.Blobs, Some(phrase))

  //  other

  // @see: https://docs.gitlab.com/ee/api/branches.html#list-repository-branches
  def getBranches(projectID: EntityId, searchTerm: Option[String]): F[Either[GitlabError, Vector[GitlabBranchInfo]]] = {
    val req = reqGen.get(
      s"$API/projects/${projectID.toStringId}/repository/branches",
      projectID,
      searchTerm.map(ParamQuery.search).getOrElse(NoParam),
    )
    getAllPaginatedResponse[GitlabBranchInfo](req, "branches-per-project", AllPages)
  }

  // @see: https://docs.gitlab.com/ee/api/protected_branches.html
  def getProtectedBranches(projectID: EntityId): F[Either[GitlabError, Vector[ProtectedBranchesConfig]]] = {
    val req = reqGen.get(
      s"$API/projects/${projectID.toStringId}/protected_branches",
      projectID,
    )
    getAllPaginatedResponse[ProtectedBranchesConfig](req, "protected-branches-per-project", AllPages)
  }

}
