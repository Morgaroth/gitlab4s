package io.gitlab.mateuszjaje.gitlabclient
package apis

import models.{AwardableScope, EmojiAward, MergeRequestInfo}
import query.ParamQuery._

import cats.data.EitherT

trait EmojiAwardsAPI[F[_]] {
  this: GitlabRestAPI[F] =>

  // award emojis

  // @see: https://docs.gitlab.com/ee/api/award_emoji.html#list-an-awardables-award-emoji
  def getEmojiAwards(projectID: EntityId, scope: AwardableScope, awardableId: BigInt): EitherT[F, GitlabError, Vector[EmojiAward]] = {
    implicit val rId: RequestId = RequestId.newOne(s"get-$scope-awards")
    val req                     = reqGen.get(s"$API/projects/${projectID.toStringId}/$scope/$awardableId/award_emoji", projectID)
    invokeRequest(req).unmarshall[Vector[EmojiAward]]
  }

  // @see: https://docs.gitlab.com/ee/api/award_emoji.html#award-a-new-emoji
  def awardEmoji(
      projectID: EntityId,
      scope: AwardableScope,
      awardableId: BigInt,
      emojiName: String,
  ): EitherT[F, GitlabError, EmojiAward] = {
    implicit val rId: RequestId = RequestId.newOne(s"award-$scope-emoji")
    val req = reqGen
      .post(s"$API/projects/${projectID.toStringId}/$scope/$awardableId/award_emoji", projectID, "name".eqParam(emojiName))
    invokeRequest(req).unmarshall[EmojiAward]
  }

  // @see: https://docs.gitlab.com/ee/api/award_emoji.html#delete-an-award-emoji
  def unawardEmoji(projectID: EntityId, scope: AwardableScope, awardableId: BigInt, awardId: BigInt): EitherT[F, GitlabError, Unit] = {
    implicit val rId: RequestId = RequestId.newOne(s"unaward-$scope-emoji")
    val req = reqGen.delete(s"$API/projects/${projectID.toStringId}/$scope/$awardableId/award_emoji/$awardId", projectID)
    invokeRequest(req).map(_ => ())
  }

  def getEmojiAwards(mergeRequest: MergeRequestInfo): EitherT[F, GitlabError, Vector[EmojiAward]] =
    getEmojiAwards(mergeRequest.project_id, AwardableScope.MergeRequests, mergeRequest.iid)

  def awardEmoji(mergeRequest: MergeRequestInfo, emojiName: String): EitherT[F, GitlabError, EmojiAward] =
    awardEmoji(mergeRequest.project_id, AwardableScope.MergeRequests, mergeRequest.iid, emojiName)

  def unawardEmoji(mergeRequest: MergeRequestInfo, emojiAward: EmojiAward): EitherT[F, GitlabError, Unit] =
    unawardEmoji(mergeRequest.project_id, AwardableScope.MergeRequests, mergeRequest.iid, emojiAward.id)

  def awardMergeRequestEmoji(projectID: EntityId, mergeRequestIID: BigInt, emojiName: String): EitherT[F, GitlabError, EmojiAward] =
    awardEmoji(projectID, AwardableScope.MergeRequests, mergeRequestIID, emojiName)

  def getMergeRequestEmoji(projectID: EntityId, mergeRequestIID: BigInt): EitherT[F, GitlabError, Vector[EmojiAward]] =
    getEmojiAwards(projectID, AwardableScope.MergeRequests, mergeRequestIID)

}
