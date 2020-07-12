package io.morgaroth.gitlabclient

import java.time.ZonedDateTime

import cats.Monad
import cats.data.EitherT
import cats.instances.vector._
import cats.syntax.traverse._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Decoder
import io.circe.generic.auto._
import io.morgaroth.gitlabclient.helpers.CustomDateTimeFormatter.RichZonedDateTime
import io.morgaroth.gitlabclient.marshalling.Gitlab4SMarshalling
import io.morgaroth.gitlabclient.models._
import io.morgaroth.gitlabclient.query.ParamQuery._
import io.morgaroth.gitlabclient.query._

trait GitlabRestAPI[F[_]] extends LazyLogging with Gitlab4SMarshalling {
  type GitlabResponseT[A] = EitherT[F, GitlabError, A]

  implicit def m: Monad[F]

  val API = "/api/v4"

  def config: GitlabConfig

  private val reqGen = RequestGenerator(config)

  protected def invokeRequest(request: GitlabRequest)(implicit requestId: RequestId): EitherT[F, GitlabError, String] = {
    invokeRequestRaw(request).map(_.payload)
  }

  protected def invokeRequestRaw(request: GitlabRequest)(implicit requestId: RequestId): EitherT[F, GitlabError, GitlabResponse]

  def getCurrentUser: GitlabResponseT[GitlabFullUser] = {
    implicit val rId: RequestId = RequestId.newOne("get-current-user")
    val req = reqGen.get(API + "/user")
    invokeRequest(req).unmarshall[GitlabFullUser]
  }

  // @see: https://docs.gitlab.com/ee/api/search.html#scope-merge_requests
  private def globalSearch(scope: SearchScope, phrase: String) = {
    val req = reqGen.get(s"$API/search", scope.toParam, search(phrase))
    getAllPaginatedResponse[MergeRequestInfo](req, s"global-search-${scope.name}", AllPages)
  }

  // Merge requests

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

