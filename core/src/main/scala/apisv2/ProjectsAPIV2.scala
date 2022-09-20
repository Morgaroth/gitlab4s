package io.gitlab.mateuszjaje.gitlabclient
package apisv2

import models.*
import query.ParamQuery.*

import java.time.ZonedDateTime

trait ProjectsAPIV2[F[_]] {
  this: GitlabRestAPIV2[F] =>

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-single-project
  def getProject(projectId: EntityId): F[Either[GitlabError, ProjectInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-by-id")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}", projectId)
    invokeRequest(req).unmarshall[ProjectInfo]
  }

  def getProjects(ids: Iterable[BigInt]): F[Either[GitlabError, Vector[ProjectInfo]]] = {
    val a: Vector[F[Either[GitlabError, ProjectInfo]]] = ids.toVector.map(x => getProject(x))
    m.sequence(a)
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#list-all-projects
  def getProjects(paging: Paging = AllPages, sort: Sorting[ProjectsSort] = null): F[Either[GitlabError, Vector[ZonedDateTime]]] = {
    implicit val rId: RequestId = RequestId.newOne("get-all-projects")
    val q = Vector(
      wrap(sort).flatMap(_.toParams),
      Vector("min_access_level".eqParam("40")),
    ).flatten
    val req = reqGen.get(s"$API/projects", q *)
    getAllPaginatedResponse(req, "get-all-projects", paging)
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#edit-project
  def editProject(projectId: EntityId, updates: EditProjectRequest): F[Either[GitlabError, ProjectInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("edit-project")
    val req                     = reqGen.put(s"$API/projects/${projectId.toStringId}", MJson.write(updates), projectId)
    invokeRequest(req).unmarshall[ProjectInfo]
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-project-push-rules
  def getPushRules(projectId: EntityId): F[Either[GitlabError, PushRules]] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-push-rules")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/push_rule", projectId)
    invokeRequest(req).unmarshall[PushRules]
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-project-push-rules
  def editPushRules(projectId: EntityId, updates: EditPushRuleRequest): F[Either[GitlabError, PushRules]] = {
    implicit val rId: RequestId = RequestId.newOne("edit-project-push-rules")
    val req                     = reqGen.put(s"$API/projects/${projectId.toStringId}/push_rule", MJson.write(updates), projectId)
    invokeRequest(req).unmarshall[PushRules]
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-project-push-rules
  def createPushRules(projectId: EntityId, pushRules: EditPushRuleRequest): F[Either[GitlabError, PushRules]] = {
    implicit val rId: RequestId = RequestId.newOne("create-project-push-rules")
    val req                     = reqGen.post(s"$API/projects/${projectId.toStringId}/push_rule", MJson.write(pushRules), projectId)
    invokeRequest(req).unmarshall[PushRules]
  }

  // @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-project-level-rules
  def getProjectApprovalRules(projectId: EntityId): F[Either[GitlabError, Vector[ProjectApprovalRule]]] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-approval-rules")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/approval_rules", projectId)
    invokeRequest(req).unmarshall[Vector[ProjectApprovalRule]]
  }

  //  @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#create-project-level-rule
  def createProjectApprovalRule(
      projectId: EntityId,
      payload: UpsertProjectApprovalRule,
  ): F[Either[GitlabError, ProjectApprovalRule]] = {
    implicit val rId: RequestId = RequestId.newOne("create-project-approval-rule")
    val req                     = reqGen.post(s"$API/projects/${projectId.toStringId}/approval_rules", MJson.write(payload), projectId)
    invokeRequest(req).unmarshall[ProjectApprovalRule]
  }

  //  @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#update-project-level-rule
  def updateProjectApprovalRule(
      projectId: EntityId,
      approvalRuleId: BigInt,
      payload: UpsertProjectApprovalRule,
  ): F[Either[GitlabError, ProjectApprovalRule]] = {
    implicit val rId: RequestId = RequestId.newOne("update-project-approval-rule")
    val req = reqGen.put(s"$API/projects/${projectId.toStringId}/approval_rules/$approvalRuleId", MJson.write(payload), projectId)
    invokeRequest(req).unmarshall[ProjectApprovalRule]
  }

}
