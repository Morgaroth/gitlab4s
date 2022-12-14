package io.gitlab.mateuszjaje.gitlabclient
package models

import marshalling.{EnumMarshalling, EnumMarshallingGlue}
import models.ResponseTargetType.{DiffNote, DiscussionNote, Issue, MergeRequest, Note}

import cats.syntax.either._
import io.circe.{Codec, Decoder, DecodingFailure}

import java.time.ZonedDateTime

sealed abstract class TargetType(val name: String) extends Product with Serializable

object TargetTypes {

  case object Issue extends TargetType("issue")

  case object Milestone extends TargetType("milestone")

  case object MergeRequest extends TargetType("merge_request")

  case object Note extends TargetType("note")

  case object Project extends TargetType("project")

  case object Snippet extends TargetType("snippet")

  case object User extends TargetType("user")

}

sealed abstract class ActionType(val name: String) extends Product with Serializable

object ActionTypes {

  case object Created extends ActionType("created")

  case object Updated extends ActionType("updated")

  case object Closed extends ActionType("closed")

  case object Reopened extends ActionType("reopened")

  case object Pushed extends ActionType("pushed")

  case object Commented extends ActionType("commented")

  case object Merged extends ActionType("merged")

  case object Joined extends ActionType("joined")

  case object Left extends ActionType("left")

  case object Destroyed extends ActionType("destroyed")

  case object Expired extends ActionType("expired")

}

sealed abstract class ActionName(val name: String) extends Product with Serializable

object ActionName extends EnumMarshallingGlue[ActionName] {

  case object Approved extends ActionName("approved")

  case object Created extends ActionName("created")

  case object Joined extends ActionName("joined")

  case object Leaved extends ActionName("left")

  case object PushedTo extends ActionName("pushed to")

  case object PushedNew extends ActionName("pushed new")

  case object Opened extends ActionName("opened")

  case object CommentedOn extends ActionName("commented on")

  case object Accepted extends ActionName("accepted")

  case object Imported extends ActionName("imported")

  case object Closed extends ActionName("closed")

  case object Deleted extends ActionName("deleted")

  case object RemovedDueToExpiry extends ActionName("removed due to membership expiration from")

  val all = Seq(
    Approved,
    Created,
    Joined,
    Leaved,
    PushedTo,
    PushedNew,
    Opened,
    CommentedOn,
    Accepted,
    Imported,
    Closed,
    Deleted,
    RemovedDueToExpiry,
  )

  val byName = all.map(x => x.name -> x).toMap

  override def rawValue: ActionName => String = _.name

  implicit val ActionNameCirceCodec: Codec[ActionName] = EnumMarshalling.stringEnumCodecOf(ActionName)
}

sealed abstract class ResponseTargetType(val name: String) extends Product with Serializable

object ResponseTargetType extends EnumMarshallingGlue[ResponseTargetType] {

  case object MergeRequest extends ResponseTargetType("MergeRequest")

  case object DiffNote extends ResponseTargetType("DiffNote") // when some piece of code was commented, the MR's Diff page

  case object DiscussionNote extends ResponseTargetType("DiscussionNote") // when resolvable discussion was started on a MR page

  case object Note extends ResponseTargetType("Note") // when simple note was posted on a MR page

  case object Issue extends ResponseTargetType("Issue")

  val all    = Seq(MergeRequest, DiffNote, DiscussionNote, Note, Issue)
  val byName = all.map(x => x.name -> x).toMap

  override def rawValue: ResponseTargetType => String = _.name

  implicit val ActionNameCirceCodec: Codec[ResponseTargetType] = EnumMarshalling.stringEnumCodecOf(ResponseTargetType)
}

sealed trait PushData

object PushData {

  import io.circe.generic.semiauto._

  implicit val BranchPushDataCirceCodec: Decoder[RefPushData]  = deriveDecoder[RefPushData]
  implicit val BranchTagCreatedCirceCodec: Decoder[RefCreated] = deriveDecoder[RefCreated]
  implicit val RefRemovedCirceCodec: Decoder[RefRemoved]       = deriveDecoder[RefRemoved]

  implicit val buildCauseDecoder: Decoder[PushData] = Decoder.instance { cursor =>
    val actionField  = cursor.downField("action").as[String]
    val refTypeField = cursor.downField("ref_type").as[String]
    actionField.flatMap(a => refTypeField.map(a -> _)).flatMap {
      case ("created", "branch") => cursor.as[RefCreated]
      case ("created", "tag")    => cursor.as[RefCreated]
      case ("pushed", "branch")  => cursor.as[RefPushData]
      case ("pushed", "tag")     => cursor.as[RefPushData]
      case ("removed", "branch") => cursor.as[RefRemoved]
      case ("removed", "tag")    => cursor.as[RefRemoved]
      case unknown =>
        DecodingFailure(s"unknown combination of push_data.action&ref_type: $unknown for push data object", cursor.history).asLeft
    }
  }

}

