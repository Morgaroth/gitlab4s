package io.morgaroth.gitlabclient.apis

import cats.instances.vector._
import cats.syntax.traverse._
import io.morgaroth.gitlabclient._
import io.morgaroth.gitlabclient.models._
import io.morgaroth.gitlabclient.query.ParamQuery._

trait ProjectsAPI[F[_]] {
  this: GitlabRestAPI[F] =>

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-single-project
  def getProject(projectId: EntityId): GitlabResponseT[ProjectInfo] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-by-id")
    val req                     = reqGen.get(API + s"/projects/${projectId.toStringId}")
    invokeRequest(req).unmarshall[ProjectInfo]
  }

  def getProjects(ids: Iterable[BigInt]): GitlabResponseT[Vector[ProjectInfo]] =
    ids.toVector.traverse(x => getProject(x))

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

  // @see: https://docs.gitlab.com/ee/api/projects.html#edit-project
  def editProject(projectId: EntityId, updates: EditProjectRequest): GitlabResponseT[ProjectInfo] = {
    implicit val rId: RequestId = RequestId.newOne("edit-project")
    val req                     = reqGen.put(API + s"/projects/${projectId.toStringId}", MJson.write(updates))
    invokeRequest(req).unmarshall[ProjectInfo]
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-project-push-rules
  def getPushRules(projectId: EntityId): GitlabResponseT[PushRules] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-push-rules")
    val req                     = reqGen.get(API + s"/projects/${projectId.toStringId}/push_rule")
    invokeRequest(req).unmarshall[PushRules]
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-project-push-rules
  def editPushRules(projectId: EntityId, updates: EditPushRuleRequest): GitlabResponseT[PushRules] = {
    implicit val rId: RequestId = RequestId.newOne("edit-project-push-rules")
    val req                     = reqGen.put(API + s"/projects/${projectId.toStringId}/push_rule", MJson.write(updates))
    invokeRequest(req).unmarshall[PushRules]
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-project-push-rules
  def createPushRules(projectId: EntityId, pushRules: EditPushRuleRequest): GitlabResponseT[PushRules] = {
    implicit val rId: RequestId = RequestId.newOne("create-project-push-rules")
    val req                     = reqGen.post(API + s"/projects/${projectId.toStringId}/push_rule", MJson.write(pushRules))
    invokeRequest(req).unmarshall[PushRules]
  }

}
