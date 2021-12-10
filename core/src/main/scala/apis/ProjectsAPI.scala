package io.gitlab.mateuszjaje.gitlabclient
package apis

import models._
import query.ParamQuery._

import cats.data.EitherT
import cats.instances.vector._
import cats.syntax.traverse._

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

  // @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-project-level-rules
  def getProjectApprovalRules(projectId: EntityId): EitherT[F, GitlabError, Vector[ProjectApprovalRule]] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-approval-rules")
    val req                     = reqGen.get(API + s"/projects/${projectId.toStringId}/approval_rules")
    invokeRequest(req).unmarshall[Vector[ProjectApprovalRule]]
  }

  //  @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#create-project-level-rule
  def createProjectApprovalRule(
      projectId: EntityId,
      payload: UpsertProjectApprovalRule,
  ): EitherT[F, GitlabError, ProjectApprovalRule] = {
    implicit val rId: RequestId = RequestId.newOne("create-project-approval-rule")
    val req                     = reqGen.post(API + s"/projects/${projectId.toStringId}/approval_rules", MJson.write(payload))
    invokeRequest(req).unmarshall[ProjectApprovalRule]
  }

  //  @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#update-project-level-rule
  def updateProjectApprovalRule(
      projectId: EntityId,
      approvalRuleId: BigInt,
      payload: UpsertProjectApprovalRule,
  ): EitherT[F, GitlabError, ProjectApprovalRule] = {
    implicit val rId: RequestId = RequestId.newOne("update-project-approval-rule")
    val req = reqGen.put(API + s"/projects/${projectId.toStringId}/approval_rules/$approvalRuleId", MJson.write(payload))
    invokeRequest(req).unmarshall[ProjectApprovalRule]
  }

}
