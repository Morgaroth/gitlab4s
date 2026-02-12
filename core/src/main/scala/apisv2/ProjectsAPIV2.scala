package io.gitlab.mateuszjaje.gitlabclient
package apisv2

import models.*

trait ProjectsAPIV2[F[_]] extends ProjectsRawAPIV2[F] {
  this: GitlabRestBaseV2[F] =>

  def getProject(projectId: EntityId): F[Either[GitlabError, ProjectInfo]] = getProjectRaw[ProjectInfo](projectId)

  def getProjects(ids: Iterable[BigInt]): F[Either[GitlabError, Vector[ProjectInfo]]] = getProjectsRaw(ids)

  def getProjects(paging: Paging = AllPages, sort: Sorting[ProjectsSort] = null): F[Either[GitlabError, Vector[ProjectInfo]]] =
    getProjectsRaw[ProjectInfo](paging, sort)

  def editProject(projectId: EntityId, updates: EditProjectRequest): F[Either[GitlabError, ProjectInfo]] =
    editProjectRaw[ProjectInfo](projectId, updates)

  def getPushRules(projectId: EntityId): F[Either[GitlabError, PushRules]] = getPushRulesRaw[PushRules](projectId)

  def editPushRules(projectId: EntityId, updates: EditPushRuleRequest): F[Either[GitlabError, PushRules]] =
    editPushRulesRaw[PushRules](projectId, updates)

  def createPushRules(projectId: EntityId, pushRules: EditPushRuleRequest): F[Either[GitlabError, PushRules]] =
    createPushRulesRaw[PushRules](projectId, pushRules)

  def getProjectApprovalRules(projectId: EntityId): F[Either[GitlabError, Vector[ProjectApprovalRule]]] =
    getProjectApprovalRulesRaw[ProjectApprovalRule](projectId)

  def createProjectApprovalRule(projectId: EntityId, payload: UpsertProjectApprovalRule): F[Either[GitlabError, ProjectApprovalRule]] =
    createProjectApprovalRuleRaw[ProjectApprovalRule](projectId, payload)

  def updateProjectApprovalRule(
      projectId: EntityId,
      approvalRuleId: BigInt,
      payload: UpsertProjectApprovalRule,
  ): F[Either[GitlabError, ProjectApprovalRule]] =
    updateProjectApprovalRuleRaw[ProjectApprovalRule](projectId, approvalRuleId, payload)

}