  private def renderParams(
                            myReaction: String, search: String, state: MergeRequestState,
                            updatedBefore: ZonedDateTime, updatedAfter: ZonedDateTime,
                            createdBefore: ZonedDateTime, createdAfter: ZonedDateTime,
                            sort: Sorting[MergeRequestsSort],
                          ): Vector[ParamQuery] = {
    Vector(
      wrap(sort).flatMap(s => List("order_by".eqParam(s.field.property), "sort".eqParam(s.direction.toString))),
      wrap(myReaction).map("my_reaction_emoji".eqParam),
      wrap(updatedBefore).map("updated_before".eqParam),
      wrap(updatedAfter).map("updated_after".eqParam),
      wrap(createdBefore).map("created_before".eqParam),
      wrap(createdAfter).map("created_after".eqParam),
      wrap(search).map("search".eqParam),
      List(state.toParam),
    ).flatten
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
  def getMergeRequestDiff(projectID: EntityId, mrId: BigInt): EitherT[F, GitlabError, MergeRequestFull] = {
    implicit val rId: RequestId = RequestId.newOne("get-merge-request-diff")
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/merge_requests/$mrId/changes")
    invokeRequest(req).unmarshall[MergeRequestFull]
  }

  // award emojis

  // @see: https://docs.gitlab.com/ee/api/award_emoji.html#list-an-awardables-award-emoji
  def getEmojiAwards(projectID: EntityId, scope: AwardableScope, awardableId: BigInt): EitherT[F, GitlabError, Vector[EmojiAward]] = {
    implicit val rId: RequestId = RequestId.newOne(s"get-$scope-awards")
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/$scope/$awardableId/award_emoji")
    invokeRequest(req).unmarshall[Vector[EmojiAward]]
  }

  // @see: https://docs.gitlab.com/ee/api/award_emoji.html#award-a-new-emoji
  def awardEmoji(projectID: EntityId, scope: AwardableScope, awardableId: BigInt, emojiName: String)
  : EitherT[F, GitlabError, EmojiAward] = {
    implicit val rId: RequestId = RequestId.newOne(s"award-$scope-emoji")
    val req = reqGen.post(s"$API/projects/${projectID.toStringId}/$scope/$awardableId/award_emoji", "name".eqParam(emojiName))
    invokeRequest(req).unmarshall[EmojiAward]
  }

  // @see: https://docs.gitlab.com/ee/api/award_emoji.html#delete-an-award-emoji
  def unawardEmoji(projectID: EntityId, scope: AwardableScope, awardableId: BigInt, awardId: BigInt): EitherT[F, GitlabError, Unit] = {
    implicit val rId: RequestId = RequestId.newOne(s"unaward-$scope-emoji")
    val req = reqGen.delete(s"$API/projects/${projectID.toStringId}/$scope/$awardableId/award_emoji/$awardId")
    invokeRequest(req).map(_ => ())
  }

  def getEmojiAwards(mergeRequest: MergeRequestInfo): EitherT[F, GitlabError, Vector[EmojiAward]] = {
    getEmojiAwards(mergeRequest.project_id, AwardableScope.MergeRequests, mergeRequest.iid)
  }

  def awardEmoji(mergeRequest: MergeRequestInfo, emojiName: String): EitherT[F, GitlabError, EmojiAward] = {
    awardEmoji(mergeRequest.project_id, AwardableScope.MergeRequests, mergeRequest.iid, emojiName)
  }

  def unawardEmoji(mergeRequest: MergeRequestInfo, emojiAward: EmojiAward): EitherT[F, GitlabError, Unit] = {
    unawardEmoji(mergeRequest.project_id, AwardableScope.MergeRequests, mergeRequest.iid, emojiAward.id)
  }

  def awardMergeRequestEmoji(projectID: EntityId, mergeRequestIID: BigInt, emojiName: String): EitherT[F, GitlabError, EmojiAward] = {
    awardEmoji(projectID, AwardableScope.MergeRequests, mergeRequestIID, emojiName)
  }

  def getMergeRequestEmoji(projectID: EntityId, mergeRequestIID: BigInt): EitherT[F, GitlabError, Vector[EmojiAward]] = {
    getEmojiAwards(projectID, AwardableScope.MergeRequests, mergeRequestIID)
  }

  // approvals

  // @see: https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-configuration-1
  def getApprovals(projectId: EntityId, mergeRequestIId: BigInt): EitherT[F, GitlabError, MergeRequestApprovals] = {
    implicit val rId: RequestId = RequestId.newOne("get-mr-approvals")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/merge_requests/$mergeRequestIId/approvals")
    invokeRequest(req).unmarshall[MergeRequestApprovals]
  }

  // @see https://docs.gitlab.com/ee/api/merge_request_approvals.html#get-the-approval-state-of-merge-requests
  def getMergeRequestApprovalRules(projectId: EntityId, mergeRequestIId: BigInt): EitherT[F, GitlabError, Vector[MergeRequestApprovalRule]] = {
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

  def deleteApprovalRule(projectId: EntityId, mergeRequestIId: BigInt, approvalRule: MergeRequestApprovalRule): EitherT[F, GitlabError, String] = {
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

  // commits

  // @see: https://docs.gitlab.com/ee/api/commits.html#get-a-single-commit
  def getCommit(projectId: EntityId, ref: String): EitherT[F, GitlabError, Commit] = {
    implicit val rId: RequestId = RequestId.newOne("get-single-commit")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$ref")
    invokeRequest(req).unmarshall[Commit]
  }

  // @see: https://docs.gitlab.com/ee/api/commits.html#get-references-a-commit-is-pushed-to
  def getCommitRefs(projectId: EntityId, commitId: String): EitherT[F, GitlabError, Vector[RefSimpleInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("get-refs-of-a-commit")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$commitId/refs")
    invokeRequest(req).unmarshall[Vector[RefSimpleInfo]]
  }

  def getCommits(projectId: EntityId,
                 path: String = null,
                 ref: String = null,
                 since: ZonedDateTime = null,
                 until: ZonedDateTime = null,
                 paging: Paging = AllPages,
                ): EitherT[F, GitlabError, Vector[CommitSimple]] = {
    val params = Vector(
      wrap(ref).map("ref_name".eqParam(_)),
      wrap(path).map("path".eqParam(_)),
      wrap(since).map(_.toISO8601UTC).map("since".eqParam(_)),
      wrap(until).map(_.toISO8601UTC).map("until".eqParam(_)),
    ).flatten
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits", params: _*)
    getAllPaginatedResponse[CommitSimple](req, "get-commits", paging)
  }

  // @see: https://docs.gitlab.com/ee/api/commits.html#get-the-diff-of-a-commit
  def getDiffOfACommit(projectId: EntityId, ref: String): EitherT[F, GitlabError, Vector[FileDiff]] = {
    implicit val rId: RequestId = RequestId.newOne("get-commits-diff")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$ref/diff")
    invokeRequest(req).unmarshall[Vector[FileDiff]]
  }

  // @see: https://docs.gitlab.com/ee/api/commits.html#list-merge-requests-associated-with-a-commit
  def getMergeRequestsOfCommit(projectId: EntityId, commitSha: String): EitherT[F, GitlabError, Vector[MergeRequestInfo]] = {
    implicit val rId: RequestId = RequestId.newOne("get-commit-merge-requests")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$commitSha/merge_requests")
    invokeRequest(req).unmarshall[Vector[MergeRequestInfo]]
  }

  // @see: https://docs.gitlab.com/ee/api/commits.html#get-references-a-commit-is-pushed-to
  def getCommitsReferences(projectId: EntityId, commitSha: String): EitherT[F, GitlabError, Vector[CommitReference]] = {
    implicit val rId: RequestId = RequestId.newOne("get-commit-references")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/commits/$commitSha/refs")
    invokeRequest(req).unmarshall[Vector[CommitReference]]
  }

  // tags

  // @see: https://docs.gitlab.com/ee/api/tags.html#list-project-repository-tags
  def getProjectTags(projectId: EntityId,
                     search: String = null,
                     paging: Paging = AllPages,
                     sort: Sorting[TagsSort] = null,
                    ): EitherT[F, GitlabError, Vector[TagInfo]] = {

    val q = Vector(
      wrap(sort).flatMap(_.toParams),
      wrap(search).map("search".eqParam),
    ).flatten
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/tags", q: _*)
    getAllPaginatedResponse[TagInfo](req, "get-tags", paging)
  }

  // @see: https://docs.gitlab.com/ee/api/tags.html#create-a-new-tag
  def createTag(projectId: EntityId, tagName: String, refToTag: String, message: Option[String], description: Option[String])
  : EitherT[F, GitlabError, TagInfo] = {
    implicit val rId: RequestId = RequestId.newOne("create-tag")
    val q = Vector(
      Vector("tag_name".eqParam(tagName), "ref".eqParam(refToTag)),
      message.map("message".eqParam).toList,
      description.map("release_description".eqParam).toList,
    ).flatten
    val req = reqGen.post(s"$API/projects/${projectId.toStringId}/repository/tags", q: _*)
    invokeRequest(req).unmarshall[TagInfo]
  }

  // @see: https://docs.gitlab.com/ee/api/tags.html#get-a-single-repository-tag
  def getTag(projectId: EntityId, tagName: String): EitherT[F, GitlabError, TagInfo] = {
    implicit val rId: RequestId = RequestId.newOne("get-tag")
    val req = reqGen.get(s"$API/projects/${projectId.toStringId}/repository/tags/$tagName")
    invokeRequest(req).unmarshall[TagInfo]
  }

  // @see: https://docs.gitlab.com/ee/api/tags.html#delete-a-tag
  def deleteTag(projectId: EntityId, tagName: String)
  : EitherT[F, GitlabError, Unit] = {
    implicit val rId: RequestId = RequestId.newOne("delete-tag")
    val req = reqGen.delete(s"$API/projects/${projectId.toStringId}/repository/tags/$tagName")
    invokeRequest(req).map(_ => ())
  }

  //  other

  def groupSearchCommits(groupId: EntityId, phrase: String): GitlabResponseT[String] = {
    implicit val rId: RequestId = RequestId.newOne("group-search-mr")
    groupGlobalSearch(groupId, SearchScope.Commits, Some(phrase))
  }

  // @see: https://docs.gitlab.com/ee/api/projects.html#get-single-project
  def getProject(projectId: EntityId): GitlabResponseT[ProjectInfo] = {
    implicit val rId: RequestId = RequestId.newOne("get-project-by-id")
    val req = reqGen.get(API + s"/projects/${projectId.toStringId}")
    invokeRequest(req).unmarshall[ProjectInfo]
  }

  def getProjects(ids: Iterable[BigInt]): GitlabResponseT[Vector[ProjectInfo]] = {
    ids.toVector.traverse(x => getProject(x))
  }

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

  // @see: https://docs.gitlab.com/ee/api/branches.html#list-repository-branches
  def getBranches(projectID: EntityId, searchTerm: Option[String]): GitlabResponseT[Vector[GitlabBranchInfo]] = {
    val req = reqGen.get(s"$API/projects/${projectID.toStringId}/repository/branches",
      searchTerm.map(ParamQuery.search).getOrElse(NoParam)
    )
    getAllPaginatedResponse[GitlabBranchInfo](req, "merge-requests-per-project", AllPages)
  }

  private def getAllPaginatedResponse[A: Decoder](req: GitlabRequest, kind: String, paging: Paging): EitherT[F, GitlabError, Vector[A]] = {

    val pageSize = paging match {
      case PageCount(_, pageSize) => pageSize
      case EntitiesCount(count) if count < 100 => count
      case _ => 100
    }

    val entitiesLimit = paging match {
      case PageCount(pagesCount, pageSize) => pageSize * pagesCount
      case EntitiesCount(expectedEntitiesCount) => expectedEntitiesCount
      case _ => Int.MaxValue
    }

    def getAll(pageNo: Int, pageSizeEff: Int, acc: Vector[A]): EitherT[F, GitlabError, Vector[A]] = {
      implicit val rId: RequestId = RequestId.newOne(s"$kind-page-$pageNo")

      val resp = invokeRequestRaw(req.withParams(pageSizeEff.pageSizeParam, pageNo.pageNumParam))

      def nextPageHeaders(headers: Map[String, String]): Option[(Int, Int)] = for {
        nextPageNum <- headers.get("X-Next-Page").filter(_.nonEmpty).map(_.toInt)
        perPage <- headers.get("X-Per-Page").filter(_.nonEmpty).map(_.toInt)
      } yield (nextPageNum, perPage)

      for {
        result <- resp.unmarshall[Vector[A]]
        respHeaders <- resp.map(_.headers)
        currentResult = acc ++ result
        nextPageInfo = nextPageHeaders(respHeaders).map(x => x._1 -> math.min(x._2, entitiesLimit - currentResult.length)).filter(_._2 > 0)
        res <- nextPageInfo.map(p => getAll(p._1, p._2, currentResult)).getOrElse(EitherT.pure[F, GitlabError](currentResult))
      } yield res
    }

    getAll(1, pageSize, Vector.empty)
  }

  def wrap[T](value: T): List[T] = Option(value).toList
}