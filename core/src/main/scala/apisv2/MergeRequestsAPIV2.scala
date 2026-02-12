package io.gitlab.mateuszjaje.gitlabclient
package apisv2

import helpers.{NullValue, NullableField}
import models.*

import java.time.ZonedDateTime

trait MergeRequestsAPIV2[F[_]] extends MergeRequestsRawAPIV2[F] {
  this: GitlabRestBaseV2[F] =>

  def getMergeRequests(
      projectID: EntityId,
      state: MergeRequestState = MergeRequestStates.All,
      author: NullableField[EntityId] = NullValue,
      search: NullableField[String] = NullValue,
      myReaction: NullableField[String] = NullValue,
      createdBefore: NullableField[ZonedDateTime] = NullValue,
      createdAfter: NullableField[ZonedDateTime] = NullValue,
      updatedBefore: NullableField[ZonedDateTime] = NullValue,
      updatedAfter: NullableField[ZonedDateTime] = NullValue,
      withMergeStatusRecheck: NullableField[Boolean] = NullValue,
      paging: Paging = AllPages,
      sort: NullableField[Sorting[MergeRequestsSort]] = NullValue,
  ): F[Either[GitlabError, Vector[MergeRequestInfo]]] = getMergeRequestsRaw[MergeRequestInfo](
    projectID,
    state,
    author,
    search,
    myReaction,
    createdBefore,
    createdAfter,
    updatedBefore,
    updatedAfter,
    withMergeStatusRecheck,
    paging,
    sort,
  )

