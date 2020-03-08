package io.morgaroth.gitlabclient.models

import java.time.ZonedDateTime

import io.circe.Codec
import io.morgaroth.gitlabclient.marshalling.{EnumMarshalling, EnumMarshallingGlue}

sealed abstract class MergeRequestNoteType(val name: String) extends Product with Serializable

object MergeRequestNoteType {
  implicit val MergeRequestNoteTypeCirceCodec: Codec[MergeRequestNoteType] = EnumMarshalling.stringEnumCodecOf(MergeRequestNoteTypes)
}

object MergeRequestNoteTypes extends EnumMarshallingGlue[MergeRequestNoteType] {

  case object DiffNote extends MergeRequestNoteType("DiffNote")

  val all: Seq[MergeRequestNoteType] = Seq(DiffNote)
  val byName: Map[String, MergeRequestNoteType] = all.map(x => x.name -> x).toMap

  override def rawValue: MergeRequestNoteType => String = _.name
}

sealed abstract class MergeRequestNoteableType(val name: String) extends Product with Serializable

object MergeRequestNoteableType {
  implicit val MergeRequestNoteableTypesCirceCodec: Codec[MergeRequestNoteableType] = EnumMarshalling.stringEnumCodecOf(MergeRequestNoteableTypes)

}

object MergeRequestNoteableTypes extends EnumMarshallingGlue[MergeRequestNoteableType] {

  case object MergeRequest extends MergeRequestNoteableType("MergeRequest")

  val all: Seq[MergeRequestNoteableType] = Seq(MergeRequest)
  val byName: Map[String, MergeRequestNoteableType] = all.map(x => x.name -> x).toMap

  override def rawValue: MergeRequestNoteableType => String = _.name
}

case class MergeRequestNotePosition(
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
                             `type`: Option[MergeRequestNoteType], // not present for system "new commits added..." etc, DiffNote when MR comment
                             body: String,
                             author: GitlabUser,
                             created_at: ZonedDateTime,
                             updated_at: ZonedDateTime,
                             system: Boolean,
                             noteable_id: BigInt,
                             noteable_iid: BigInt,
                             noteable_type: MergeRequestNoteableType,
                             position: Option[MergeRequestNotePosition], // not present for system "new commits added..." etc, present when MR comment
                             resolvable: Boolean,
                             resolved: Option[Boolean], // not present for system "new commits added..." etc, present when MR comment
                             resolved_by: Option[GitlabUser],
                           )