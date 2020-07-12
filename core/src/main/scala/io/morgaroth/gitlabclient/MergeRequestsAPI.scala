package io.morgaroth.gitlabclient

import java.time.ZonedDateTime

import cats.data.EitherT
import cats.instances.vector._
import cats.syntax.traverse._
import io.circe.generic.auto._
import io.morgaroth.gitlabclient.models._
import io.morgaroth.gitlabclient.query.ParamQuery._
import io.morgaroth.gitlabclient.query._

trait MergeRequestsAPI[F[_]] {
  this: GitlabRestAPI[F] =>

  def globalMRsSearch(phrase: String): GitlabResponseT[Vector[MergeRequestInfo]] = {
    globalSearch(SearchScope.MergeRequests, phrase)
  }

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests-1
  private def groupGlobalSearch(groupId: EntityId, scope: SearchScope, phrase: Option[String])
                               (implicit rId: RequestId) = {
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
                        search: String = null,
                        myReaction: String = null,
                        updatedBefore: ZonedDateTime = null,
                        updatedAfter: ZonedDateTime = null,
                        createdBefore: ZonedDateTime = null,
                        createdAfter: ZonedDateTime = null,
                        paging: Paging = AllPages,
                        sort: Sorting[MergeRequestsSort] = null
                      ): GitlabResponseT[Vector[MergeRequestInfo]] = {
    val q = renderParams(myReaction, search, state, updatedBefore, updatedAfter, createdBefore, createdAfter, sort)
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/merge_requests", q: _*)
    getAllPaginatedResponse[MergeRequestInfo](req, "merge-requests-per-project", paging)
  }

