package io.morgaroth.gitlabclient.models

import io.circe.generic.extras._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Codec, Decoder}
import io.morgaroth.gitlabclient.marshalling.{EnumMarshalling, EnumMarshallingGlue}

import java.time.ZonedDateTime

sealed abstract class ReferenceType(val name: String) extends Product with Serializable

object ReferenceType extends EnumMarshallingGlue[ReferenceType] {

  case object Branch extends ReferenceType("branch")

  case object Tag extends ReferenceType("tag")

  val all: Seq[ReferenceType]            = Seq(Branch, Tag)
  val byName: Map[String, ReferenceType] = all.map(x => x.name -> x).toMap

  override def rawValue: ReferenceType => String = _.name

  implicit val MergeRequestStateCirceCodec: Codec[ReferenceType] = EnumMarshalling.stringEnumCodecOf(ReferenceType)

}

case class CommitSimple(
    id: String,
    short_id: String,
    title: String,
    author_name: String,
    author_email: String,
    authored_date: ZonedDateTime,
    committer_name: String,
    committer_email: String,
    committed_date: ZonedDateTime,
    created_at: ZonedDateTime,
    message: String,
    parent_ids: Vector[String],
)

object CommitSimple {
  implicit val CommitSimpleDecoder: Decoder[CommitSimple] = deriveDecoder[CommitSimple]
}

case class CommitStats(
    additions: Int,
    deletions: Int,
    total: Int,
)

object CommitStats {
  implicit val CommitStatsDecoder: Decoder[CommitStats] = deriveDecoder[CommitStats]
}

case class LastPipelineInfo(
    id: BigInt,
    ref: String,
    sha: String,
    status: String,
)

object LastPipelineInfo {
  implicit val LastPipelineInfoDecoder: Decoder[LastPipelineInfo] = deriveDecoder[LastPipelineInfo]
}

case class Commit(
    id: String,
    short_id: String,
    title: String,
    author_name: String,
    author_email: String,
    authored_date: ZonedDateTime,
    committer_name: String,
    committer_email: String,
    committed_date: ZonedDateTime,
    created_at: ZonedDateTime,
    message: String,
    parent_ids: Vector[String],
    web_url: String,
    stats: CommitStats,
    last_pipeline: Option[LastPipelineInfo],
    status: Option[String],
)

object Commit {
  implicit val CommitDecoder: Decoder[Commit] = deriveDecoder[Commit]
}

@ConfiguredJsonCodec
case class RefSimpleInfo(
    name: String,
    @JsonKey("type") kind: String,
)

case class FileDiff(
    diff: String,
    new_path: String,
    old_path: String,
    a_mode: String,
    b_mode: String,
    new_file: Boolean,
    renamed_file: Boolean,
    deleted_file: Boolean,
)

object FileDiff {
  implicit val FileDiffDecoder: Decoder[FileDiff] = deriveDecoder[FileDiff]
}

case class CommitReference(
    `type`: ReferenceType,
    name: String,
)

object CommitReference {
  implicit val CommitReferenceDecoder: Decoder[CommitReference] = deriveDecoder[CommitReference]
}
