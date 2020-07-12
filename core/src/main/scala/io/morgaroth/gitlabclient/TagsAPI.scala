package io.morgaroth.gitlabclient

import cats.data.EitherT
import io.circe.generic.auto._
import io.morgaroth.gitlabclient.models._
import io.morgaroth.gitlabclient.query.ParamQuery._

trait TagsAPI[F[_]] {
  this: GitlabRestAPI[F] =>

  // @see: https://docs.gitlab.com/ee/api/tags.html#list-project-repository-tags
  def getProjectTags(projectId: EntityId,
                     search: String = null,
                     paging: Paging = AllPages,
                     sort: Sorting[TagsSort] = null,
                    ): EitherT[F, GitlabError, Vector[TagInfo]] = {

    val q = Vector(
      wrap(sort).flatMap(_.toParams),
      wrap(search).map("search".eqParam),
    ).flatten
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/tags", q: _*)
    getAllPaginatedResponse[TagInfo](req, "get-tags", paging)
  }

  // @see: https://docs.gitlab.com/ee/api/tags.html#create-a-new-tag
  def createTag(projectId: EntityId, tagName: String, refToTag: String, message: Option[String], description: Option[String])
  : EitherT[F, GitlabError, TagInfo] = {
    implicit val rId: RequestId = RequestId.newOne("create-tag")
    val q = Vector(
      Vector("tag_name".eqParam(tagName), "ref".eqParam(refToTag)),
      message.map("message".eqParam).toList,
      description.map("release_description".eqParam).toList,
    ).flatten
    val req = reqGen.post(s"$API/projects/${projectId.toStringId}/repository/tags", q: _*)
    invokeRequest(req).unmarshall[TagInfo]
  }

  // @see: https://docs.gitlab.com/ee/api/tags.html#get-a-single-repository-tag
  def getTag(projectId: EntityId, tagName: String): EitherT[F, GitlabError, TagInfo] = {
    implicit val rId: RequestId = RequestId.newOne("get-tag")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/tags/$tagName")
    invokeRequest(req).unmarshall[TagInfo]
  }

  // @see: https://docs.gitlab.com/ee/api/tags.html#delete-a-tag
  def deleteTag(projectId: EntityId, tagName: String)
  : EitherT[F, GitlabError, Unit] = {
    implicit val rId: RequestId = RequestId.newOne("delete-tag")
    val req = reqGen.delete(s"$API/projects/${projectId.toStringId}/repository/tags/$tagName")
    invokeRequest(req).map(_ => ())
  }

}
