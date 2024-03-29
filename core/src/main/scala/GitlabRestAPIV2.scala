package io.gitlab.mateuszjaje.gitlabclient

import apisv2.*
import apisv2.GitlabApiT.syntax.*
import helpers.NullableField
import marshalling.Gitlab4SMarshalling
import models.*
import query.*
import query.ParamQuery.*

import com.typesafe.scalalogging.LazyLogging
import io.circe.Decoder

import java.time.ZonedDateTime

trait GitlabRestAPIV2[F[_]]
    extends LazyLogging
    with Gitlab4SMarshalling
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

  val API            = "/api/v4"
  val AuthHeaderName = "Private-Token"

  def config: GitlabConfig

  protected val reqGen = RequestGenerator(config)

  protected def invokeRequest(request: GitlabRequest)(implicit requestId: RequestId): F[Either[GitlabError, String]] =
    invokeRequestRaw(request).map(_.payload)

  protected def invokeRequestRaw(request: GitlabRequest)(implicit requestId: RequestId): F[Either[GitlabError, GitlabResponse[String]]]

  protected def byteRequest(request: GitlabRequest)(implicit requestId: RequestId): F[Either[GitlabError, GitlabResponse[Array[Byte]]]]

  def authHeader(req: GitlabRequest): String =
    req.extraHeaders.getOrElse(AuthHeaderName, config.tokenForPath(req.projectId))

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

  def genericRequest(path: String, requestCode: String): F[Either[GitlabError, GitlabResponse[String]]] = {
    implicit val rId: RequestId = RequestId.newOne(requestCode)
    val req                     = reqGen.get(API + s"/${path.stripPrefix("/")}")
    invokeRequestRaw(req)
  }

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests
  protected def globalSearch(scope: SearchScope, phrase: String): F[Either[GitlabError, Vector[MergeRequestInfo]]] = {
    val req = reqGen.get(s"$API/search", scope.toParam, search(phrase))
    getAllPaginatedResponse[MergeRequestInfo](req, s"global-search-${scope.name}", AllPages)
  }

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-commits-premium-1
  def groupSearchCommits(groupId: EntityId, phrase: String): F[Either[GitlabError, Vector[CommitSimple]]] =
    groupGlobalSearch[CommitSimple](groupId, SearchScope.Commits, Some(phrase))

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-commits-premium-1
  def groupSearchBlobs(groupId: EntityId, phrase: String): F[Either[GitlabError, Vector[BlobInfo]]] =
    groupGlobalSearch[BlobInfo](groupId, SearchScope.Blobs, Some(phrase))

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests-1
  private def groupGlobalSearch[T: Decoder](groupId: EntityId, scope: SearchScope, phrase: Option[String]) = {
    val req = reqGen.get(s"$API/groups/${groupId.toStringId}/search", scope.toParam, phrase.map(Search).getOrElse(NoParam))
    getAllPaginatedResponse[T](req, "global-commits-search", AllPages)
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

  protected def getAllPaginatedResponse[A: Decoder](
      req: GitlabRequest,
      kind: String,
      paging: Paging,
  ): F[Either[GitlabError, Vector[A]]] = {

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

    def getAll(pageNo: Int, pageSizeEff: Int, acc: Vector[A]): F[Either[GitlabError, Vector[A]]] = {
      implicit val rId: RequestId = RequestId.newOne(s"$kind-page-$pageNo")

      val resp: F[Either[GitlabError, GitlabResponse[String]]] = invokeRequestRaw(
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
        res <- nextPageInfo.map(p => getAll(p._1, p._2, currentResult)).getOrElse(currentResult.pure)
      } yield res

    }

    getAll(1, pageSize, Vector.empty)
  }

  def wrap[T](value: T): List[T] = Option(value).toList
}