  // traverse over all states and fetch merge requests for every state, gitlab doesn't offer search by multiple states
  def getMergeRequests(projectID: EntityId, states: Iterable[MergeRequestState]): GitlabResponseT[Vector[MergeRequestInfo]] = {
    states.toVector.traverse { state =>
      getMergeRequests(projectID, state)
    }.map(_.flatten)
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#list-group-merge-requests
  def getGroupMergeRequests(
                             groupId: EntityId,
                             state: MergeRequestState = MergeRequestStates.All,
                             search: String = null,
                             myReaction: String = null,
                             updatedBefore: ZonedDateTime = null,
                             updatedAfter: ZonedDateTime = null,
                             createdBefore: ZonedDateTime = null,
                             createdAfter: ZonedDateTime = null,
                             paging: Paging = AllPages,
                             sort: Sorting[MergeRequestsSort] = null,
                           ): GitlabResponseT[Vector[MergeRequestInfo]] = {
    val q = renderParams(myReaction, search, state, updatedBefore, updatedAfter, createdBefore, createdAfter, sort)

    val req = reqGen.get(s"$API/groups/${groupId.toStringId}/merge_requests", q: _*)
    getAllPaginatedResponse[MergeRequestInfo](req, "merge-requests-per-group", paging)
  }

  // traverse over all states and fetch merge requests for every state, gitlab doesn't offer search by multiple states
  def getGroupMergeRequests(groupId: EntityId, states: Iterable[MergeRequestState]): GitlabResponseT[Vector[MergeRequestInfo]] = {
    states.toVector.traverse { state =>
      getGroupMergeRequests(groupId, state = state)
    }.map(_.flatten)
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#update-mr
  def updateMergeRequest(projectID: EntityId, mrId: BigInt, updateMrPayload: UpdateMRPayload): GitlabResponseT[MergeRequestInfo] = {
    implicit val rId: RequestId = RequestId.newOne("update-mr")
    val req = reqGen.put(s"$API/projects/${projectID.toStringId}/merge_requests/$mrId", MJson.write(updateMrPayload))
    invokeRequest(req).unmarshall[MergeRequestInfo]
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#get-single-mr
  def getMergeRequest(projectID: EntityId, mrId: BigInt): EitherT[F, GitlabError, MergeRequestFull] = {
    implicit val rId: RequestId = RequestId.newOne("get-merge-request-info")
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/merge_requests/$mrId")
    invokeRequest(req).unmarshall[MergeRequestFull]
  }

  // @see: https://docs.gitlab.com/ee/api/merge_requests.html#get-single-mr-changes
  def getMergeRequestDiff(projectID: EntityId, mrId: BigInt)
  : EitherT[F, GitlabError, MergeRequestFull] = {
    implicit val rId: RequestId = RequestId.newOne("get-merge-request-diff")
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/merge_requests/$mrId/changes")
    invokeRequest(req).unmarshall[MergeRequestFull]
  }

  // approvals

  // @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-configuration-1
  def getApprovals(projectId: EntityId, mergeRequestIId: BigInt)
  : EitherT[F, GitlabError, MergeRequestApprovals] = {
    implicit val rId: RequestId = RequestId.newOne("get-mr-approvals")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approvals")
    invokeRequest(req).unmarshall[MergeRequestApprovals]
  }

  // @see https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-the-approval-state-of-merge-requests
  def getMergeRequestApprovalRules(projectId: EntityId, mergeRequestIId: BigInt)
  : EitherT[F, GitlabError, Vector[MergeRequestApprovalRule]] = {
    implicit val rId: RequestId = RequestId.newOne("get-mr-approval-rules")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approval_rules")
    invokeRequest(req).unmarshall[Vector[MergeRequestApprovalRule]]
  }

  // @see https://docs.gitlab.com/ee/api/merge_request_approvals.html#create-merge-request-level-rule
  def createApprovalRule(projectId: EntityId, mergeRequestIId: BigInt, payload: CreateMergeRequestApprovalRule)
  : EitherT[F, GitlabError, MergeRequestApprovalRule] = {
    implicit val rId: RequestId = RequestId.newOne("create-mr-approval-rule")
    val req = reqGen.post(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approval_rules", MJson.write(payload))
    invokeRequest(req).unmarshall[MergeRequestApprovalRule]
  }

  def createApprovalRule(projectId: EntityId, mergeRequestIId: BigInt, name: String, userIds: Vector[BigInt])
  : EitherT[F, GitlabError, MergeRequestApprovalRule] = {
    createApprovalRule(projectId, mergeRequestIId, CreateMergeRequestApprovalRule.oneOf(name, userIds: _*))
  }

  // @see https://docs.gitlab.com/ee/api/merge_request_approvals.html#delete-merge-request-level-rule
  def deleteApprovalRule(projectId: EntityId, mergeRequestIId: BigInt, approvalRuleId: BigInt)
  : EitherT[F, GitlabError, String] = {
    implicit val rId: RequestId = RequestId.newOne("delete-mr-approval-rule")
    val req = reqGen.delete(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approval_rules/$approvalRuleId")
    invokeRequest(req)
  }

  def deleteApprovalRule(projectId: EntityId, mergeRequestIId: BigInt, approvalRule: MergeRequestApprovalRule)
  : EitherT[F, GitlabError, String] = {
    deleteApprovalRule(projectId, mergeRequestIId, approvalRule.id)
  }

  // merge-request discussions & notes

  // @see: https://docs.gitlab.com/ee/api/notes.html#list-all-merge-request-notes
  def getMergeRequestNotes(projectId: EntityId, mergeRequestIId: BigInt, paging: Paging = AllPages, sort: Option[Sorting[MergeRequestNotesSort]] = None)
  : EitherT[F, GitlabError, Vector[MergeRequestNote]] = {
    val q = sort.map(s => Seq("order_by".eqParam(s.field.property), "sort".eqParam(s.direction.toString))).toList.flatten
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes", q: _*)

    getAllPaginatedResponse[MergeRequestNote](req, "merge-request-notes", paging)
  }

  // @see: https://docs.gitlab.com/ee/api/notes.html#create-new-merge-request-note
  def createMergeRequestNote(projectId: EntityId, mergeRequestIId: BigInt, body: String): EitherT[F, GitlabError, MergeRequestNote] = {
    implicit val rId: RequestId = RequestId.newOne("merge-request-note-create")
    val payload = MJson.write(MergeRequestNoteCreate(body))
    val req = reqGen.post(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes", payload)
    invokeRequest(req).unmarshall[MergeRequestNote]
  }

  // @see: https://docs.gitlab.com/ee/api/notes.html#modify-existing-merge-request-note
  def updateMergeRequestNote(projectId: EntityId, mergeRequestIId: BigInt, noteId: BigInt, newBody: String)
  : EitherT[F, GitlabError, MergeRequestNote] = {
    implicit val rId: RequestId = RequestId.newOne("merge-request-note-update")
    val payload = MJson.write(MergeRequestNoteCreate(newBody))
    val req = reqGen.put(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes/$noteId", payload)
    invokeRequest(req).unmarshall[MergeRequestNote]
  }

  // @see: https://docs.gitlab.com/ee/api/notes.html#delete-a-merge-request-note
  def deleteMergeRequestNote(projectId: EntityId, mergeRequestIId: BigInt, noteId: BigInt): EitherT[F, GitlabError, Unit] = {
    implicit val rId: RequestId = RequestId.newOne("merge-request-note-delete")
    val req = reqGen.delete(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/notes/$noteId")
    invokeRequest(req).map(_ => ())
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#list-project-merge-request-discussion-items
  def getMergeRequestDiscussions(projectId: EntityId, mergeRequestIId: BigInt): EitherT[F, GitlabError, Vector[MergeRequestDiscussion]] = {
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions")
    getAllPaginatedResponse[MergeRequestDiscussion](req, "get-merge-request-discussions", AllPages)
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#get-single-merge-request-discussion-item
  def getMergeRequestDiscussion(projectId: EntityId, mergeRequestIId: BigInt, discussionId: String)
  : EitherT[F, GitlabError, MergeRequestDiscussion] = {
    implicit val rId: RequestId = RequestId.newOne("get-merge-request-discussion")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions/$discussionId")
    invokeRequest(req).unmarshall[MergeRequestDiscussion]
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#create-new-merge-request-thread
  def createMergeRequestDiscussion(projectId: EntityId, mergeRequestIId: BigInt, payload: CreateMRDiscussion)
  : EitherT[F, GitlabError, MergeRequestDiscussion] = {
    implicit val rId: RequestId = RequestId.newOne("post-mr-discussion")
    val req = reqGen.post(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions", MJson.write(payload))
    invokeRequest(req).unmarshall[MergeRequestDiscussion]
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#resolve-a-merge-request-thread
  def resolveMergeRequestDiscussion(projectId: EntityId, mergeRequestIId: BigInt, discussionId: String, resolved: Boolean)
  : EitherT[F, GitlabError, MergeRequestDiscussion] = {
    implicit val rId: RequestId = RequestId.newOne("resolve-mr-discussion")
    val payload = MRDiscussionUpdate.resolve(resolved)
    val req = reqGen.put(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions/$discussionId", MJson.write(payload))
    invokeRequest(req).unmarshall[MergeRequestDiscussion]
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#create-new-merge-request-thread
  def createMergeRequestDiscussionNote(projectId: EntityId, mergeRequestIId: BigInt, discussionId: String, body: String)
  : EitherT[F, GitlabError, MergeRequestNote] = {
    implicit val rId: RequestId = RequestId.newOne("reply-mr-discussion")
    val payload = MergeRequestNoteCreate(body)
    val req = reqGen.post(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions/$discussionId/notes", MJson.write(payload))
    invokeRequest(req).unmarshall[MergeRequestNote]
  }

  // @see: https://docs.gitlab.com/ee/api/discussions.html#modify-an-existing-merge-request-thread-note
  def updateMergeRequestDiscussionNote(projectId: EntityId, mergeRequestIId: BigInt, discussionId: String, noteId: BigInt, payload: MRDiscussionUpdate)
  : EitherT[F, GitlabError, MergeRequestNote] = {
    implicit val rId: RequestId = RequestId.newOne("update-mr-discussion-note")
    val req = reqGen.put(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/discussions/$discussionId/notes/$noteId", MJson.write(payload))
    invokeRequest(req).unmarshall[MergeRequestNote]
  }

}
