package io.gitlab.mateuszjaje.gitlabclient
package models

import maintenance.MissingPropertiesLogger
import marshalling.{EnumMarshalling, EnumMarshallingGlue}

import io.circe.generic.semiauto.{deriveCodec, deriveEncoder}
import io.circe.{Codec, Encoder}

import java.time.ZonedDateTime

sealed abstract class NoteType(val name: String) extends Product with Serializable

object NoteType {
  implicit val NoteTypeCirceCodec: Codec[NoteType] = EnumMarshalling.stringEnumCodecOf(NoteTypes)
}

object NoteTypes extends EnumMarshallingGlue[NoteType] {

  case object Note     extends NoteType("Note")
  case object DiffNote extends NoteType("DiffNote")

  case object DiscussionNote extends NoteType("DiscussionNote")

  val all: Seq[NoteType]            = Seq(Note, DiffNote, DiscussionNote)
  val byName: Map[String, NoteType] = all.map(x => x.name -> x).toMap

  override def rawValue: NoteType => String = _.name
}

sealed abstract class LineRangeType(val name: String) extends Product with Serializable

object LineRangeType {
  implicit val LineRangeTypeCirceCodec: Codec[LineRangeType] = EnumMarshalling.stringEnumCodecOf(LineRangeTypes)
}

object LineRangeTypes extends EnumMarshallingGlue[LineRangeType] {

  case object New      extends LineRangeType("new")
  case object Old      extends LineRangeType("old")
  case object Expanded extends LineRangeType("expanded")

  val all: Seq[LineRangeType]            = Seq(New, Old, Expanded)
  val byName: Map[String, LineRangeType] = all.map(x => x.name -> x).toMap

  override def rawValue: LineRangeType => String = _.name
}

sealed abstract class NoteableType(val name: String) extends Product with Serializable

object NoteableType {
  implicit val NoteableTypesCirceCodec: Codec[NoteableType] = EnumMarshalling.stringEnumCodecOf(NoteableTypes)
}

object NoteableTypes extends EnumMarshallingGlue[NoteableType] {

  case object MergeRequest extends NoteableType("MergeRequest")

  case object Commit extends NoteableType("Commit")

  case object Issue extends NoteableType("Issue")

  val all: Seq[NoteableType]            = Seq(MergeRequest, Commit, Issue)
  val byName: Map[String, NoteableType] = all.map(x => x.name -> x).toMap

  override def rawValue: NoteableType => String = _.name
}

case class LineDef(
    line_code: String,
    `type`: Option[LineRangeType],
    old_line: Option[Int],
    new_line: Option[Int],
)

object LineDef {
  implicit val LineDefCodec: Codec[LineDef] = MissingPropertiesLogger.loggingCodec(deriveCodec[LineDef])
}

case class LineRange(
    start: LineDef,
    end: LineDef,
)

object LineRange {
  implicit val LineRangeCodec: Codec[LineRange] = MissingPropertiesLogger.loggingCodec(deriveCodec[LineRange])
}

case class NotePosition(
    base_sha: Option[String], // not sure in what case
    start_sha: String,
    head_sha: String,
    old_path: Option[String],
    new_path: String,
    position_type: String,
    old_line: Option[Int],
    new_line: Option[Int],
    line_range: Option[LineRange],
)

object NotePosition {
  implicit val NotePositionCodec: Codec[NotePosition] = MissingPropertiesLogger.loggingCodec(deriveCodec[NotePosition])
}

case class MergeRequestNote(
    id: BigInt,
    `type`: Option[NoteType], // not present for system "new commits added..." etc, DiffNote when MR comment
    body: String,
    author: GitlabUser,
    created_at: ZonedDateTime,
    updated_at: ZonedDateTime,
    system: Boolean,
    commit_id: Option[String],
    noteable_type: NoteableType,
    noteable_id: Option[BigInt],    // when noteable_type is commit
    noteable_iid: Option[BigInt],   // when noteable_type is commit
    position: Option[NotePosition], // not present for system "new commits added..." etc, present when MR comment
    resolvable: Boolean,
    resolved: Option[Boolean], // not present for system "new commits added..." etc, present when MR comment
    resolved_by: Option[GitlabUser],
    resolved_at: Option[ZonedDateTime],
    attachment: Option[Int],
    confidential: Boolean,
    commands_changes: Map[String, Int],
)

object MergeRequestNote {
  implicit val MergeRequestNoteCodec: Codec[MergeRequestNote] = MissingPropertiesLogger.loggingCodec(deriveCodec[MergeRequestNote])
}

case class MergeRequestNoteCreate(
    body: String,
)

object MergeRequestNoteCreate {
  implicit val MergeRequestNoteCreateEncoder: Encoder[MergeRequestNoteCreate] = deriveEncoder[MergeRequestNoteCreate]
}

case class MergeRequestDiscussion(
    id: String,
    individual_note: Boolean,
    notes: Vector[MergeRequestNote],
)

object MergeRequestDiscussion {
  implicit val MergeRequestDiscussionCodec: Codec[MergeRequestDiscussion] = deriveCodec[MergeRequestDiscussion]
}

case class NewThreadPosition(
    base_sha: String,
    start_sha: String,
    head_sha: String,
    position_type: String,
    new_path: String,
    old_path: String,
    new_line: Option[Int],
    old_line: Option[Int],
)

object NewThreadPosition {
  implicit val NewThreadPositionEncoder: Encoder[NewThreadPosition] = deriveEncoder[NewThreadPosition]

  def apply(shaRefs: DiffRefs, change: FileDiff, newLine: Option[Int], oldLine: Option[Int]): NewThreadPosition =
    new NewThreadPosition(
      shaRefs.base_sha.get,
      shaRefs.start_sha,
      shaRefs.head_sha.get,
      "text",
      change.new_path,
      change.old_path,
      newLine,
      oldLine,
    )

}

case class CreateMRDiscussion(
    body: String,
    position: Option[NewThreadPosition],
)

object CreateMRDiscussion {
  implicit val createMRDiscussionEncoder: Encoder[CreateMRDiscussion] = deriveEncoder[CreateMRDiscussion]

  def mrDiscussion(body: String): CreateMRDiscussion =
    CreateMRDiscussion(body, None)

  def threadOnNewLine(diff: DiffRefs, change: FileDiff, line: Int, body: String): CreateMRDiscussion =
    CreateMRDiscussion(body, Some(NewThreadPosition(diff, change, Some(line), None)))

  def threadOnOldLine(diff: DiffRefs, change: FileDiff, line: Int, body: String): CreateMRDiscussion =
    CreateMRDiscussion(body, Some(NewThreadPosition(diff, change, None, Some(line))))

}

case class MRDiscussionUpdate private (
    body: Option[String],
    resolved: Option[Boolean],
)

object MRDiscussionUpdate {
  implicit val MRDiscussionUpdateEncoder: Encoder[MRDiscussionUpdate] = deriveEncoder[MRDiscussionUpdate]

  def resolve(newValue: Boolean): MRDiscussionUpdate =
    MRDiscussionUpdate(None, Some(newValue))

  def body(newValue: String): MRDiscussionUpdate =
    MRDiscussionUpdate(Some(newValue), None)

}
