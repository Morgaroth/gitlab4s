package io.gitlab.mateuszjaje.gitlabclient
package models

import marshalling.{EnumMarshalling, EnumMarshallingGlue}

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import java.time.ZonedDateTime

sealed abstract class AwardableScope(val name: String) extends Product with Serializable {
  override def toString: String = name
}

object AwardableScope {

  case object MergeRequests extends AwardableScope("merge_requests")

  case object Issues extends AwardableScope("issues")

  case object Snippets extends AwardableScope("snippets")

  val all: Seq[AwardableScope]            = Seq(MergeRequests, Issues, Snippets)
  val byName: Map[String, AwardableScope] = all.map(x => x.name -> x).toMap

  val awardableTypeToScope = Map(
    AwardableType.MergeRequest -> AwardableScope.MergeRequests,
    AwardableType.Issue        -> AwardableScope.Issues,
    AwardableType.Snippet      -> AwardableScope.Snippets,
  )

  def fromAwardableType(awardableType: AwardableType): AwardableScope = awardableTypeToScope(awardableType)
}

sealed abstract class AwardableType(val name: String) extends Product with Serializable

object AwardableType extends EnumMarshallingGlue[AwardableType] {

  case object MergeRequest extends AwardableType("MergeRequest")

  case object Issue extends AwardableType("Issue")

  case object Snippet extends AwardableType("Snippet")

  val all: Seq[AwardableType]            = Seq(MergeRequest, Issue, Snippet)
  val byName: Map[String, AwardableType] = all.map(x => x.name -> x).toMap

  override def rawValue: AwardableType => String = _.name

  implicit val AwardableTypeCirceCodec: Codec[AwardableType] = EnumMarshalling.stringEnumCodecOf(AwardableType)
}

case class EmojiAward(
    id: BigInt,
    name: String,
    user: GitlabUser,
    created_at: ZonedDateTime,
    updated_at: ZonedDateTime,
    awardable_id: Int,
    awardable_type: AwardableType,
)

object EmojiAward {
  implicit val EmojiAwardCodec: Codec[EmojiAward] = deriveCodec[EmojiAward]
}
