package io.gitlab.mateuszjaje.gitlabclient
package apisv2

import GitlabRestBaseV2.renderParams
import apisv2.GitlabApiT.syntax.*
import helpers.{NullValue, NullableField}
import models.*
import query.ParamQuery.*

import io.circe.Decoder

import java.time.ZonedDateTime

trait MergeRequestsRawAPIV2[F[_]] {
  this: GitlabRestBaseV2[F] =>

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#list-project-merge-requests
  def getMergeRequestsRaw[T: Decoder](
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
  ): F[Either[GitlabError, Vector[T]]] = {
    val q = renderParams(
      myReaction,
      author,
      search,
      state,
      updatedBefore,
      updatedAfter,
      createdBefore,
      createdAfter,
      withMergeStatusRecheck,
      sort,
    )
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/merge_requests", projectID, q *)
    getAllPaginatedResponse[T](req, "merge-requests-per-project", paging)
  }

  // traverse over all states and fetch merge requests for every state, gitlab doesn't offer search by multiple states
  def getMergeRequestsRaw[T: Decoder](projectID: EntityId, states: Iterable[MergeRequestState]): F[Either[GitlabError, Vector[T]]] = {
    val value1                                      = m.sequence(states.toVector.map(state => getMergeRequestsRaw[T](projectID, state)))
    val value: GitlabApiT.Ops[F, Vector[Vector[T]]] = value1
    value.map(_.flatten)
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#list-group-merge-requests
  def getGroupMergeRequestsRaw[T: Decoder](
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
  ): F[Either[GitlabError, Vector[T]]] = {
    val q = renderParams(
      myReaction,
      author,
      search,
      state,
      updatedBefore,
      updatedAfter,
      createdBefore,
      createdAfter,
      withMergeStatusRecheck,
      sort,
    )

    val req = reqGen.get(s"$API/groups/${groupId.toStringId}/merge_requests", q *)
    getAllPaginatedResponse[T](req, "merge-requests-per-group", paging)
  }

  // traverse over all states and fetch merge requests for every state, gitlab doesn't offer search by multiple states
  def getGroupMergeRequestsRaw[T: Decoder](groupId: EntityId, states: Iterable[MergeRequestState]): F[Either[GitlabError, Vector[T]]] = {
    val value = m.sequence(states.toVector.map(state => getGroupMergeRequestsRaw[T](groupId, state = state)))
    val value1: GitlabApiT.Ops[F, Vector[Vector[T]]] = value
    value1.map(_.flatten)
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#update-mr
  def updateMergeRequestRaw[T: Decoder](projectID: EntityId, mrId: BigInt, updateMrPayload: UpdateMRPayload): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("update-mr")
    val req = reqGen
      .put(s"$API/projects/${projectID.toStringId}/merge_requests/$mrId", MJson.write(updateMrPayload), projectID)
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#get-single-mr
  def getMergeRequestRaw[T: Decoder](projectID: EntityId, mrId: BigInt): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("get-merge-request-info")
    val req                     = reqGen.get(s"$API/projects/${projectID.toStringId}/merge_requests/$mrId", projectID)
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#delete-a-merge-request
  def deleteMergeRequest(projectID: EntityId, mrIid: BigInt): F[Either[GitlabError, Unit]] = {
    implicit val rId: RequestId = RequestId.newOne("delete-merge-request")
    val req                     = reqGen.delete(s"$API/projects/${projectID.toStringId}/merge_requests/$mrIid", projectID)
    invokeRequest(req).map(_ => ())
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#get-single-mr-changes
  def getMergeRequestDiffRaw[T: Decoder](projectID: EntityId, mrId: BigInt): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("get-merge-request-diff")
    val req                     = reqGen.get(s"$API/projects/${projectID.toStringId}/merge_requests/$mrId/changes", projectID)
    invokeRequest(req).unmarshall[T]
  }

  // approvals

  // @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-configuration-1
  def getApprovalsRaw[T: Decoder](projectId: EntityId, mergeRequestIId: BigInt): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("get-mr-approvals")
    val req                     = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approvals", projectId)
    invokeRequest(req).unmarshall[T]
  }

  def getApprovalsRaw[T: Decoder](mergeRequest: MergeRequestID): F[Either[GitlabError, T]] =
    getApprovalsRaw[T](mergeRequest.project_id, mergeRequest.iid)

  //  @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#approve-merge-request
  def approveMergeRequestRaw[T: Decoder](
      projectId: EntityId,
      mergeRequestIId: BigInt,
      headSha: Option[String] = None,
  ): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("approve-mr")
    val data                    = headSha.map("sha" -> _).toMap
    val req = reqGen.post(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approve", MJson.write(data), projectId)
    invokeRequest(req).unmarshall[T]
  }

  def approveMergeRequestRaw[T: Decoder](
      mergeRequest: MergeRequestID,
      headSha: Option[String],
  ): F[Either[GitlabError, T]] = approveMergeRequestRaw(mergeRequest.project_id, mergeRequest.iid, headSha)

  //  @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#unapprove-merge-request
  def unapproveMergeRequestRaw[T: Decoder](
      projectId: EntityId,
      mergeRequestIId: BigInt,
  ): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("unapprove-mr")
    val req                     = reqGen.post(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/unapprove", projectId)
    invokeRequest(req).unmarshall[T]
  }

  // @see https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-the-approval-state-of-merge-requests
  def getMergeRequestApprovalRulesRaw[T: Decoder](
      projectId: EntityId,
      mergeRequestIId: BigInt,
  ): F[Either[GitlabError, Vector[T]]] = {
    implicit val rId: RequestId = RequestId.newOne("get-mr-approval-rules")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approval_rules", projectId)
    invokeRequest(req).unmarshall[Vector[T]]
  }

  // @see https://docs.gitlab.com/ee/api/merge_request_approvals.html#create-merge-request-level-rule
  def createApprovalRuleRaw[T: Decoder](
      projectId: EntityId,
      mergeRequestIId: BigInt,
      payload: CreateMergeRequestApprovalRule,
  ): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("create-mr-approval-rule")
    val req = reqGen
      .post(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approval_rules", MJson.write(payload), projectId)
    invokeRequest(req).unmarshall[T]
  }

  def createApprovalRuleRaw[T: Decoder](
      projectId: EntityId,
      mergeRequestIId: BigInt,
      name: String,
      userIds: Vector[BigInt],
  ): F[Either[GitlabError, T]] =
    createApprovalRuleRaw(projectId, mergeRequestIId, CreateMergeRequestApprovalRule.oneOf(name, userIds *))

  // @see https://docs.gitlab.com/ee/api/merge_request_approvals.html#delete-merge-request-level-rule
  def deleteApprovalRule(projectId: EntityId, mergeRequestIId: BigInt, approvalRuleId: BigInt): F[Either[GitlabError, String]] = {
    implicit val rId: RequestId = RequestId.newOne("delete-mr-approval-rule")
    val req =
      reqGen.delete(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approval_rules/$approvalRuleId", projectId)
    invokeRequest(req)
  }

  def deleteApprovalRule(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      approvalRule: MergeRequestApprovalRule,
  ): F[Either[GitlabError, String]] =
    deleteApprovalRule(projectId, mergeRequestIId, approvalRule.id)

  // merge-request discussions & notes

  // @see: https://docs.gitlab.com/ee/api/notes.html#list-all-merge-request-notes
  def getMergeRequestNotesRaw[T: Decoder](
      projectId: EntityId,
      mergeRequestIId: BigInt,
      paging: Paging = AllPages,
      sort: Option[Sorting[MergeRequestNotesSort]] = None,
  ): F[Either[GitlabError, Vector[T]]] = {
    val q   = sort.map(s => Seq("order_by".eqParam(s.field.property), "sort".eqParam(s.direction.toString))).toList.flatten
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes", projectId, q *)

    getAllPaginatedResponse[T](req, "merge-request-notes", paging)
  }

  def getMergeRequestNotesRaw[T: Decoder](mr: MergeRequestID): F[Either[GitlabError, Vector[T]]] =
    getMergeRequestNotesRaw(mr.project_id, mr.iid)

  def getMergeRequestNotesRaw[T: Decoder](
      mr: MergeRequestID,
      paging: Paging,
      sort: Option[Sorting[MergeRequestNotesSort]],
  ): F[Either[GitlabError, Vector[T]]] =
    getMergeRequestNotesRaw(mr.project_id, mr.iid, paging, sort)

  // @see: https://docs.gitlab.com/ee/api/notes.html#get-single-merge-request-note
  def getMergeRequestNoteRaw[T: Decoder](
      projectId: EntityId,
      mergeRequestIId: BigInt,
      noteId: BigInt,
  ): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("get-merge-request-note")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes/$noteId", projectId)
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/notes.html#create-new-merge-request-note
  def createMergeRequestNoteRaw[T: Decoder](projectId: EntityId, mergeRequestIId: BigInt, body: String): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("merge-request-note-create")
    val payload                 = MJson.write(MergeRequestNoteCreate(body))
    val req = reqGen
      .post(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes", payload, projectId)
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/notes.html#modify-existing-merge-request-note
  def updateMergeRequestNoteRaw[T: Decoder](
      projectId: EntityId,
      mergeRequestIId: BigInt,
      noteId: BigInt,
      newBody: String,
  ): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("merge-request-note-update")
    val payload                 = MJson.write(MergeRequestNoteCreate(newBody))
    val req = reqGen
      .put(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes/$noteId", payload, projectId)
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/notes.html#delete-a-merge-request-note
  def deleteMergeRequestNote(projectId: EntityId, mergeRequestIId: BigInt, noteId: BigInt): F[Either[GitlabError, Unit]] = {
    implicit val rId: RequestId = RequestId.newOne("merge-request-note-delete")
    val req = reqGen.delete(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes/$noteId", projectId)
    invokeRequest(req).map(_ => ())
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#list-project-merge-request-discussion-items
  def getMergeRequestDiscussionsRaw[T: Decoder](projectId: EntityId, mergeRequestIId: BigInt): F[Either[GitlabError, Vector[T]]] = {
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions", projectId)
    getAllPaginatedResponse[T](req, "get-merge-request-discussions", AllPages)
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#get-single-merge-request-discussion-item
  def getMergeRequestDiscussionRaw[T: Decoder](
      projectId: EntityId,
      mergeRequestIId: BigInt,
      discussionId: String,
  ): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("get-merge-request-discussion")
    val req = reqGen
      .get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions/$discussionId", projectId)
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#create-new-merge-request-thread
  def createMergeRequestDiscussionRaw[T: Decoder](
      projectId: EntityId,
      mergeRequestIId: BigInt,
      payload: CreateMRDiscussion,
  ): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("post-mr-discussion")
    val req = reqGen
      .post(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions", MJson.write(payload), projectId)
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#resolve-a-merge-request-thread
  def resolveMergeRequestDiscussionRaw[T: Decoder](
      projectId: EntityId,
      mergeRequestIId: BigInt,
      discussionId: String,
      resolved: Boolean,
  ): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("resolve-mr-discussion")
    val payload                 = MRDiscussionUpdate.resolve(resolved)
    val req = reqGen
      .put(
        s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions/$discussionId",
        MJson.write(payload),
        projectId,
      )
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#create-new-merge-request-thread
  def createMergeRequestDiscussionNoteRaw[T: Decoder](
      projectId: EntityId,
      mergeRequestIId: BigInt,
      discussionId: String,
      body: String,
  ): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("reply-mr-discussion")
    val payload                 = MergeRequestNoteCreate(body)
    val req = reqGen
      .post(
        s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions/$discussionId/notes",
        MJson.write(payload),
        projectId,
      )
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#modify-an-existing-merge-request-thread-note
  def updateMergeRequestDiscussionNoteRaw[T: Decoder](
      projectId: EntityId,
      mergeRequestIId: BigInt,
      discussionId: String,
      noteId: BigInt,
      payload: MRDiscussionUpdate,
  ): F[Either[GitlabError, T]] = {
    implicit val rId: RequestId = RequestId.newOne("update-mr-discussion-note")
    val req = reqGen
      .put(
        s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions/$discussionId/notes/$noteId",
        MJson.write(payload),
        projectId,
      )
    invokeRequest(req).unmarshall[T]
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#list-merge-requests
  def globalMergeRequestSearchRaw[T: Decoder](
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
  ): F[Either[GitlabError, Vector[T]]] = {
    implicit val rId: RequestId = RequestId.newOne("list-mrs")
    val q = Vector(
      state.toList.map(_.toParam),
      author.toList.map {
        case NumericEntityIdId(id) => "author_id".eqParam(id)
        case StringEntityId(id)    => "author_username".eqParam(id)
      },
      scope.toList.map(_.name).map("scope" eqParam _),
      titleOrDescriptionText.toList.map("search" eqParam _),
      createdBefore.toList.map("created_before" eqParam _),
      createdAfter.toList.map("created_after" eqParam _),
      updatedBefore.toList.map("updated_before" eqParam _),
      updatedAfter.toList.map("updated_after" eqParam _),
      withMergeStatusRecheck.toList.map("with_merge_status_recheck" eqParam _),
      sort.toList.flatMap(s => List("order_by".eqParam(s.field.property), "sort".eqParam(s.direction.toString))),
    ).flatten
    val req = reqGen.get(s"$API/merge_requests", q *)
    invokeRequest(req).unmarshall[Vector[T]]
  }

}
