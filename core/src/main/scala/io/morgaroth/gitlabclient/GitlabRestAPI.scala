package io.morgaroth.gitlabclient

import cats.Monad
import cats.data.EitherT
import cats.instances.vector._
import cats.syntax.traverse._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Decoder
import io.circe.generic.auto._
import io.morgaroth.gitlabclient.marshalling.Gitlab4SMarshalling
import io.morgaroth.gitlabclient.models._
import io.morgaroth.gitlabclient.query.ParamQuery._
import io.morgaroth.gitlabclient.query._

import scala.language.{higherKinds, postfixOps}

trait GitlabRestAPI[F[_]] extends LazyLogging with Gitlab4SMarshalling {
  type GitlabResponse[A] = EitherT[F, GitlabError, A]

  implicit def m: Monad[F]

  val API = "/api/v4"

  def config: GitlabConfig

  private val reqGen = RequestGenerator(config)

  protected def invokeRequest(request: GitlabRequest)(implicit requestId: RequestId): EitherT[F, GitlabError, String]

  def getCurrentUser: GitlabResponse[GitlabFullUser] = {
    implicit val rId: RequestId = RequestId.newOne("get-current-user")
    val req = reqGen.get(API + "/user")
    invokeRequest(req).unmarshall[GitlabFullUser]
  }

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests
  private def globalSearch(scope: SearchScope, phrase: String) = {
    val req = reqGen.get(s"$API/search", scope, Search(phrase))
    getAllPaginatedResponse[MergeRequestInfo](req, s"global-search-${scope.name}")
  }

  // Merge requests

  def globalMRsSearch(phrase: String): GitlabResponse[Vector[MergeRequestInfo]] = {
    globalSearch(SearchScope.MergeRequests, phrase)
  }

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests-1
  private def groupGlobalSearch(groupId: String, scope: SearchScope, phrase: String)(implicit rId: RequestId) = {
    val req = reqGen.get(s"$API/groups/$groupId/search", scope, Search(phrase))
    invokeRequest(req)
  }

  def groupSearchMrs(groupId: String, phrase: String): GitlabResponse[Vector[MergeRequestInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("group-search-mr")
    groupGlobalSearch(groupId, SearchScope.MergeRequests, phrase)
      .unmarshall[Vector[MergeRequestInfo]]
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#list-project-merge-requests
  def getMergeRequests(projectID: ProjectID, state: MergeRequestState = MergeRequestStates.All): GitlabResponse[Vector[MergeRequestInfo]] = {
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/merge_requests", state)
    getAllPaginatedResponse[MergeRequestInfo](req, "merge-requests-per-project")
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#update-mr
  def updateMergeRequest(projectID: ProjectID, mrID: BigInt, updateMrPayload: UpdateMRPayload): GitlabResponse[MergeRequestInfo] = {
    implicit val rId: RequestId = RequestId.newOne("update-mr")
    val req = reqGen.put(s"$API/projects/${projectID.toStringId}/merge_requests/$mrID", MJson.write(updateMrPayload))
    invokeRequest(req).unmarshall[MergeRequestInfo]
  }

  //  other

  def groupSearchCommits(groupId: String, phrase: String): GitlabResponse[String] = {
    implicit val rId: RequestId = RequestId.newOne("group-search-mr")
    groupGlobalSearch(groupId, SearchScope.Commits, phrase)
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-single-project
  def getProject(projectId: ProjectID): GitlabResponse[ProjectInfo] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-by-id")
    val req = reqGen.get(API + s"/projects/${projectId.toStringId}")
    invokeRequest(req).unmarshall[ProjectInfo]
  }

  def getProjects(ids: Iterable[BigInt]): GitlabResponse[Vector[ProjectInfo]] = {
    ids.toVector.traverse(x => getProject(x))
  }

  // @see: https://docs.gitlab.com/ee/api/branches.html#list-repository-branches
  def getBranches(projectID: ProjectID, searchTerm: Option[String]): GitlabResponse[Vector[GitlabBranchInfo]] = {
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/repository/branches",
                         searchTerm.map(ParamQuery.search).getOrElse(NoParam)
                         )
    getAllPaginatedResponse[GitlabBranchInfo](req, "merge-requests-per-project")
  }

  private def getAllPaginatedResponse[A: Decoder](req: GitlabRequest, kind: String, pageSize: Int = 100): EitherT[F, GitlabError, Vector[A]] = {
    def getAll(pageNo: Int, acc: Vector[A]): EitherT[F, GitlabError, Vector[A]] = {
      implicit val rId: RequestId = RequestId.newOne(s"$kind-page-$pageNo")
      req.withParams(pageSize.pageSizeParam, pageNo.pageNumParam)
      invokeRequest(req).unmarshall[Vector[A]].flatMap { result =>
        if (result.size == pageSize) {
          getAll(pageNo + 1, acc ++ result)
        } else {
          EitherT.pure(acc ++ result)
        }
      }
    }

    getAll(1, Vector.empty)
  }
}