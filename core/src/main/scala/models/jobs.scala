package io.gitlab.mateuszjaje.gitlabclient
package models

import maintenance.MissingPropertiesLogger
import marshalling.{EnumMarshalling, EnumMarshallingGlue}

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import java.time.ZonedDateTime

sealed abstract class JobScope(val name: String) extends Product with Serializable

object JobScope {

  case object Created extends JobScope("created")

  case object Pending extends JobScope("pending")

  case object Running extends JobScope("running")

  case object Failed extends JobScope("failed")

  case object Success extends JobScope("success")

  case object Canceled extends JobScope("canceled")

  case object Skipped extends JobScope("skipped")

  case object Manual extends JobScope("manual")

}

sealed abstract class JobStatus(val name: String) extends Product with Serializable

object JobStatus extends EnumMarshallingGlue[JobStatus] {

  case object Created extends JobStatus("created")

  case object Success extends JobStatus("success")

  case object Skipped extends JobStatus("skipped")

  case object Failed extends JobStatus("failed")

  case object Canceled extends JobStatus("canceled")

  case object Running extends JobStatus("running")

  case object Scheduled extends JobStatus("scheduled")

  case object Manual extends JobStatus("manual")

  val all: Seq[JobStatus]            = Seq(Created, Running, Success, Skipped, Failed, Canceled, Scheduled, Manual)
  val byName: Map[String, JobStatus] = all.map(x => x.name -> x).toMap

  override def rawValue: JobStatus => String = _.name

  implicit val JobStatusCirceCodec: Codec[JobStatus] = EnumMarshalling.stringEnumCodecOf(JobStatus)
}

case class JobFullInfo(
    id: BigInt,
    status: JobStatus,
    stage: String,
    name: String,
    ref: String,
    tag: Boolean,
    //coverage ??
    allow_failure: Boolean,
    queued_duration: Option[Double],
    tag_list: Vector[String],
    artifacts_file: Option[ArtifactFile],
    created_at: ZonedDateTime,
    started_at: Option[ZonedDateTime],
    finished_at: Option[ZonedDateTime],
    duration: Double,
    user: GitlabUser,
    commit: CommitSimple,
    pipeline: PipelineShort,
    web_url: String,
    artifacts: Vector[PipelineArtifactSimple],
    runner: Option[JobRunner],
    artifacts_expire_at: Option[ZonedDateTime],
    coverage: Option[String],
    failure_reason: Option[String],
)

object JobFullInfo {
  implicit val JobFullInfoCodec: Codec[JobFullInfo] = MissingPropertiesLogger.loggingCodec(deriveCodec[JobFullInfo])
}

case class JobRunner(
    id: BigInt,
    description: String,
    ip_address: String,
    active: Boolean,
    is_shared: Boolean,
    runner_type: String,
    name: String,
    online: Boolean,
)

object JobRunner {
  implicit val JobRunnerCodec: Codec[JobRunner] = deriveCodec[JobRunner]
}
