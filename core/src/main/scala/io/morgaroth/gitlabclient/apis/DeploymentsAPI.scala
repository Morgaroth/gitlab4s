package io.morgaroth.gitlabclient.apis

import cats.data.EitherT
import io.morgaroth.gitlabclient._
import io.morgaroth.gitlabclient.helpers.CustomDateTimeFormatter._
import io.morgaroth.gitlabclient.models._
import io.morgaroth.gitlabclient.query.ParamQuery._

import java.time.ZonedDateTime

trait DeploymentsAPI[F[_]] {
  this: GitlabRestAPI[F] =>

  // @see: https://docs.gitlab.com/ee/api/deployments.html#list-project-deployments
  def getProjectDeployments(projectId: EntityId,
                            environment: String = null,
                            updatedAfter: ZonedDateTime = null,
                            updatedBefore: ZonedDateTime = null,
                            status: String = null,
                            paging: Paging = AllPages,
                            sort: Sorting[DeploymentsSort] = null,
                           ): EitherT[F, GitlabError, Vector[DeploymentInfo]] = {

    val params = Vector(
      wrap(environment).map("environment".eqParam(_)),
      wrap(status).map("status".eqParam(_)),
      wrap(updatedAfter).map(_.toISO8601UTC).map("updated_after".eqParam(_)),
      wrap(updatedBefore).map(_.toISO8601UTC).map("updated_before".eqParam(_)),
      wrap(sort).flatMap(s => List("order_by".eqParam(s.field.property), "sort".eqParam(s.direction.toString))),
    ).flatten
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/deployments", params: _*)
    getAllPaginatedResponse[DeploymentInfo](req, "get-deployments", paging)
  }

}
