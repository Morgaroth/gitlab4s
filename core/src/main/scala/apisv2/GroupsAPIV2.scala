package io.gitlab.mateuszjaje.gitlabclient
package apisv2

import models.*
import query.ParamQuery.*

trait GroupsAPIV2[F[_]] {
  this: GitlabRestAPIV2[F] =>

  // @see: https://docs.gitlab.com/ee/api/groups.html#list-a-groups-projects
  def getGroupProjects(
      groupId: EntityId,
      archived: Option[Boolean] = None,
      search: Option[String] = None,
      includeSubgroups: Option[Boolean] = None,
      sort: Sorting[ProjectsSort] = null,
      paging: Paging = AllPages,
  ): F[Either[GitlabError, Vector[ProjectInfo]]] = {
    val params = Vector(
      archived.map("archived".eqParam(_)),
      search.map("search".eqParam(_)),
      includeSubgroups.map("include_subgroups".eqParam(_)),
      wrap(sort).flatMap(_.toParams),
    ).flatten
    val req = reqGen.get(s"$API/groups/${groupId.toStringId}/projects", groupId, params *)
    getAllPaginatedResponse[ProjectInfo](req, "get-group-projects", paging)
  }

}
