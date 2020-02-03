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
  type GitlabResponseT[A] = EitherT[F, GitlabError, A]

  implicit def m: Monad[F]

  val API = "/api/v4"

  def config: GitlabConfig

  private val reqGen = RequestGenerator(config)

  protected def invokeRequest(request: GitlabRequest)(implicit requestId: RequestId): EitherT[F, GitlabError, String] = {
    invokeRequestRaw(request).map(_.payload)
  }

  protected def invokeRequestRaw(request: GitlabRequest)(implicit requestId: RequestId): EitherT[F, GitlabError, GitlabResponse]

  def getCurrentUser: GitlabResponseT[GitlabFullUser] = {
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

  def globalMRsSearch(phrase: String): GitlabResponseT[Vector[MergeRequestInfo]] = {
    globalSearch(SearchScope.MergeRequests, phrase)
  }

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests-1
  private def groupGlobalSearch(groupId: String, scope: SearchScope, phrase: String)(implicit rId: RequestId) = {
    val req = reqGen.get(s"$API/groups/$groupId/search", scope, Search(phrase))
    invokeRequest(req)
  }

  def groupSearchMrs(groupId: String, phrase: String): GitlabResponseT[Vector[MergeRequestInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("group-search-mr")
    groupGlobalSearch(groupId, SearchScope.MergeRequests, phrase)
      .unmarshall[Vector[MergeRequestInfo]]
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#list-project-merge-requests
  def getMergeRequests(projectID: ProjectID, state: MergeRequestState = MergeRequestStates.All): GitlabResponseT[Vector[MergeRequestInfo]] = {
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/merge_requests", state)
    getAllPaginatedResponse[MergeRequestInfo](req, "merge-requests-per-project")
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#update-mr
  def updateMergeRequest(projectID: ProjectID, mrID: BigInt, updateMrPayload: UpdateMRPayload): GitlabResponseT[MergeRequestInfo] = {
    implicit val rId: RequestId = RequestId.newOne("update-mr")
    val req = reqGen.put(s"$API/projects/${projectID.toStringId}/merge_requests/$mrID", MJson.write(updateMrPayload))
    invokeRequest(req).unmarshall[MergeRequestInfo]
  }

  //  other

  // @see: https://docs.gitlab.com/ee/api/award_emoji.html#list-an-awardables-award-emoji
  def getMergeRequestEmoji(projectID: ProjectID, mergeRequestIID: BigInt): EitherT[F, GitlabError, Vector[EmojiAward]] = {
    getEmojiAwards(projectID, AwardableScope.MergeRequests, mergeRequestIID)
  }

  def groupSearchCommits(groupId: String, phrase: String): GitlabResponseT[String] = {
    implicit val rId: RequestId = RequestId.newOne("group-search-mr")
    groupGlobalSearch(groupId, SearchScope.Commits, phrase)
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-single-project
  def getProject(projectId: ProjectID): GitlabResponseT[ProjectInfo] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-by-id")
    val req = reqGen.get(API + s"/projects/${projectId.toStringId}")
    invokeRequest(req).unmarshall[ProjectInfo]
  }

  def getProjects(ids: Iterable[BigInt]): GitlabResponseT[Vector[ProjectInfo]] = {
    ids.toVector.traverse(x => getProject(x))
  }

  // @see: https://docs.gitlab.com/ee/api/branches.html#list-repository-branches
  def getBranches(projectID: ProjectID, searchTerm: Option[String]): GitlabResponseT[Vector[GitlabBranchInfo]] = {
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/repository/branches",
                         searchTerm.map(ParamQuery.search).getOrElse(NoParam)
                         )
    getAllPaginatedResponse[GitlabBranchInfo](req, "merge-requests-per-project")
  }

  private def getEmojiAwards(projectID: ProjectID, scope: AwardableScope, awardableId: BigInt) = {
    implicit val rId: RequestId = RequestId.newOne(s"get-$scope-awards")
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/$scope/$awardableId/award_emoji")
    invokeRequest(req).unmarshall[Vector[EmojiAward]]
  }

  private def getAllPaginatedResponse[A: Decoder](req: GitlabRequest, kind: String): EitherT[F, GitlabError, Vector[A]] = {
    def getAll(pageNo: Int, pageSizeEff: Int, acc: Vector[A]): EitherT[F, GitlabError, Vector[A]] = {
      implicit val rId: RequestId = RequestId.newOne(s"$kind-page-$pageNo")

      val resp = invokeRequestRaw(req.withParams(pageSizeEff.pageSizeParam, pageNo.pageNumParam))

      def nextPage(headers: Map[String, String]): Option[(Int, Int)] = for {
        nextPageNum <- headers.get("X-Next-Page").filter(_.nonEmpty).map(_.toInt)
        perPage <- headers.get("X-Per-Page").filter(_.nonEmpty).map(_.toInt)
      } yield (nextPageNum, perPage)

      for {
        result <- resp.unmarshall[Vector[A]]
        respHeaders <- resp.map(_.headers)
        res <- nextPage(respHeaders).map(p => getAll(p._1, p._2, acc ++ result)).getOrElse(EitherT.pure[F, GitlabError](acc ++ result))
      } yield res
    }

    getAll(1, 100, Vector.empty)
  }
}