case class RefPushData(
    commit_count: Int,
    action: String,
    ref_type: String,
    commit_from: String,
    commit_to: String,
    ref: String,
    commit_title: Option[String],
    ref_count: Option[Vector[String]],
) extends PushData

case class RefCreated(
    commit_count: Int,
    action: String,
    ref_type: String,
    commit_to: Option[String],
    ref: Option[String],
    commit_title: Option[String],
    ref_count: Option[Int],
) extends PushData

case class RefRemoved(
    commit_count: Int,
    action: String,
    ref_type: String,
    commit_from: String,
    ref: String,
) extends PushData

trait EventInfo {
  protected def action_name: ActionName

  protected def author_id: BigInt

  protected def project_id: BigInt

  protected def author_username: String

  protected def created_at: ZonedDateTime

  def author: GitlabUser

  def action = action_name

  def projectId = project_id

  def authorId = author_id

  def authorUsername = author_username

  def createdAt = created_at
}

case class ProjectCreatedEvent(
    project_id: BigInt,
    action_name: ActionName,
    created_at: ZonedDateTime,
    author: GitlabUser,
    author_id: BigInt,
    author_username: String,
) extends EventInfo

case class PushedEventInfo(
    project_id: BigInt,
    action_name: ActionName,
    created_at: ZonedDateTime,
    author: GitlabUser,
    author_id: BigInt,
    author_username: String,
    push_data: PushData,
) extends EventInfo

case class MREventInfo(
    project_id: BigInt,
    action_name: ActionName,
    created_at: ZonedDateTime,
    target_id: BigInt,
    target_iid: BigInt,
    target_type: ResponseTargetType,
    target_title: String,
    author: GitlabUser,
    author_id: BigInt,
    author_username: String,
) extends EventInfo

case class DiffNoteEvent(
    project_id: BigInt,
    action_name: ActionName,
    created_at: ZonedDateTime,
    target_id: BigInt,
    target_iid: BigInt,
    target_type: ResponseTargetType,
    target_title: String,
    note: MergeRequestNote,
    author: GitlabUser,
    author_id: BigInt,
    author_username: String,
) extends EventInfo

case class IssueEvent(
    project_id: BigInt,
    action_name: ActionName,
    created_at: ZonedDateTime,
    target_id: BigInt,
    target_iid: BigInt,
    target_type: ResponseTargetType,
    target_title: String,
    author: GitlabUser,
    author_id: BigInt,
    author_username: String,
) extends EventInfo

object EventInfo {
  import ActionName._
  import io.circe.generic.semiauto._

  implicit val GitlabUserCirceCodec: Decoder[GitlabUser]                   = deriveDecoder[GitlabUser]
  implicit val PushedEventInfoCirceCodec: Decoder[PushedEventInfo]         = deriveDecoder[PushedEventInfo]
  implicit val MREventInfoCirceCodec: Decoder[MREventInfo]                 = deriveDecoder[MREventInfo]
  implicit val NotePositionCirceCodec: Decoder[NotePosition]               = deriveDecoder[NotePosition]
  implicit val MergeRequestNoteCirceCodec: Decoder[MergeRequestNote]       = deriveDecoder[MergeRequestNote]
  implicit val DiffNoteEventCirceCodec: Decoder[DiffNoteEvent]             = deriveDecoder[DiffNoteEvent]
  implicit val IssueEventCirceCodec: Decoder[IssueEvent]                   = deriveDecoder[IssueEvent]
  implicit val ProjectCreatedEventCirceCodec: Decoder[ProjectCreatedEvent] = deriveDecoder[ProjectCreatedEvent]

  implicit val EventInfoDecoder: Decoder[EventInfo] = Decoder.instance { cursor =>
    val actionField = cursor.downField("action_name").as[ActionName]
    val targetType  = cursor.downField("target_type").as[Option[ResponseTargetType]]
    actionField.flatMap(a => targetType.map(a -> _)).flatMap {
      case (Created | Joined | Imported | Leaved | RemovedDueToExpiry, None) => cursor.as[ProjectCreatedEvent]
      case (PushedTo | PushedNew | Deleted, None)                            => cursor.as[PushedEventInfo]
      case (Approved | Opened | Accepted | Closed, Some(MergeRequest))       => cursor.as[MREventInfo]
      case (Opened | Closed, Some(Issue))                                    => cursor.as[IssueEvent]
      case (CommentedOn, Some(DiffNote | Note | DiscussionNote))             => cursor.as[DiffNoteEvent]
      case unknown =>
        DecodingFailure(s"unknown mapping for action_name & target_type: $unknown for EventInfo object", cursor.history).asLeft
    }
  }

}
