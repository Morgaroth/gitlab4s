package io.morgaroth.gitlabclient.apis

import cats.data.EitherT
import cats.instances.vector._
import cats.syntax.traverse._
import io.morgaroth.gitlabclient._
import io.morgaroth.gitlabclient.helpers.{NullValue, NullableField}
import io.morgaroth.gitlabclient.models._
import io.morgaroth.gitlabclient.query.ParamQuery._
import io.morgaroth.gitlabclient.query._

import java.time.ZonedDateTime

trait MergeRequestsAPI[F[_]] {
  this: GitlabRestAPI[F] =>

  def globalMRsSearch(phrase: String): GitlabResponseT[Vector[MergeRequestInfo]] =
    globalSearch(SearchScope.MergeRequests, phrase)

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests-1
  private def groupGlobalSearch(
      groupId: EntityId,
      scope: SearchScope,
      phrase: Option[String],
  )(implicit rId: RequestId) = {
    val req = reqGen.get(s"$API/groups/${groupId.toStringId}/search", scope.toParam, phrase.map(Search).getOrElse(NoParam))
    invokeRequest(req)
  }

  def groupSearchMrs(groupId: EntityId, phrase: String): GitlabResponseT[Vector[MergeRequestInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("group-search-mr")
    groupGlobalSearch(groupId, SearchScope.MergeRequests, Some(phrase))
      .unmarshall[Vector[MergeRequestInfo]]
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#list-project-merge-requests
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
  ): GitlabResponseT[Vector[MergeRequestInfo]] = {
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
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/merge_requests", q: _*).withProjectId(projectID)
    getAllPaginatedResponse[MergeRequestInfo](req, "merge-requests-per-project", paging)
  }

  // traverse over all states and fetch merge requests for every state, gitlab doesn't offer search by multiple states
  def getMergeRequests(projectID: EntityId, states: Iterable[MergeRequestState]): GitlabResponseT[Vector[MergeRequestInfo]] =
    states.toVector.traverse(state => getMergeRequests(projectID, state)).map(_.flatten)

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
  ): GitlabResponseT[Vector[MergeRequestInfo]] = {
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

    val req = reqGen.get(s"$API/groups/${groupId.toStringId}/merge_requests", q: _*)
    getAllPaginatedResponse[MergeRequestInfo](req, "merge-requests-per-group", paging)
  }

