package io.morgaroth.gitlabclient.models

import java.time.ZonedDateTime

import io.morgaroth.gitlabclient.marshalling.EnumMarshallingGlue

sealed abstract class AwardableScope(val name: String) {
  override def toString: String = name
}

object AwardableScope {

  final case object MergeRequests extends AwardableScope("merge_requests")

  final case object Issues extends AwardableScope("issues")

  final case object Snippets extends AwardableScope("snippets")

  val all: Seq[AwardableScope] = Seq(MergeRequests, Issues, Snippets)
  val byName: Map[String, AwardableScope] = all.map(x => x.name -> x).toMap
}

sealed abstract class AwardableType(val name: String) extends Product with Serializable

object AwardableType extends EnumMarshallingGlue[AwardableType] {

  final case object MergeRequest extends AwardableType("MergeRequest")

  final case object Issue extends AwardableType("Issue")

  final case object Snippet extends AwardableType("Snippet")

  val all: Seq[AwardableType] = Seq(MergeRequest, Issue, Snippet)
  val byName: Map[String, AwardableType] = all.map(x => x.name -> x).toMap

  override def rawValue: AwardableType => String = _.name
}

case class EmojiAward(
                       id: BigInt,
                       name: String,
                       user: GitlabUser,
                       created_at: ZonedDateTime,
                       updated_at: ZonedDateTime,
                       awardable_id: Int,
                       awardable_type: AwardableType
                     )