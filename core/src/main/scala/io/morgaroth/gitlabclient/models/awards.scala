package io.morgaroth.gitlabclient.models

import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Codec, Decoder}
import io.morgaroth.gitlabclient.marshalling.{EnumMarshalling, EnumMarshallingGlue}

import java.time.ZonedDateTime

sealed abstract class AwardableScope(val name: String) extends Product with Serializable {
  override def toString: String = name
}

object AwardableScope {

  final case object MergeRequests extends AwardableScope("merge_requests")

  final case object Issues extends AwardableScope("issues")

  final case object Snippets extends AwardableScope("snippets")

  val all: Seq[AwardableScope] = Seq(MergeRequests, Issues, Snippets)
  val byName: Map[String, AwardableScope] = all.map(x => x.name -> x).toMap

  val awardableTypeToScope = Map(
    AwardableType.MergeRequest -> AwardableScope.MergeRequests,
    AwardableType.Issue -> AwardableScope.Issues,
    AwardableType.Snippet -> AwardableScope.Snippets,
  )

  def fromAwardableType(awardableType: AwardableType): AwardableScope = awardableTypeToScope(awardableType)
}

sealed abstract class AwardableType(val name: String) extends Product with Serializable

object AwardableType extends EnumMarshallingGlue[AwardableType] {

  final case object MergeRequest extends AwardableType("MergeRequest")

  final case object Issue extends AwardableType("Issue")

  final case object Snippet extends AwardableType("Snippet")

  val all: Seq[AwardableType] = Seq(MergeRequest, Issue, Snippet)
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
                       awardable_type: AwardableType
                     )

object EmojiAward {
  implicit val EmojiAwardDecoder: Decoder[EmojiAward] = deriveDecoder[EmojiAward]
}