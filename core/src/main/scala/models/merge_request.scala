package io.gitlab.mateuszjaje.gitlabclient
package models

import maintenance.MissingPropertiesLogger
import marshalling.{EnumMarshalling, EnumMarshallingGlue}

import io.circe.generic.semiauto.{deriveCodec, deriveEncoder}
import io.circe.{Codec, Encoder}

import java.time.ZonedDateTime

sealed abstract class MergeRequestState(val name: String) extends Product with Serializable

object MergeRequestState {
  implicit val MergeRequestStateCirceCodec: Codec[MergeRequestState] = EnumMarshalling.stringEnumCodecOf(MergeRequestStates)
}

object MergeRequestStates extends EnumMarshallingGlue[MergeRequestState] {

  case object Open extends MergeRequestState("opened")

  case object Closed extends MergeRequestState("closed")

  case object Locked extends MergeRequestState("locked")

  case object Merged extends MergeRequestState("merged")

  case object All extends MergeRequestState("all")

  val all: Seq[MergeRequestState]            = Seq(Open, Closed, Locked, Merged, All)
  val byName: Map[String, MergeRequestState] = all.map(x => x.name -> x).toMap

  override def rawValue: MergeRequestState => String = _.name
}

sealed abstract class MergeStatus(val name: String) extends Product with Serializable

object MergeStatus extends EnumMarshallingGlue[MergeStatus] {

  case object CanBeMerged extends MergeStatus("can_be_merged")

  case object CannotBeMerged extends MergeStatus("cannot_be_merged")

  case object Checking extends MergeStatus("checking")

  case object Unchecked extends MergeStatus("unchecked")

  case object CannotBeMergedRecheck extends MergeStatus("cannot_be_merged_recheck")

  val all: Seq[MergeStatus] = Seq(
    CanBeMerged,
    CannotBeMerged,
    Checking,
    Unchecked,
    CannotBeMergedRecheck,
  )

  val byName: Map[String, MergeStatus] = all.map(x => x.name -> x).toMap

  override def rawValue: MergeStatus => String = _.name

  implicit val MergeStatusCirceCodec: Codec[MergeStatus] = EnumMarshalling.stringEnumCodecOf(MergeStatus)
}

case class TaskStatus(count: Int, completed_count: Int)

object TaskStatus {
  implicit val TaskStatusCodec: Codec[TaskStatus] = MissingPropertiesLogger.loggingCodec(deriveCodec[TaskStatus])
}

case class ReferencesInfo(short: String, relative: String, full: String)

object ReferencesInfo {
  implicit val ReferencesInfoCodec: Codec[ReferencesInfo] = MissingPropertiesLogger.loggingCodec(deriveCodec[ReferencesInfo])
}

trait MergeRequestID {
  def id: BigInt
  def iid: BigInt
  def project_id: BigInt
}

trait MergeRequestSimple extends MergeRequestID {
  def title: String
  def description: Option[String]
  def state: MergeRequestState
  def author: GitlabUser
  def created_at: ZonedDateTime
  def updated_at: ZonedDateTime
  def merged_by: Option[GitlabUser]
  def merged_at: Option[ZonedDateTime]
  def closed_by: Option[GitlabUser]
  def closed_at: Option[ZonedDateTime]
  def target_branch: String
  def source_branch: String
  def source_project_id: Option[BigInt]
  def target_project_id: BigInt
  def references: ReferencesInfo
  def web_url: String
}

case class MergeRequestInfo(
    id: BigInt,
    iid: BigInt,
    project_id: BigInt,
    title: String,
    description: Option[String],
    state: MergeRequestState,
    merged_by: Option[GitlabUser],
    merged_at: Option[ZonedDateTime],
    closed_by: Option[GitlabUser],
    closed_at: Option[ZonedDateTime],
    created_at: ZonedDateTime,
    updated_at: ZonedDateTime,
    target_branch: String,
    source_branch: String,
    upvotes: Int,
    downvotes: Int,
    author: GitlabUser,
    assignee: Option[GitlabUser],
    assignees: Option[Vector[GitlabUser]],
    source_project_id: Option[BigInt],
    target_project_id: BigInt,
    labels: Vector[String],
    merge_status: MergeStatus,
    sha: Option[String],
    merge_commit_sha: Option[String],
    squash_commit_sha: Option[String],
    user_notes_count: Int,
    reference: String,
    references: ReferencesInfo,
    discussion_locked: Option[Boolean],
    work_in_progress: Boolean,
    draft: Boolean,
    merge_when_pipeline_succeeds: Boolean,
    should_remove_source_branch: Option[Boolean],
    force_remove_source_branch: Option[Boolean],
    allow_collaboration: Option[Boolean],
    allow_maintainer_to_push: Option[Boolean],
    squash: Boolean,
    has_conflicts: Option[Boolean],
    blocking_discussions_resolved: Option[Boolean],
    web_url: String,
    task_completion_status: TaskStatus,
    milestone: Option[String],
    reviewers: Vector[GitlabUser],
    approvals_before_merge: Option[Int],
    time_stats: TimeStats,
) extends MergeRequestSimple

