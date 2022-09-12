package io.gitlab.mateuszjaje.gitlabclient

import apisv2.ThisMonad.syntax
import apisv2.ThisMonad.syntax.*
import apisv2.*
import helpers.NullableField
import marshalling.Gitlab4SMarshalling
import models.*
import query.ParamQuery.*
import query.*

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
    with ProjectsAPIV2[F]
    with CommitsAPIV2[F] {

  implicit def m: ThisMonad[F]

  val API            = "/api/v4"
  val AuthHeaderName = "Private-Token"

  def config: GitlabConfig

  protected val reqGen = RequestGenerator(config)

  protected def invokeRequest(request: GitlabRequest)(implicit requestId: RequestId): F[Either[GitlabError, String]] =
    invokeRequestRaw(request).map(_.payload)

  protected def invokeRequestRaw(request: GitlabRequest)(implicit requestId: RequestId): F[Either[GitlabError, GitlabResponse[String]]]

  protected def byteRequest(request: GitlabRequest)(implicit requestId: RequestId): F[Either[GitlabError, GitlabResponse[Array[Byte]]]]

  def authHeader(req: GitlabRequest) =
    req.extraHeaders.getOrElse(AuthHeaderName, config.tokenForPath(req.projectId))

  def getCurrentUser = {
    implicit val rId: RequestId = RequestId.newOne("get-current-user")
    val req                     = reqGen.get(API + "/user")
    invokeRequest(req).unmarshall[GitlabFullUser]
  }

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests
  protected def globalSearch(scope: SearchScope, phrase: String): F[Either[GitlabError, Vector[MergeRequestInfo]]] = {
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

  def groupSearchCommits(groupId: EntityId, phrase: String): F[Either[GitlabError, String]] = {
    implicit val rId: RequestId = RequestId.newOne("group-search-mr")
    groupGlobalSearch(groupId, SearchScope.Commits, Some(phrase))
  }

  // @see: https://docs.gitlab.com/ee/api/branches.html#list-repository-branches
  def getBranches(projectID: EntityId, searchTerm: Option[String]): F[Either[GitlabError, Vector[GitlabBranchInfo]]] = {
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/repository/branches", searchTerm.map(ParamQuery.search).getOrElse(NoParam))
    getAllPaginatedResponse[GitlabBranchInfo](req, "merge-requests-per-project", AllPages)
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

      val resultF: F[Either[GitlabError, Vector[A]]]                = Gitlab4SMarshalling.unmarshallF(resp).unmarshall[Vector[A]]
      val respHeadersF: F[Either[GitlabError, Map[String, String]]] = syntax.toOps(resp).map(_.headers)
      val currentResultF: F[Either[GitlabError, Vector[A]]]         = syntax.toOps(resultF).map(acc ++ _)
      val nextPageInfoF: F[Either[GitlabError, Option[(Int, Int)]]] = syntax.toOps2(respHeadersF).flatMap { respHeaders =>
        syntax.toOps2(currentResultF).map { currentResult =>
          nextPageHeaders(respHeaders).map(x => x._1 -> math.min(x._2, entitiesLimit - currentResult.length)).filter(_._2 > 0)
        }
      }
      val resF: F[Either[GitlabError, Vector[A]]] = syntax.toOps2(nextPageInfoF).flatMap { nextPageInfO =>
        syntax.toOps2(currentResultF).flatMap { currentResult =>
         val a: F[Either[GitlabError, Vector[A]]] = nextPageInfO
            .map(nextPageInfo => getAll(nextPageInfo._1, nextPageInfo._2, currentResult))
            .getOrElse(syntax.pureOps(currentResult).pure)
          a
        }
      }
      resF
//      for {
//        result      <- resultF
//        respHeaders <- syntax.toOps(resp).map(_.headers)
//        currentResult = acc ++ result
//        nextPageInfo  = nextPageHeaders(respHeaders).map(x => x._1 -> math.min(x._2, entitiesLimit - currentResult.length)).filter(_._2 > 0)
//        res <- nextPageInfo.map(p => getAll(p._1, p._2, currentResult)).getOrElse(currentResult.pure)
//      } yield res
    }

    getAll(1, pageSize, Vector.empty)
  }

  def wrap[T](value: T): List[T] = Option(value).toList
}
