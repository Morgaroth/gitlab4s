package io.morgaroth.gitlabclient.models

import java.time.ZonedDateTime

import io.circe.Codec
import io.morgaroth.gitlabclient.marshalling.{EnumMarshalling, EnumMarshallingGlue}

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

  val all: Seq[MergeRequestState] = Seq(Open, Closed, Locked, Merged, All)
  val byName: Map[String, MergeRequestState] = all.map(x => x.name -> x).toMap

  override def rawValue: MergeRequestState => String = _.name
}


sealed abstract class MergeStatus(val name: String) extends Product with Serializable

object MergeStatus extends EnumMarshallingGlue[MergeStatus] {

  case object CanBeMerged extends MergeStatus("can_be_merged")

  case object CannotBeMerged extends MergeStatus("cannot_be_merged")

  val all: Seq[MergeStatus] = Seq(CanBeMerged, CannotBeMerged)
  val byName: Map[String, MergeStatus] = all.map(x => x.name -> x).toMap

  override def rawValue: MergeStatus => String = _.name

  implicit val MergeStatusCirceCodec: Codec[MergeStatus] = EnumMarshalling.stringEnumCodecOf(MergeStatus)
}

case class TaskStatus(count: Int, completed_count: Int)

case class ReferencesInfo(short: String, relative: String, full: String)

case class MergeRequestInfo(
                             id: BigInt,
                             iid: BigInt,
                             project_id: BigInt,
                             title: String,
                             description: String,
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
                             source_project_id: BigInt,
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
                             merge_when_pipeline_succeeds: Boolean,
                             should_remove_source_branch: Option[Boolean],
                             force_remove_source_branch: Option[Boolean],
                             allow_collaboration: Option[Boolean],
                             allow_maintainer_to_push: Option[Boolean],
                             squash: Boolean,
                             has_conflicts: Option[Boolean],
                             blocking_discussions_resolved: Option[Boolean],

                             web_url: String,
                             task_completion_status: TaskStatus
                           )

case class UpdateMRPayload(
                            target_branch: Option[String] = None,
                            title: Option[String] = None,
                            assignee_id: Option[Long] = None,
                            assignee_ids: Option[Vector[Long]] = None,
                            milestone_id: Option[Long] = None,
                            labels: Option[Vector[String]] = None,
                            description: Option[String] = None,
                            state_event: Option[MergeRequestState] = None,
                            remove_source_branch: Option[Boolean] = None,
                            squash: Option[Boolean] = None,
                            discussion_locked: Option[Boolean] = None,
                            allow_collaboration: Option[Boolean] = None,
                            allow_maintainer_to_push: Option[Boolean] = None,
                          )

object UpdateMRPayload {
  def description(newValue: String): UpdateMRPayload = new UpdateMRPayload(description = Some(newValue))
}

case class MergeRequestFull(
                             id: BigInt,
                             iid: BigInt,
                             project_id: BigInt,
                             title: String,
                             description: String,
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
                             source_project_id: BigInt,
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
                             head_pipeline: Option[Pipeline],
                             pipeline: Option[PipelineShort],

                             merge_error: Option[String],
                             user: UserMergeInfo,
                           )

case class UserMergeInfo(can_merge: Boolean)

case class DiffRefs(
                     base_sha: Option[String],
                     head_sha: Option[String],
                     start_sha: String,
                   )

sealed abstract class PipelineStatus(val name: String) extends Product with Serializable

object PipelineStatus extends EnumMarshallingGlue[PipelineStatus] {

  case object Success extends PipelineStatus("success")

  case object Skipped extends PipelineStatus("skipped")

  case object Failed extends PipelineStatus("failed")

  case object Canceled extends PipelineStatus("canceled")

  case object Running extends PipelineStatus("running")

  val all: Seq[PipelineStatus] = Seq(Success, Skipped, Failed, Canceled, Running)
  val byName: Map[String, PipelineStatus] = all.map(x => x.name -> x).toMap

  override def rawValue: PipelineStatus => String = _.name

  implicit val PipelineStatusCirceCodec: Codec[PipelineStatus] = EnumMarshalling.stringEnumCodecOf(PipelineStatus)
}

case class PipelineShort(
                          id: BigInt,
                          sha: String,
                          ref: String,
                          status: PipelineStatus,
                          created_at: ZonedDateTime,
                          updated_at: ZonedDateTime,
                          web_url: String,
                        )

case class Pipeline(
                     id: BigInt,
                     sha: String,
                     ref: String,
                     status: PipelineStatus,
                     created_at: ZonedDateTime,
                     updated_at: ZonedDateTime,
                     web_url: String,
                     before_sha: String,
                     tag: Boolean,
                     yaml_errors: Option[String],
                     user: GitlabUser,
                     started_at: Option[ZonedDateTime],
                     finished_at: Option[ZonedDateTime],
                     committed_at: Option[ZonedDateTime],
                     duration: Option[Int],
                     coverage: Option[Boolean],
                     detailed_status: PipelineStatusInfo,
                   )

case class PipelineStatusInfo(
                               icon: String,
                               text: String,
                               label: String,
                               group: String,
                               tooltip: String,
                               has_details: Boolean,
                               details_path: String,
                               illustration: Option[String],
                               favicon: String,
                             )