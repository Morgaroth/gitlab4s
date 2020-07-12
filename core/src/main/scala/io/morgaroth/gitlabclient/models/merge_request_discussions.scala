package io.morgaroth.gitlabclient.models

import java.time.ZonedDateTime

import io.circe.Codec
import io.morgaroth.gitlabclient.marshalling.{EnumMarshalling, EnumMarshallingGlue}

sealed abstract class NoteType(val name: String) extends Product with Serializable

object NoteType {
  implicit val NoteTypeCirceCodec: Codec[NoteType] = EnumMarshalling.stringEnumCodecOf(NoteTypes)
}

object NoteTypes extends EnumMarshallingGlue[NoteType] {

  case object DiffNote extends NoteType("DiffNote")

  case object DiscussionNote extends NoteType("DiscussionNote")

  val all: Seq[NoteType] = Seq(DiffNote, DiscussionNote)
  val byName: Map[String, NoteType] = all.map(x => x.name -> x).toMap

  override def rawValue: NoteType => String = _.name
}

sealed abstract class NoteableType(val name: String) extends Product with Serializable

object NoteableType {
  implicit val NoteableTypesCirceCodec: Codec[NoteableType] = EnumMarshalling.stringEnumCodecOf(NoteableTypes)

}

object NoteableTypes extends EnumMarshallingGlue[NoteableType] {

  case object MergeRequest extends NoteableType("MergeRequest")

  case object Commit extends NoteableType("Commit")

  case object Issue extends NoteableType("Issue")

  val all: Seq[NoteableType] = Seq(MergeRequest, Commit, Issue)
  val byName: Map[String, NoteableType] = all.map(x => x.name -> x).toMap

  override def rawValue: NoteableType => String = _.name
}

case class NotePosition(
                         base_sha: Option[String], // not sure in what case
                         start_sha: String,
                         head_sha: String,
                         old_path: String,
                         new_path: String,
                         position_type: String,
                         old_line: Option[Int],
                         new_line: Option[Int],
                       )

case class MergeRequestNote(
                             id: BigInt,
                             `type`: Option[NoteType], // not present for system "new commits added..." etc, DiffNote when MR comment
                             body: String,
                             author: GitlabUser,
                             created_at: ZonedDateTime,
                             updated_at: ZonedDateTime,
                             system: Boolean,
                             noteable_type: NoteableType,
                             noteable_id: Option[BigInt], // when noteable_type is commit
                             noteable_iid: Option[BigInt], // when noteable_type is commit
                             position: Option[NotePosition], // not present for system "new commits added..." etc, present when MR comment
                             resolvable: Boolean,
                             resolved: Option[Boolean], // not present for system "new commits added..." etc, present when MR comment
                             resolved_by: Option[GitlabUser],
                           )

case class MergeRequestNoteCreate(
                                   body: String,
                                 )

case class MergeRequestDiscussion(
                                   id: String,
                                   individual_note: Boolean,
                                   notes: Vector[MergeRequestNote]
                                 )

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
  def apply(shaRefs: DiffRefs, change: FileDiff, newLine: Option[Int], oldLine: Option[Int]): NewThreadPosition =
    new NewThreadPosition(shaRefs.base_sha.get, shaRefs.start_sha, shaRefs.head_sha.get, "text", change.new_path, change.old_path, newLine, oldLine)
}

case class CreateMRDiscussion(
                               body: String,
                               position: Option[NewThreadPosition],
                             )

object CreateMRDiscussion {
  def mrDiscussion(body: String): CreateMRDiscussion =
    CreateMRDiscussion(body, None)

  def threadOnNewLine(diff: DiffRefs, change: FileDiff, line: Int, body: String): CreateMRDiscussion =
    CreateMRDiscussion(body, Some(NewThreadPosition(diff, change, Some(line), None)))

  def threadOnOldLine(diff: DiffRefs, change: FileDiff, line: Int, body: String): CreateMRDiscussion =
    CreateMRDiscussion(body, Some(NewThreadPosition(diff, change, None, Some(line))))
}

case class MRDiscussionUpdate private(
                                       body: Option[String],
                                       resolved: Option[Boolean],
                                     )

object MRDiscussionUpdate {
  def resolve(newValue: Boolean): MRDiscussionUpdate =
    MRDiscussionUpdate(None, Some(newValue))

  def body(newValue: String): MRDiscussionUpdate =
    MRDiscussionUpdate(Some(newValue), None)
}