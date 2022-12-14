package io.gitlab.mateuszjaje.gitlabclient

import apis.*
import helpers.NullableField
import marshalling.Gitlab4SMarshalling
import models.*
import query.ParamQuery.*
import query.*

import cats.Monad
import cats.data.EitherT
import com.typesafe.scalalogging.LazyLogging
import io.circe.Decoder

import java.time.ZonedDateTime

trait GitlabRestAPI[F[_]]
    extends LazyLogging
    with Gitlab4SMarshalling
    with EmojiAwardsAPI[F]
    with TagsAPI[F]
    with MergeRequestsAPI[F]
    with EventsAPI[F]
    with DeploymentsAPI[F]
    with JobsAPI[F]
    with PipelinesAPI[F]
    with ProjectsAPI[F]
    with CommitsAPI[F] {

  type GitlabResponseT[A] = EitherT[F, GitlabError, A]

  implicit def m: Monad[F]

  val API            = "/api/v4"
  val AuthHeaderName = "Private-Token"

  def config: GitlabConfig

  protected val reqGen = RequestGenerator(config)

  protected def invokeRequest(request: GitlabRequest)(implicit requestId: RequestId): EitherT[F, GitlabError, String] =
    invokeRequestRaw(request).map(_.payload)

  protected def invokeRequestRaw(request: GitlabRequest)(implicit requestId: RequestId): EitherT[F, GitlabError, GitlabResponse[String]]

  protected def byteRequest(request: GitlabRequest)(implicit requestId: RequestId): EitherT[F, GitlabError, GitlabResponse[Array[Byte]]]

  def authHeader(req: GitlabRequest) =
    req.extraHeaders.getOrElse(AuthHeaderName, config.tokenForPath(req.projectId))

  def getCurrentUser: GitlabResponseT[GitlabFullUser] = {
    implicit val rId: RequestId = RequestId.newOne("get-current-user")
    val req                     = reqGen.get(API + "/user")
    invokeRequest(req).unmarshall[GitlabFullUser]
  }

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests
  protected def globalSearch(scope: SearchScope, phrase: String) = {
    val req = reqGen.get(s"$API/search", scope.toParam, search(phrase))
    getAllPaginatedResponse[MergeRequestInfo](req, s"global-search-${scope.name}", AllPages)
  }

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests-1
  private def groupGlobalSearch(groupId: EntityId, scope: SearchScope, phrase: Option[String])(implicit rId: RequestId) = {
    val req = reqGen.get(s"$API/groups/${groupId.toStringId}/search", scope.toParam, phrase.map(Search).getOrElse(NoParam))
    invokeRequest(req)
  }

  protected def renderParams(
      myReaction: NullableField[String],
      author: NullableField[EntityId],
      search: NullableField[String],
      state: MergeRequestState,
      updatedBefore: NullableField[ZonedDateTime],
      updatedAfter: NullableField[ZonedDateTime],
      createdBefore: NullableField[ZonedDateTime],
      createdAfter: NullableField[ZonedDateTime],
      withMergeStatusRecheck: NullableField[Boolean],
      sort: NullableField[Sorting[MergeRequestsSort]],
  ): Vector[ParamQuery] = {
    Vector(
      author.toList.map {
        case NumericEntityIdId(id) => "author_id".eqParam(id)
        case StringEntityId(id)    => "author_username".eqParam(id)
      },
      sort.toList.flatMap(s => List("order_by".eqParam(s.field.property), "sort".eqParam(s.direction.toString))),
      myReaction.toList.map("my_reaction_emoji".eqParam),
      updatedBefore.toList.map("updated_before".eqParam),
      updatedAfter.toList.map("updated_after".eqParam),
      createdBefore.toList.map("created_before".eqParam),
      createdAfter.toList.map("created_after".eqParam),
      withMergeStatusRecheck.toList.map("with_merge_status_recheck".eqParam),
      search.toList.map("search".eqParam),
      List(state.toParam),
    ).flatten
  }

  //  other

  def groupSearchCommits(groupId: EntityId, phrase: String): GitlabResponseT[String] = {
    implicit val rId: RequestId = RequestId.newOne("group-search-mr")
    groupGlobalSearch(groupId, SearchScope.Commits, Some(phrase))
  }

  // @see: https://docs.gitlab.com/ee/api/branches.html#list-repository-branches
  def getBranches(projectID: EntityId, searchTerm: Option[String]): GitlabResponseT[Vector[GitlabBranchInfo]] = {
    val req = reqGen.get(
      s"$API/projects/${projectID.toStringId}/repository/branches",
      projectID,
      searchTerm.map(ParamQuery.search).getOrElse(NoParam),
    )
    getAllPaginatedResponse[GitlabBranchInfo](req, "merge-requests-per-project", AllPages)
  }

  protected def getAllPaginatedResponse[A: Decoder](
      req: GitlabRequest,
      kind: String,
      paging: Paging,
  ): EitherT[F, GitlabError, Vector[A]] = {

    val pageSize = paging match {
      case PageCount(_, pageSize)              => pageSize
      case EntitiesCount(count) if count < 100 => count
      case _                                   => 100
    }

    val entitiesLimit = paging match {
      case PageCount(pagesCount, pageSize)      => pageSize * pagesCount
      case EntitiesCount(expectedEntitiesCount) => expectedEntitiesCount
      case _                                    => Int.MaxValue
    }

    def getAll(pageNo: Int, pageSizeEff: Int, acc: Vector[A]): EitherT[F, GitlabError, Vector[A]] = {
      implicit val rId: RequestId = RequestId.newOne(s"$kind-page-$pageNo")

      val resp: EitherT[F, GitlabError, GitlabResponse[String]] = invokeRequestRaw(
        req.withParams(pageSizeEff.pageSizeParam, pageNo.pageNumParam),
      )

      def nextPageHeaders(headers: Map[String, String]): Option[(Int, Int)] = {
        val lowercased = headers.map(x => x._1.toLowerCase -> x._2)
        for {
          nextPageNum <- lowercased.get("x-next-page").filter(_.nonEmpty).map(_.toInt)
          perPage     <- lowercased.get("x-per-page").filter(_.nonEmpty).map(_.toInt)
        } yield (nextPageNum, perPage)
      }

      for {
        result      <- resp.unmarshall[Vector[A]]
        respHeaders <- resp.map(_.headers)
        currentResult = acc ++ result
        nextPageInfo  = nextPageHeaders(respHeaders).map(x => x._1 -> math.min(x._2, entitiesLimit - currentResult.length)).filter(_._2 > 0)
        res <- nextPageInfo.map(p => getAll(p._1, p._2, currentResult)).getOrElse(EitherT.pure[F, GitlabError](currentResult))
      } yield res
    }

    getAll(1, pageSize, Vector.empty)
  }

  def wrap[T](value: T): List[T] = Option(value).toList
}
