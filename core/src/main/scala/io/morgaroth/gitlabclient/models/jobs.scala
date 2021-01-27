package io.morgaroth.gitlabclient.models

import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Codec, Decoder}
import io.morgaroth.gitlabclient.marshalling.{EnumMarshalling, EnumMarshallingGlue}

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
)

object JobFullInfo {
  implicit val JobFullInfoDecoder: Decoder[JobFullInfo] = deriveDecoder[JobFullInfo]
}

case class JobRunner(
    id: BigInt,
    description: String,
    ip_address: String,
    active: Boolean,
    is_shared: Boolean,
    name: String,
    online: Boolean,
    status: String,
)

object JobRunner {
  implicit val JobRunnerDecoder: Decoder[JobRunner] = deriveDecoder[JobRunner]
}
