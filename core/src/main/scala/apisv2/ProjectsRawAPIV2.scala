package io.gitlab.mateuszjaje.gitlabclient
package apisv2

import apisv2.GitlabApiT.syntax.toOps
import models.{EditProjectRequest, EditPushRuleRequest, UpsertProjectApprovalRule}
import query.ParamQuery.*

import io.circe.Decoder

trait ProjectsRawAPIV2[F[_]] {
  this: GitlabRestBaseV2[F] =>

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-single-project
  def getProjectRaw[T: Decoder](projectId: EntityId): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-by-id")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}", projectId)
    invokeRequest(req).unmarshall[T]
  }

  def getProjectsRaw[T: Decoder](ids: Iterable[BigInt]): F[Either[GitlabError, Vector[T]]] = {
    val a: Vector[F[Either[GitlabError, T]]] = ids.toVector.map(x => getProjectRaw(x))
    m.sequence(a)
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#list-all-projects
  def getProjectsRaw[T: Decoder](paging: Paging = AllPages, sort: Sorting[ProjectsSort] = null): F[Either[GitlabError, Vector[T]]] = {
    val q = Vector(
      wrap(sort).flatMap(_.toParams),
      Vector("min_access_level".eqParam("40")),
    ).flatten
    val req = reqGen.get(s"$API/projects", q *)
    getAllPaginatedResponse[T](req, "get-all-projects", paging)
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#edit-project
  def editProjectRaw[T: Decoder](projectId: EntityId, updates: EditProjectRequest): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("edit-project")
    val req                     = reqGen.put(s"$API/projects/${projectId.toStringId}", MJson.write(updates), projectId)
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-project-push-rules
  def getPushRulesRaw[T: Decoder](projectId: EntityId): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-push-rules")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/push_rule", projectId)
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-project-push-rules
  def editPushRulesRaw[T: Decoder](projectId: EntityId, updates: EditPushRuleRequest): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("edit-project-push-rules")
    val req                     = reqGen.put(s"$API/projects/${projectId.toStringId}/push_rule", MJson.write(updates), projectId)
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-project-push-rules
  def createPushRulesRaw[T: Decoder](projectId: EntityId, pushRules: EditPushRuleRequest): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("create-project-push-rules")
    val req                     = reqGen.post(s"$API/projects/${projectId.toStringId}/push_rule", MJson.write(pushRules), projectId)
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-project-level-rules
  def getProjectApprovalRulesRaw[T: Decoder](projectId: EntityId): F[Either[GitlabError, Vector[T]]] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-approval-rules")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/approval_rules", projectId)
    invokeRequest(req).unmarshall[Vector[T]]
  }

  //  @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#create-project-level-rule
  def createProjectApprovalRuleRaw[T: Decoder](
      projectId: EntityId,
      payload: UpsertProjectApprovalRule,
  ): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("create-project-approval-rule")
    val req                     = reqGen.post(s"$API/projects/${projectId.toStringId}/approval_rules", MJson.write(payload), projectId)
    invokeRequest(req).unmarshall[T]
  }

  //  @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#update-project-level-rule
  def updateProjectApprovalRuleRaw[T: Decoder](
      projectId: EntityId,
      approvalRuleId: BigInt,
      payload: UpsertProjectApprovalRule,
  ): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("update-project-approval-rule")
    val req = reqGen.put(s"$API/projects/${projectId.toStringId}/approval_rules/$approvalRuleId", MJson.write(payload), projectId)
    invokeRequest(req).unmarshall[T]
  }

  def archiveProject(
      projectId: EntityId,
  ): F[Either[GitlabError, Unit]] = {
    implicit val rId: RequestId = RequestId.newOne("archive-project")
    val req                     = reqGen.post(s"$API/projects/${projectId.toStringId}/archive", projectId)
    invokeRequest(req).map(_ => ())
  }

}