  // traverse over all states and fetch merge requests for every state, gitlab doesn't offer search by multiple states
  def getMergeRequests(projectID: EntityId, states: Iterable[MergeRequestState]): F[Either[GitlabError, Vector[MergeRequestInfo]]] = {
    val vector: Vector[F[Either[GitlabError, Vector[MergeRequestInfo]]]] = states.toVector.map(state => getMergeRequests(projectID, state))
    val value1                                                           = m.sequence(vector)
    val value: F[Either[GitlabError, Vector[Vector[MergeRequestInfo]]]]  = value1
    GitlabApiT.syntax.toOps(value).map(_.flatten)
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#list-group-merge-requests
  def getGroupMergeRequests(
      groupId: EntityId,
      state: MergeRequestState = MergeRequestStates.All,
      search: NullableField[String] = NullValue,
      author: NullableField[EntityId] = NullValue,
      myReaction: NullableField[String] = NullValue,
      updatedBefore: NullableField[ZonedDateTime] = NullValue,
      updatedAfter: NullableField[ZonedDateTime] = NullValue,
      createdBefore: NullableField[ZonedDateTime] = NullValue,
      createdAfter: NullableField[ZonedDateTime] = NullValue,
      withMergeStatusRecheck: NullableField[Boolean] = NullValue,
      paging: Paging = AllPages,
      sort: NullableField[Sorting[MergeRequestsSort]] = NullValue,
  ): F[Either[GitlabError, Vector[MergeRequestInfo]]] = getGroupMergeRequestsRaw[MergeRequestInfo](
    groupId,
    state,
    search,
    author,
    myReaction,
    updatedBefore,
    updatedAfter,
    createdBefore,
    createdAfter,
    withMergeStatusRecheck,
    paging,
    sort,
  )

  def getGroupMergeRequests(groupId: EntityId, states: Iterable[MergeRequestState]): F[Either[GitlabError, Vector[MergeRequestInfo]]] =
    getGroupMergeRequestsRaw[MergeRequestInfo](groupId, states)

  def updateMergeRequest(projectID: EntityId, mrId: BigInt, updateMrPayload: UpdateMRPayload): F[Either[GitlabError, MergeRequestFull]] =
    updateMergeRequestRaw[MergeRequestFull](projectID, mrId, updateMrPayload)

  def getMergeRequest(projectID: EntityId, mrId: BigInt): F[Either[GitlabError, MergeRequestFull]] =
    getMergeRequestRaw[MergeRequestFull](projectID, mrId)

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#get-single-mr-changes
  def getMergeRequestDiff(projectID: EntityId, mrId: BigInt): F[Either[GitlabError, MergeRequestFull]] =
    getMergeRequestDiffRaw[MergeRequestFull](projectID, mrId)

  // approvals

  // @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-configuration-1
  def getApprovals(projectId: EntityId, mergeRequestIId: BigInt): F[Either[GitlabError, MergeRequestApprovals]] =
    getApprovalsRaw[MergeRequestApprovals](projectId, mergeRequestIId)

  def getApprovals(mergeRequest: MergeRequestID): F[Either[GitlabError, MergeRequestApprovals]] =
    getApprovals(mergeRequest.project_id, mergeRequest.iid)

  //  @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#approve-merge-request
  def approveMergeRequest(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      headSha: Option[String] = None,
  ): F[Either[GitlabError, MergeRequestApprovals]] = approveMergeRequestRaw[MergeRequestApprovals](projectId, mergeRequestIId, headSha)

  def approveMergeRequest(
      mergeRequest: MergeRequestID,
      headSha: Option[String],
  ): F[Either[GitlabError, MergeRequestApprovals]] = approveMergeRequest(mergeRequest.project_id, mergeRequest.iid, headSha)

  //  @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#unapprove-merge-request
  def unapproveMergeRequest(
      projectId: EntityId,
      mergeRequestIId: BigInt,
  ): F[Either[GitlabError, MergeRequestApprovals]] = unapproveMergeRequestRaw[MergeRequestApprovals](projectId, mergeRequestIId)

  // @see https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-the-approval-state-of-merge-requests
  def getMergeRequestApprovalRules(
      projectId: EntityId,
      mergeRequestIId: BigInt,
  ): F[Either[GitlabError, Vector[MergeRequestApprovalRule]]] =
    getMergeRequestApprovalRulesRaw[MergeRequestApprovalRule](projectId, mergeRequestIId)

  // @see https://docs.gitlab.com/ee/api/merge_request_approvals.html#create-merge-request-level-rule
  def createApprovalRule(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      payload: CreateMergeRequestApprovalRule,
  ): F[Either[GitlabError, MergeRequestApprovalRule]] = createApprovalRuleRaw[MergeRequestApprovalRule](projectId, mergeRequestIId, payload)

  def createApprovalRule(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      name: String,
      userIds: Vector[BigInt],
  ): F[Either[GitlabError, MergeRequestApprovalRule]] =
    createApprovalRule(projectId, mergeRequestIId, CreateMergeRequestApprovalRule.oneOf(name, userIds *))

  // merge-request discussions & notes

  // @see: https://docs.gitlab.com/ee/api/notes.html#list-all-merge-request-notes
  def getMergeRequestNotes(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      paging: Paging = AllPages,
      sort: Option[Sorting[MergeRequestNotesSort]] = None,
  ): F[Either[GitlabError, Vector[MergeRequestNote]]] = getMergeRequestNotesRaw[MergeRequestNote](projectId, mergeRequestIId, paging, sort)

  def getMergeRequestNotes(mr: MergeRequestID): F[Either[GitlabError, Vector[MergeRequestNote]]] =
    getMergeRequestNotes(mr.project_id, mr.iid)

  def getMergeRequestNotes(
      mr: MergeRequestID,
      paging: Paging,
      sort: Option[Sorting[MergeRequestNotesSort]],
  ): F[Either[GitlabError, Vector[MergeRequestNote]]] =
    getMergeRequestNotes(mr.project_id, mr.iid, paging, sort)

  // @see: https://docs.gitlab.com/ee/api/notes.html#get-single-merge-request-note
  def getMergeRequestNote(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      noteId: BigInt,
  ): F[Either[GitlabError, MergeRequestNote]] = getMergeRequestNoteRaw[MergeRequestNote](projectId, mergeRequestIId, noteId)

  // @see: https://docs.gitlab.com/ee/api/notes.html#create-new-merge-request-note
  def createMergeRequestNote(projectId: EntityId, mergeRequestIId: BigInt, body: String): F[Either[GitlabError, MergeRequestNote]] =
    createMergeRequestNoteRaw[MergeRequestNote](projectId, mergeRequestIId, body)

  // @see: https://docs.gitlab.com/ee/api/notes.html#modify-existing-merge-request-note
  def updateMergeRequestNote(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      noteId: BigInt,
      newBody: String,
  ): F[Either[GitlabError, MergeRequestNote]] = updateMergeRequestNoteRaw[MergeRequestNote](projectId, mergeRequestIId, noteId, newBody)

  // @see: https://docs.gitlab.com/ee/api/discussions.html#list-project-merge-request-discussion-items
  def getMergeRequestDiscussions(projectId: EntityId, mergeRequestIId: BigInt): F[Either[GitlabError, Vector[MergeRequestDiscussion]]] =
    getMergeRequestDiscussionsRaw[MergeRequestDiscussion](projectId, mergeRequestIId)

  // @see: https://docs.gitlab.com/ee/api/discussions.html#get-single-merge-request-discussion-item
  def getMergeRequestDiscussion(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      discussionId: String,
  ): F[Either[GitlabError, MergeRequestDiscussion]] =
    getMergeRequestDiscussionRaw[MergeRequestDiscussion](projectId, mergeRequestIId, discussionId)

  // @see: https://docs.gitlab.com/ee/api/discussions.html#create-new-merge-request-thread
  def createMergeRequestDiscussion(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      payload: CreateMRDiscussion,
  ): F[Either[GitlabError, MergeRequestDiscussion]] =
    createMergeRequestDiscussionRaw[MergeRequestDiscussion](projectId, mergeRequestIId, payload)

  // @see: https://docs.gitlab.com/ee/api/discussions.html#resolve-a-merge-request-thread
  def resolveMergeRequestDiscussion(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      discussionId: String,
      resolved: Boolean,
  ): F[Either[GitlabError, MergeRequestDiscussion]] =
    resolveMergeRequestDiscussionRaw[MergeRequestDiscussion](projectId, mergeRequestIId, discussionId, resolved)

  // @see: https://docs.gitlab.com/ee/api/discussions.html#create-new-merge-request-thread
  def createMergeRequestDiscussionNote(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      discussionId: String,
      body: String,
  ): F[Either[GitlabError, MergeRequestNote]] =
    createMergeRequestDiscussionNoteRaw[MergeRequestNote](projectId, mergeRequestIId, discussionId, body)

  // @see: https://docs.gitlab.com/ee/api/discussions.html#modify-an-existing-merge-request-thread-note
  def updateMergeRequestDiscussionNote(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      discussionId: String,
      noteId: BigInt,
      payload: MRDiscussionUpdate,
  ): F[Either[GitlabError, MergeRequestNote]] =
    updateMergeRequestDiscussionNoteRaw[MergeRequestNote](projectId, mergeRequestIId, discussionId, noteId, payload)

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#list-merge-requests
  def globalMergeRequestSearch(
      state: NullableField[MergeRequestState] = NullValue,
      author: NullableField[EntityId] = NullValue,
      scope: NullableField[MergeRequestSearchScope] = NullValue,
      titleOrDescriptionText: NullableField[String] = NullValue,
      createdBefore: NullableField[ZonedDateTime] = NullValue,
      createdAfter: NullableField[ZonedDateTime] = NullValue,
      updatedBefore: NullableField[ZonedDateTime] = NullValue,
      updatedAfter: NullableField[ZonedDateTime] = NullValue,
      withMergeStatusRecheck: NullableField[Boolean] = NullValue,
      sort: NullableField[Sorting[MergeRequestsSort]] = NullValue,
  ): F[Either[GitlabError, Vector[MergeRequestInfo]]] = globalMergeRequestSearchRaw[MergeRequestInfo](
    state,
    author,
    scope,
    titleOrDescriptionText,
    createdBefore,
    createdAfter,
    updatedBefore,
    updatedAfter,
    withMergeStatusRecheck,
    sort,
  )

}
