package io.gitlab.mateuszjaje.gitlabclient
package models

import maintenance.MissingPropertiesLogger
import marshalling.{EnumMarshalling, EnumMarshallingGlue}

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

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
  implicit val CommitSimpleCodec: Codec[CommitSimple] = MissingPropertiesLogger.loggingCodec(deriveCodec[CommitSimple])
}

case class CommitStats(
    additions: Int,
    deletions: Int,
    total: Int,
)

object CommitStats {
  implicit val CommitStatsCodec: Codec[CommitStats] = deriveCodec[CommitStats]
}

case class LastPipelineInfo(
    id: BigInt,
    ref: String,
    sha: String,
    status: String,
)

object LastPipelineInfo {
  implicit val LastPipelineInfoCodec: Codec[LastPipelineInfo] = deriveCodec[LastPipelineInfo]
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
  implicit val CommitCodec: Codec[Commit] = deriveCodec[Commit]
}

case class RefSimpleInfo(
    name: String,
    `type`: String,
) {
  val kind = `type`
}

object RefSimpleInfo {
  implicit val RefSimpleInfoCodec: Codec[RefSimpleInfo] = deriveCodec
}

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
  implicit val FileDiffCodec: Codec[FileDiff] = MissingPropertiesLogger.loggingCodec(deriveCodec[FileDiff])
}

case class CommitReference(
    `type`: ReferenceType,
    name: String,
)

object CommitReference {
  implicit val CommitReferenceCodec: Codec[CommitReference] = deriveCodec[CommitReference]
}
