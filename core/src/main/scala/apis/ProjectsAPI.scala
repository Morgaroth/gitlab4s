package io.gitlab.mateuszjaje.gitlabclient
package apis

import models.*
import query.ParamQuery.*

import cats.data.EitherT
import cats.instances.vector.*
import cats.syntax.traverse.*

trait ProjectsAPI[F[_]] {
  this: GitlabRestAPI[F] =>

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-single-project
  def getProject(projectId: EntityId): EitherT[F, GitlabError, ProjectInfo] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-by-id")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}", projectId)
    invokeRequest(req).unmarshall[ProjectInfo]
  }

  def getProjects(ids: Iterable[BigInt]): EitherT[F, GitlabError, Vector[ProjectInfo]] =
    ids.toVector.traverse(x => getProject(x))

  // @see: https://docs.gitlab.com/ee/api/projects.html#list-all-projects
  def getProjects(paging: Paging = AllPages, sort: Sorting[ProjectsSort] = null): EitherT[F, GitlabError, Vector[ProjectInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("get-all-projects")
    val q = Vector(
      wrap(sort).flatMap(_.toParams),
      Vector("min_access_level".eqParam("40")),
    ).flatten
    val req = reqGen.get(s"$API/projects", q *)
    getAllPaginatedResponse(req, "get-all-projects", paging)
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#edit-project
  def editProject(projectId: EntityId, updates: EditProjectRequest): EitherT[F, GitlabError, ProjectInfo] = {
    implicit val rId: RequestId = RequestId.newOne("edit-project")
    val req                     = reqGen.put(s"$API/projects/${projectId.toStringId}", MJson.write(updates), projectId)
    invokeRequest(req).unmarshall[ProjectInfo]
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-project-push-rules
  def getPushRules(projectId: EntityId): EitherT[F, GitlabError, PushRules] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-push-rules")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/push_rule", projectId)
    invokeRequest(req).unmarshall[PushRules]
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-project-push-rules
  def editPushRules(projectId: EntityId, updates: EditPushRuleRequest): EitherT[F, GitlabError, PushRules] = {
    implicit val rId: RequestId = RequestId.newOne("edit-project-push-rules")
    val req                     = reqGen.put(s"$API/projects/${projectId.toStringId}/push_rule", MJson.write(updates), projectId)
    invokeRequest(req).unmarshall[PushRules]
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-project-push-rules
  def createPushRules(projectId: EntityId, pushRules: EditPushRuleRequest): EitherT[F, GitlabError, PushRules] = {
    implicit val rId: RequestId = RequestId.newOne("create-project-push-rules")
    val req                     = reqGen.post(s"$API/projects/${projectId.toStringId}/push_rule", MJson.write(pushRules), projectId)
    invokeRequest(req).unmarshall[PushRules]
  }

  // @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-project-level-rules
  def getProjectApprovalRules(projectId: EntityId): EitherT[F, GitlabError, Vector[ProjectApprovalRule]] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-approval-rules")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/approval_rules", projectId)
    invokeRequest(req).unmarshall[Vector[ProjectApprovalRule]]
  }

  //  @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#create-project-level-rule
  def createProjectApprovalRule(
      projectId: EntityId,
      payload: UpsertProjectApprovalRule,
  ): EitherT[F, GitlabError, ProjectApprovalRule] = {
    implicit val rId: RequestId = RequestId.newOne("create-project-approval-rule")
    val req                     = reqGen.post(s"$API/projects/${projectId.toStringId}/approval_rules", MJson.write(payload), projectId)
    invokeRequest(req).unmarshall[ProjectApprovalRule]
  }

  //  @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#update-project-level-rule
  def updateProjectApprovalRule(
      projectId: EntityId,
      approvalRuleId: BigInt,
      payload: UpsertProjectApprovalRule,
  ): EitherT[F, GitlabError, ProjectApprovalRule] = {
    implicit val rId: RequestId = RequestId.newOne("update-project-approval-rule")
    val req = reqGen.put(s"$API/projects/${projectId.toStringId}/approval_rules/$approvalRuleId", MJson.write(payload), projectId)
    invokeRequest(req).unmarshall[ProjectApprovalRule]
  }

}
