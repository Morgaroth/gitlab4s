package io.gitlab.mateuszjaje.gitlabclient

import apisv2.*
import apisv2.GitlabApiT.syntax.*
import helpers.NullableField
import marshalling.Gitlab4SMarshalling
import models.MergeRequestState
import query.*
import query.ParamQuery.*

import com.typesafe.scalalogging.LazyLogging
import io.circe.Decoder

import java.time.ZonedDateTime

trait GitlabRestBaseV2[F[_]] extends LazyLogging with Gitlab4SMarshalling {

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

  def genericRequest(path: String, requestCode: String): F[Either[GitlabError, GitlabResponse[String]]] = {
    implicit val rId: RequestId = RequestId.newOne(requestCode)
    val req                     = reqGen.get(API + s"/${path.stripPrefix("/")}")
    invokeRequestRaw(req)
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

}

object GitlabRestBaseV2 {
  def renderParams(
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

}
