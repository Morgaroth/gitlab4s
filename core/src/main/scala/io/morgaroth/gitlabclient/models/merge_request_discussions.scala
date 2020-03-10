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

  val all: Seq[NoteableType] = Seq(MergeRequest)
  val byName: Map[String, NoteableType] = all.map(x => x.name -> x).toMap

  override def rawValue: NoteableType => String = _.name
}

case class NotePosition(
                                     base_sha: String,
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
                             noteable_id: BigInt,
                             noteable_iid: BigInt,
                             noteable_type: NoteableType,
                             position: Option[NotePosition], // not present for system "new commits added..." etc, present when MR comment
                             resolvable: Boolean,
                             resolved: Option[Boolean], // not present for system "new commits added..." etc, present when MR comment
                             resolved_by: Option[GitlabUser],
                           )

case class MergeRequestDiscussion(
                                   id: String,
                                   individual_note: Boolean,
                                   notes: Vector[MergeRequestNote]
                                 )