object MergeRequestInfo {
  implicit val MergeRequestInfoCodec: Codec[MergeRequestInfo] = MissingPropertiesLogger.loggingCodec(deriveCodec[MergeRequestInfo])
}

abstract class UpdateMergeRequestState(val name: String) extends Product with Serializable

object UpdateMergeRequestState {
  case object Reopen extends UpdateMergeRequestState("reopen")

  case object Close extends UpdateMergeRequestState("close")

  implicit val updateMRPayloadEncoder: Encoder[UpdateMergeRequestState] = Encoder.encodeString.contramap(_.name)
}

case class UpdateMRPayload(
    target_branch: Option[String] = None,
    title: Option[String] = None,
    assignee_id: Option[Long] = None,
    assignee_ids: Option[Vector[Long]] = None,
    milestone_id: Option[Long] = None,
    labels: Option[Vector[String]] = None,
    description: Option[String] = None,
    state_event: Option[UpdateMergeRequestState] = None,
    remove_source_branch: Option[Boolean] = None,
    squash: Option[Boolean] = None,
    discussion_locked: Option[Boolean] = None,
    allow_collaboration: Option[Boolean] = None,
    allow_maintainer_to_push: Option[Boolean] = None,
)

object UpdateMRPayload {
  implicit val updateMRPayloadEncoder: Encoder[UpdateMRPayload] = deriveEncoder[UpdateMRPayload]

  def description(newValue: String): UpdateMRPayload = new UpdateMRPayload(description = Some(newValue))
}

case class TimeStats(
    time_estimate: Int,
    total_time_spent: Int,
    human_time_estimate: Option[String],
    human_total_time_spent: Option[String],
)

object TimeStats {
  implicit val TimeStatsCodec: Codec[TimeStats] = MissingPropertiesLogger.loggingCodec(deriveCodec[TimeStats])
}

case class MergeRequestFull(
    id: BigInt,
    iid: BigInt,
    project_id: BigInt,
    title: String,
    description: Option[String],
    state: MergeRequestState,
    merged_by: Option[GitlabUser],
    merged_at: Option[ZonedDateTime],
    closed_by: Option[GitlabUser],
    closed_at: Option[ZonedDateTime],
    created_at: ZonedDateTime,
    updated_at: ZonedDateTime,
    target_branch: String,
    source_branch: String,
    upvotes: Int,
    downvotes: Int,
    author: GitlabUser,
    assignee: Option[GitlabUser],
    assignees: Option[Vector[GitlabUser]],
    source_project_id: Option[BigInt],
    target_project_id: BigInt,
    labels: Vector[String],
    merge_status: Option[MergeStatus],
    sha: Option[String],
    merge_commit_sha: Option[String],
    squash_commit_sha: Option[String],
    user_notes_count: Int,
    reference: String,
    references: ReferencesInfo,
    discussion_locked: Option[Boolean],
    work_in_progress: Boolean,
    draft: Boolean,
    merge_when_pipeline_succeeds: Boolean,
    should_remove_source_branch: Option[Boolean],
    force_remove_source_branch: Option[Boolean],
    allow_collaboration: Option[Boolean],
    allow_maintainer_to_push: Option[Boolean],
    squash: Boolean,
    has_conflicts: Option[Boolean],
    blocking_discussions_resolved: Option[Boolean],
    web_url: String,
    task_completion_status: TaskStatus,
    // new fields
    subscribed: Boolean,
    changes_count: Option[String],
    latest_build_started_at: Option[ZonedDateTime],
    latest_build_finished_at: Option[ZonedDateTime],
    first_deployed_to_production_at: Option[ZonedDateTime],
    approvals_before_merge: Option[Int],
    changes: Option[Vector[FileDiff]],
    diff_refs: DiffRefs,
    head_pipeline: Option[PipelineFullInfo],
    pipeline: Option[PipelineShort],
    merge_error: Option[String],
    user: UserMergeInfo,
    milestone: Option[String],
    time_stats: TimeStats,
    reviewers: Vector[GitlabUser],
    overflow: Option[Boolean],
) extends MergeRequestSimple

object MergeRequestFull {
  implicit val MergeRequestFullCodec: Codec[MergeRequestFull] = MissingPropertiesLogger.loggingCodec(deriveCodec[MergeRequestFull])
}

case class UserMergeInfo(can_merge: Boolean)

object UserMergeInfo {
  implicit val UserMergeInfoCodec: Codec[UserMergeInfo] = MissingPropertiesLogger.loggingCodec(deriveCodec[UserMergeInfo])
}

case class DiffRefs(
    base_sha: Option[String],
    head_sha: Option[String],
    start_sha: String,
)

object DiffRefs {
  implicit val DiffRefsCodec: Codec[DiffRefs] = MissingPropertiesLogger.loggingCodec(deriveCodec[DiffRefs])
}

sealed abstract class MergeRequestSearchScope(val name: String) extends Product with Serializable

object MergeRequestSearchScope {
  case object CreatedByMe  extends MergeRequestSearchScope("created_by_me")
  case object AssignedToMe extends MergeRequestSearchScope("assigned_to_me")
  case object All          extends MergeRequestSearchScope("all")
}
