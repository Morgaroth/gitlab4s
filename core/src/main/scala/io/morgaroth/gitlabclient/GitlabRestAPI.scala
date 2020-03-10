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
    getAllPaginatedResponse[MergeRequestInfo](req, s"global-search-${scope.name}", AllPages)
  }

  // Merge requests

  def globalMRsSearch(phrase: String): GitlabResponseT[Vector[MergeRequestInfo]] = {
    globalSearch(SearchScope.MergeRequests, phrase)
  }

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests-1
  private def groupGlobalSearch(groupId: EntityId, scope: SearchScope, phrase: Option[String])
                               (implicit rId: RequestId) = {
    val req = reqGen.get(s"$API/groups/${groupId.toStringId}/search", scope, phrase.map(Search).getOrElse(NoParam))
    invokeRequest(req)
  }

  def groupSearchMrs(groupId: EntityId, phrase: String): GitlabResponseT[Vector[MergeRequestInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("group-search-mr")
    groupGlobalSearch(groupId, SearchScope.MergeRequests, Some(phrase))
      .unmarshall[Vector[MergeRequestInfo]]
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#list-project-merge-requests
  def getMergeRequests(projectID: EntityId, state: MergeRequestState = MergeRequestStates.All, paging: Paging = AllPages, sort: Option[Sorting[MergeRequestsSort]] = None): GitlabResponseT[Vector[MergeRequestInfo]] = {
    val q = sort.map(s => List("order_by".eqParam(s.field.property), "sort".eqParam(s.direction.toString))).toList.flatten :+ (state: ParamQuery)
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/merge_requests", q)
    getAllPaginatedResponse[MergeRequestInfo](req, "merge-requests-per-project", paging)
  }

  // traverse over all states and fetch merge requests for every state, gitlab doesn't offer search by multiple states
  def getMergeRequests(projectID: EntityId, states: Iterable[MergeRequestState]): GitlabResponseT[Vector[MergeRequestInfo]] = {
    states.toVector.traverse { state =>
      getMergeRequests(projectID, state)
    }.map(_.flatten)
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#list-group-merge-requests
  def getGroupMergeRequests(groupId: EntityId, state: MergeRequestState = MergeRequestStates.All): GitlabResponseT[Vector[MergeRequestInfo]] = {
    val req = reqGen.get(s"$API/groups/${groupId.toStringId}/merge_requests", state)
    getAllPaginatedResponse[MergeRequestInfo](req, "merge-requests-per-group", AllPages)
  }

  // traverse over all states and fetch merge requests for every state, gitlab doesn't offer search by multiple states
  def getGroupMergeRequests(groupId: EntityId, states: Iterable[MergeRequestState]): GitlabResponseT[Vector[MergeRequestInfo]] = {
    states.toVector.traverse { state =>
      getGroupMergeRequests(groupId, state)
    }.map(_.flatten)
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#update-mr
  def updateMergeRequest(projectID: EntityId, mrID: BigInt, updateMrPayload: UpdateMRPayload): GitlabResponseT[MergeRequestInfo] = {
    implicit val rId: RequestId = RequestId.newOne("update-mr")
    val req = reqGen.put(s"$API/projects/${projectID.toStringId}/merge_requests/$mrID", MJson.write(updateMrPayload))
    invokeRequest(req).unmarshall[MergeRequestInfo]
  }

  // award emojis

  // @see: https://docs.gitlab.com/ee/api/award_emoji.html#list-an-awardables-award-emoji
  def getEmojiAwards(projectID: EntityId, scope: AwardableScope, awardableId: BigInt): EitherT[F, GitlabError, Vector[EmojiAward]] = {
    implicit val rId: RequestId = RequestId.newOne(s"get-$scope-awards")
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/$scope/$awardableId/award_emoji")
    invokeRequest(req).unmarshall[Vector[EmojiAward]]
  }

  // @see: https://docs.gitlab.com/ee/api/award_emoji.html#award-a-new-emoji
  def awardEmoji(projectID: EntityId, scope: AwardableScope, awardableId: BigInt, emojiName: String): EitherT[F, GitlabError, EmojiAward] = {
    implicit val rId: RequestId = RequestId.newOne(s"award-$scope-emoji")
    val req = reqGen.post(s"$API/projects/${projectID.toStringId}/$scope/$awardableId/award_emoji", "name".eqParam(emojiName))
    invokeRequest(req).unmarshall[EmojiAward]
  }

  // @see: https://docs.gitlab.com/ee/api/award_emoji.html#delete-an-award-emoji
  def unawardEmoji(projectID: EntityId, scope: AwardableScope, awardableId: BigInt, awardId: BigInt): EitherT[F, GitlabError, Unit] = {
    implicit val rId: RequestId = RequestId.newOne(s"unaward-$scope-emoji")
    val req = reqGen.delete(s"$API/projects/${projectID.toStringId}/$scope/$awardableId/award_emoji/$awardId")
    invokeRequest(req).map(_ => ())
  }

  def getEmojiAwards(mergeRequest: MergeRequestInfo): EitherT[F, GitlabError, Vector[EmojiAward]] = {
    getEmojiAwards(mergeRequest.project_id, AwardableScope.MergeRequests, mergeRequest.iid)
  }

  def awardEmoji(mergeRequest: MergeRequestInfo, emojiName: String): EitherT[F, GitlabError, EmojiAward] = {
    awardEmoji(mergeRequest.project_id, AwardableScope.MergeRequests, mergeRequest.iid, emojiName)
  }

  def unawardEmoji(mergeRequest: MergeRequestInfo, emojiAward: EmojiAward): EitherT[F, GitlabError, Unit] = {
    unawardEmoji(mergeRequest.project_id, AwardableScope.MergeRequests, mergeRequest.iid, emojiAward.id)
  }

  def awardMergeRequestEmoji(projectID: EntityId, mergeRequestIID: BigInt, emojiName: String): EitherT[F, GitlabError, EmojiAward] = {
    awardEmoji(projectID, AwardableScope.MergeRequests, mergeRequestIID, emojiName)
  }

  def getMergeRequestEmoji(projectID: EntityId, mergeRequestIID: BigInt): EitherT[F, GitlabError, Vector[EmojiAward]] = {
    getEmojiAwards(projectID, AwardableScope.MergeRequests, mergeRequestIID)
  }

  // approvals

  // @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-configuration-1
  def getApprovals(projectId: EntityId, mergeRequestIId: BigInt): EitherT[F, GitlabError, MergeRequestApprovals] = {
    implicit val rId: RequestId = RequestId.newOne("get-mr-approvals")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approvals")
    invokeRequest(req).unmarshall[MergeRequestApprovals]
  }

  // merge-request discussions & notes

  // @see: https://docs.gitlab.com/ee/api/notes.html#list-all-merge-request-notes
  def getMergeRequestNotes(projectId: EntityId, mergeRequestIId: BigInt, paging: Paging = AllPages, sort: Option[Sorting[MergeRequestNotesSort]] = None)
  : EitherT[F, GitlabError, Vector[MergeRequestNote]] = {
    val q = sort.map(s => Seq("order_by".eqParam(s.field.property), "sort".eqParam(s.direction.toString))).toList.flatten
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes", q: _*)

    getAllPaginatedResponse[MergeRequestNote](req, "merge-request-notes", paging)
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#list-project-merge-request-discussion-items
  def getMergeRequestDiscussions(projectId: EntityId, mergeRequestIId: BigInt): EitherT[F, GitlabError, Vector[MergeRequestDiscussion]] = {
    implicit val rId: RequestId = RequestId.newOne("get-merge-request-discussions")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions")
    getAllPaginatedResponse[MergeRequestDiscussion](req, "get-merge-request-discussions", AllPages)
  }

  //  other

  def groupSearchCommits(groupId: EntityId, phrase: String): GitlabResponseT[String] = {
    implicit val rId: RequestId = RequestId.newOne("group-search-mr")
    groupGlobalSearch(groupId, SearchScope.Commits, Some(phrase))
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-single-project
  def getProject(projectId: EntityId): GitlabResponseT[ProjectInfo] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-by-id")
    val req = reqGen.get(API + s"/projects/${projectId.toStringId}")
    invokeRequest(req).unmarshall[ProjectInfo]
  }

  def getProjects(ids: Iterable[BigInt]): GitlabResponseT[Vector[ProjectInfo]] = {
    ids.toVector.traverse(x => getProject(x))
  }

  // @see: https://docs.gitlab.com/ee/api/branches.html#list-repository-branches
  def getBranches(projectID: EntityId, searchTerm: Option[String]): GitlabResponseT[Vector[GitlabBranchInfo]] = {
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/repository/branches",
      searchTerm.map(ParamQuery.search).getOrElse(NoParam)
    )
    getAllPaginatedResponse[GitlabBranchInfo](req, "merge-requests-per-project", AllPages)
  }

  private def getAllPaginatedResponse[A: Decoder](req: GitlabRequest, kind: String, paging: Paging): EitherT[F, GitlabError, Vector[A]] = {

    val pageSize = paging match {
      case PageCount(_, pageSize) => pageSize
      case EntitiesCount(count) if count < 100 => count
      case _ => 100
    }

    val entitiesLimit = paging match {
      case PageCount(pagesCount, pageSize) => pageSize * pagesCount
      case EntitiesCount(expectedEntitiesCount) => expectedEntitiesCount
      case _ => Int.MaxValue
    }

    def getAll(pageNo: Int, pageSizeEff: Int, acc: Vector[A]): EitherT[F, GitlabError, Vector[A]] = {
      implicit val rId: RequestId = RequestId.newOne(s"$kind-page-$pageNo")

      val resp = invokeRequestRaw(req.withParams(pageSizeEff.pageSizeParam, pageNo.pageNumParam))

      def nextPageHeaders(headers: Map[String, String]): Option[(Int, Int)] = for {
        nextPageNum <- headers.get("X-Next-Page").filter(_.nonEmpty).map(_.toInt)
        perPage <- headers.get("X-Per-Page").filter(_.nonEmpty).map(_.toInt)
      } yield (nextPageNum, perPage)

      for {
        result <- resp.unmarshall[Vector[A]]
        respHeaders <- resp.map(_.headers)
        currentResult = acc ++ result
        nextPageInfo = nextPageHeaders(respHeaders).map(x => x._1 -> math.min(x._2, entitiesLimit - currentResult.length)).filter(_._2 > 0)
        res <- nextPageInfo.map(p => getAll(p._1, p._2, currentResult)).getOrElse(EitherT.pure[F, GitlabError](currentResult))
      } yield res
    }

    getAll(1, pageSize, Vector.empty)
  }
}