  // traverse over all states and fetch merge requests for every state, gitlab doesn't offer search by multiple states
  def getGroupMergeRequests(groupId: EntityId, states: Iterable[MergeRequestState]): GitlabResponseT[Vector[MergeRequestInfo]] =
    states.toVector.traverse(state => getGroupMergeRequests(groupId, state = state)).map(_.flatten)

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#update-mr
  def updateMergeRequest(projectID: EntityId, mrId: BigInt, updateMrPayload: UpdateMRPayload): GitlabResponseT[MergeRequestFull] = {
    implicit val rId: RequestId = RequestId.newOne("update-mr")
    val req = reqGen
      .put(s"$API/projects/${projectID.toStringId}/merge_requests/$mrId", MJson.write(updateMrPayload))
      .withProjectId(projectID)
    invokeRequest(req).unmarshall[MergeRequestFull]
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#get-single-mr
  def getMergeRequest(projectID: EntityId, mrId: BigInt): GitlabResponseT[MergeRequestFull] = {
    implicit val rId: RequestId = RequestId.newOne("get-merge-request-info")
    val req                     = reqGen.get(s"$API/projects/${projectID.toStringId}/merge_requests/$mrId").withProjectId(projectID)
    invokeRequest(req).unmarshall[MergeRequestFull]
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#delete-a-merge-request
  def deleteMergeRequest(projectID: EntityId, mrIid: BigInt): GitlabResponseT[Unit] = {
    implicit val rId: RequestId = RequestId.newOne("delete-merge-request")
    val req                     = reqGen.delete(s"$API/projects/${projectID.toStringId}/merge_requests/$mrIid")
    invokeRequest(req).map(_ => ())
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#get-single-mr-changes
  def getMergeRequestDiff(projectID: EntityId, mrId: BigInt): GitlabResponseT[MergeRequestFull] = {
    implicit val rId: RequestId = RequestId.newOne("get-merge-request-diff")
    val req                     = reqGen.get(s"$API/projects/${projectID.toStringId}/merge_requests/$mrId/changes").withProjectId(projectID)
    invokeRequest(req).unmarshall[MergeRequestFull]
  }

  // approvals

  // @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-configuration-1
  def getApprovals(projectId: EntityId, mergeRequestIId: BigInt): GitlabResponseT[MergeRequestApprovals] = {
    implicit val rId: RequestId = RequestId.newOne("get-mr-approvals")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approvals").withProjectId(projectId)
    invokeRequest(req).unmarshall[MergeRequestApprovals]
  }

  def getApprovals(mergeRequest: MergeRequestID): GitlabResponseT[MergeRequestApprovals] =
    getApprovals(mergeRequest.project_id, mergeRequest.iid)

  //  @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#approve-merge-request
  def approveMergeRequest(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      headSha: Option[String] = None,
  ): EitherT[F, GitlabError, MergeRequestApprovals] = {
    implicit val rId: RequestId = RequestId.newOne("approve-mr")
    val data                    = headSha.map("sha" -> _).toMap
    val req = reqGen.post(API + s"/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approve", MJson.write(data))
    invokeRequest(req).unmarshall[MergeRequestApprovals]
  }

  def approveMergeRequest(
      mergeRequest: MergeRequestID,
      headSha: Option[String],
  ): EitherT[F, GitlabError, MergeRequestApprovals] = approveMergeRequest(mergeRequest.project_id, mergeRequest.iid, headSha)

  //  @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#unapprove-merge-request
  def unapproveMergeRequest(
      projectId: EntityId,
      mergeRequestIId: BigInt,
  ): EitherT[F, GitlabError, MergeRequestApprovals] = {
    implicit val rId: RequestId = RequestId.newOne("unapprove-mr")
    val req                     = reqGen.post(API + s"/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/unapprove")
    invokeRequest(req).unmarshall[MergeRequestApprovals]
  }

  // @see https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-the-approval-state-of-merge-requests
  def getMergeRequestApprovalRules(
      projectId: EntityId,
      mergeRequestIId: BigInt,
  ): GitlabResponseT[Vector[MergeRequestApprovalRule]] = {
    implicit val rId: RequestId = RequestId.newOne("get-mr-approval-rules")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approval_rules").withProjectId(projectId)
    invokeRequest(req).unmarshall[Vector[MergeRequestApprovalRule]]
  }

  // @see https://docs.gitlab.com/ee/api/merge_request_approvals.html#create-merge-request-level-rule
  def createApprovalRule(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      payload: CreateMergeRequestApprovalRule,
  ): GitlabResponseT[MergeRequestApprovalRule] = {
    implicit val rId: RequestId = RequestId.newOne("create-mr-approval-rule")
    val req = reqGen
      .post(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approval_rules", MJson.write(payload))
      .withProjectId(projectId)
    invokeRequest(req).unmarshall[MergeRequestApprovalRule]
  }

  def createApprovalRule(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      name: String,
      userIds: Vector[BigInt],
  ): GitlabResponseT[MergeRequestApprovalRule] =
    createApprovalRule(projectId, mergeRequestIId, CreateMergeRequestApprovalRule.oneOf(name, userIds: _*))

  // @see https://docs.gitlab.com/ee/api/merge_request_approvals.html#delete-merge-request-level-rule
  def deleteApprovalRule(projectId: EntityId, mergeRequestIId: BigInt, approvalRuleId: BigInt): GitlabResponseT[String] = {
    implicit val rId: RequestId = RequestId.newOne("delete-mr-approval-rule")
    val req = reqGen.delete(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approval_rules/$approvalRuleId")
    invokeRequest(req)
  }

  def deleteApprovalRule(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      approvalRule: MergeRequestApprovalRule,
  ): GitlabResponseT[String] =
    deleteApprovalRule(projectId, mergeRequestIId, approvalRule.id)

  // merge-request discussions & notes

  // @see: https://docs.gitlab.com/ee/api/notes.html#list-all-merge-request-notes
  def getMergeRequestNotes(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      paging: Paging = AllPages,
      sort: Option[Sorting[MergeRequestNotesSort]] = None,
  ): GitlabResponseT[Vector[MergeRequestNote]] = {
    val q   = sort.map(s => Seq("order_by".eqParam(s.field.property), "sort".eqParam(s.direction.toString))).toList.flatten
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes", q: _*).withProjectId(projectId)

    getAllPaginatedResponse[MergeRequestNote](req, "merge-request-notes", paging)
  }

  def getMergeRequestNotes(mr: MergeRequestID): GitlabResponseT[Vector[MergeRequestNote]] =
    getMergeRequestNotes(mr.project_id, mr.iid)

  def getMergeRequestNotes(
      mr: MergeRequestID,
      paging: Paging,
      sort: Option[Sorting[MergeRequestNotesSort]],
  ): GitlabResponseT[Vector[MergeRequestNote]] =
    getMergeRequestNotes(mr.project_id, mr.iid, paging, sort)

  // @see: https://docs.gitlab.com/ee/api/notes.html#get-single-merge-request-note
  def getMergeRequestNote(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      noteId: BigInt,
  ): GitlabResponseT[MergeRequestNote] = {
    implicit val rId: RequestId = RequestId.newOne("get-merge-request-note")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes/$noteId").withProjectId(projectId)
    invokeRequest(req).unmarshall[MergeRequestNote]
  }

  // @see: https://docs.gitlab.com/ee/api/notes.html#create-new-merge-request-note
  def createMergeRequestNote(projectId: EntityId, mergeRequestIId: BigInt, body: String): GitlabResponseT[MergeRequestNote] = {
    implicit val rId: RequestId = RequestId.newOne("merge-request-note-create")
    val payload                 = MJson.write(MergeRequestNoteCreate(body))
    val req = reqGen
      .post(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes", payload)
    invokeRequest(req).unmarshall[MergeRequestNote]
  }

  // @see: https://docs.gitlab.com/ee/api/notes.html#modify-existing-merge-request-note
  def updateMergeRequestNote(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      noteId: BigInt,
      newBody: String,
  ): GitlabResponseT[MergeRequestNote] = {
    implicit val rId: RequestId = RequestId.newOne("merge-request-note-update")
    val payload                 = MJson.write(MergeRequestNoteCreate(newBody))
    val req = reqGen
      .put(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes/$noteId", payload)
    invokeRequest(req).unmarshall[MergeRequestNote]
  }

  // @see: https://docs.gitlab.com/ee/api/notes.html#delete-a-merge-request-note
  def deleteMergeRequestNote(projectId: EntityId, mergeRequestIId: BigInt, noteId: BigInt): GitlabResponseT[Unit] = {
    implicit val rId: RequestId = RequestId.newOne("merge-request-note-delete")
    val req                     = reqGen.delete(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes/$noteId")
    invokeRequest(req).map(_ => ())
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#list-project-merge-request-discussion-items
  def getMergeRequestDiscussions(projectId: EntityId, mergeRequestIId: BigInt): GitlabResponseT[Vector[MergeRequestDiscussion]] = {
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions").withProjectId(projectId)
    getAllPaginatedResponse[MergeRequestDiscussion](req, "get-merge-request-discussions", AllPages)
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#get-single-merge-request-discussion-item
  def getMergeRequestDiscussion(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      discussionId: String,
  ): GitlabResponseT[MergeRequestDiscussion] = {
    implicit val rId: RequestId = RequestId.newOne("get-merge-request-discussion")
    val req = reqGen
      .get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions/$discussionId")
      .withProjectId(projectId)
    invokeRequest(req).unmarshall[MergeRequestDiscussion]
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#create-new-merge-request-thread
  def createMergeRequestDiscussion(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      payload: CreateMRDiscussion,
  ): GitlabResponseT[MergeRequestDiscussion] = {
    implicit val rId: RequestId = RequestId.newOne("post-mr-discussion")
    val req = reqGen
      .post(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions", MJson.write(payload))
      .withProjectId(projectId)
    invokeRequest(req).unmarshall[MergeRequestDiscussion]
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#resolve-a-merge-request-thread
  def resolveMergeRequestDiscussion(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      discussionId: String,
      resolved: Boolean,
  ): GitlabResponseT[MergeRequestDiscussion] = {
    implicit val rId: RequestId = RequestId.newOne("resolve-mr-discussion")
    val payload                 = MRDiscussionUpdate.resolve(resolved)
    val req = reqGen
      .put(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions/$discussionId", MJson.write(payload))
      .withProjectId(projectId)
    invokeRequest(req).unmarshall[MergeRequestDiscussion]
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#create-new-merge-request-thread
  def createMergeRequestDiscussionNote(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      discussionId: String,
      body: String,
  ): GitlabResponseT[MergeRequestNote] = {
    implicit val rId: RequestId = RequestId.newOne("reply-mr-discussion")
    val payload                 = MergeRequestNoteCreate(body)
    val req = reqGen
      .post(
        s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions/$discussionId/notes",
        MJson.write(payload),
      )
      .withProjectId(projectId)
    invokeRequest(req).unmarshall[MergeRequestNote]
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#modify-an-existing-merge-request-thread-note
  def updateMergeRequestDiscussionNote(
      projectId: EntityId,
      mergeRequestIId: BigInt,
      discussionId: String,
      noteId: BigInt,
      payload: MRDiscussionUpdate,
  ): GitlabResponseT[MergeRequestNote] = {
    implicit val rId: RequestId = RequestId.newOne("update-mr-discussion-note")
    val req = reqGen
      .put(
        s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions/$discussionId/notes/$noteId",
        MJson.write(payload),
      )
      .withProjectId(projectId)
    invokeRequest(req).unmarshall[MergeRequestNote]
  }

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
  ): GitlabResponseT[Vector[MergeRequestInfo]] = {
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
    val req = reqGen.get(s"$API/merge_requests", q: _*)
    invokeRequest(req).unmarshall[Vector[MergeRequestInfo]]
  }

}
