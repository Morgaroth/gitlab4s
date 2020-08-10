package io.morgaroth.gitlabclient

import java.time.ZonedDateTime

import cats.Monad
import cats.data.EitherT
import cats.instances.vector._
import cats.syntax.traverse._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Decoder
import io.circe.generic.auto._
import io.morgaroth.gitlabclient.apis._
import io.morgaroth.gitlabclient.marshalling.Gitlab4SMarshalling
import io.morgaroth.gitlabclient.models._
import io.morgaroth.gitlabclient.query.ParamQuery._
import io.morgaroth.gitlabclient.query._

trait GitlabRestAPI[F[_]] extends LazyLogging with Gitlab4SMarshalling
  with EmojiAwardsAPI[F]
  with TagsAPI[F]
  with MergeRequestsAPI[F]
  with EventsAPI[F]
  with DeploymentsAPI[F]
  with JobsAPI[F]
  with CommitsAPI[F] {

  type GitlabResponseT[A] = EitherT[F, GitlabError, A]

  implicit def m: Monad[F]

  val API = "/api/v4"

  def config: GitlabConfig

  protected val reqGen = RequestGenerator(config)

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
  protected def globalSearch(scope: SearchScope, phrase: String) = {
    val req = reqGen.get(s"$API/search", scope.toParam, search(phrase))
    getAllPaginatedResponse[MergeRequestInfo](req, s"global-search-${scope.name}", AllPages)
  }

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests-1
  private def groupGlobalSearch(groupId: EntityId, scope: SearchScope, phrase: Option[String])
                               (implicit rId: RequestId) = {
    val req = reqGen.get(s"$API/groups/${groupId.toStringId}/search", scope.toParam, phrase.map(Search).getOrElse(NoParam))
    invokeRequest(req)
  }

  protected def renderParams(
                              myReaction: String, search: String, state: MergeRequestState,
                              updatedBefore: ZonedDateTime, updatedAfter: ZonedDateTime,
                              createdBefore: ZonedDateTime, createdAfter: ZonedDateTime,
                              sort: Sorting[MergeRequestsSort],
                            ): Vector[ParamQuery] = {
    Vector(
      wrap(sort).flatMap(s => List("order_by".eqParam(s.field.property), "sort".eqParam(s.direction.toString))),
      wrap(myReaction).map("my_reaction_emoji".eqParam),
      wrap(updatedBefore).map("updated_before".eqParam),
      wrap(updatedAfter).map("updated_after".eqParam),
      wrap(createdBefore).map("created_before".eqParam),
      wrap(createdAfter).map("created_after".eqParam),
      wrap(search).map("search".eqParam),
      List(state.toParam),
    ).flatten
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

  // @see: https://docs.gitlab.com/ee/api/projects.html#list-all-projects
  def getProjects(paging: Paging = AllPages, sort: Sorting[ProjectsSort] = null): GitlabResponseT[Vector[ProjectInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("get-all-projects")
    val q = Vector(
      wrap(sort).flatMap(_.toParams),
      Vector("min_access_level".eqParam("40")),
    ).flatten
    val req = reqGen.get(API + s"/projects", q: _*)
    getAllPaginatedResponse(req, "get-all-projects", paging)
  }

  // @see: https://docs.gitlab.com/ee/api/branches.html#list-repository-branches
  def getBranches(projectID: EntityId, searchTerm: Option[String]): GitlabResponseT[Vector[GitlabBranchInfo]] = {
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/repository/branches",
      searchTerm.map(ParamQuery.search).getOrElse(NoParam)
    )
    getAllPaginatedResponse[GitlabBranchInfo](req, "merge-requests-per-project", AllPages)
  }

  protected def getAllPaginatedResponse[A: Decoder](req: GitlabRequest, kind: String, paging: Paging): EitherT[F, GitlabError, Vector[A]] = {

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

  def wrap[T](value: T): List[T] = Option(value).toList
}