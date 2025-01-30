package io.gitlab.mateuszjaje.gitlabclient
package models

import maintenance.MissingPropertiesLogger
import marshalling.{EnumMarshalling, EnumMarshallingGlue}

import io.circe.generic.semiauto.{deriveCodec, deriveEncoder}
import io.circe.{Codec, Encoder}

import java.time.ZonedDateTime

sealed abstract class PipelineStatus(val name: String) extends Product with Serializable

object PipelineStatus extends EnumMarshallingGlue[PipelineStatus] {

  case object Created extends PipelineStatus("created")

  case object Success extends PipelineStatus("success")

  case object Skipped extends PipelineStatus("skipped")

  case object Failed extends PipelineStatus("failed")

  case object Canceled extends PipelineStatus("canceled")

  case object Pending extends PipelineStatus("pending")

  case object Running extends PipelineStatus("running")

  case object Scheduled extends PipelineStatus("scheduled")

  case object Manual extends PipelineStatus("manual")

  case object WaitingForResource extends PipelineStatus("waiting_for_resource")

  val all: Seq[PipelineStatus] = Seq(Created, Success, Skipped, Failed, Canceled, Pending, Running, Scheduled, Manual, WaitingForResource)
  val byName: Map[String, PipelineStatus] = all.map(x => x.name -> x).toMap

  override def rawValue: PipelineStatus => String = _.name

  implicit val PipelineStatusCirceCodec: Codec[PipelineStatus] = EnumMarshalling.stringEnumCodecOf(PipelineStatus)
}

sealed abstract class PipelineSource(val name: String) extends Product with Serializable

object PipelineSource extends EnumMarshallingGlue[PipelineSource] {

  case object API                      extends PipelineSource("api")
  case object Push                     extends PipelineSource("push")
  case object Web                      extends PipelineSource("web")
  case object Schedule                 extends PipelineSource("schedule")
  case object MergeRequestEvent        extends PipelineSource("merge_request_event")
  case object Trigger                  extends PipelineSource("trigger")
  case object External                 extends PipelineSource("external")
  case object Pipeline                 extends PipelineSource("pipeline")
  case object Chat                     extends PipelineSource("chat")
  case object WebIDE                   extends PipelineSource("webide")
  case object ExternalPullRequestEvent extends PipelineSource("external_pull_request_event")
  case object ParentPipeline           extends PipelineSource("parent_pipeline")
  case object OnDemandDastScan         extends PipelineSource("ondemand_dast_scan")
  case object OnDemandDastValidation   extends PipelineSource("ondemand_dast_validation")

  val all: Seq[PipelineSource] = Seq(
    API,
    Push,
    Web,
    Schedule,
    MergeRequestEvent,
    Trigger,
    External,
    Pipeline,
    Chat,
    WebIDE,
    ExternalPullRequestEvent,
    ParentPipeline,
    OnDemandDastScan,
    OnDemandDastValidation,
  )

  val byName: Map[String, PipelineSource] = all.map(x => x.name -> x).toMap

  override def rawValue: PipelineSource => String = _.name

  implicit val PipelineSourceCirceCodec: Codec[PipelineSource] = EnumMarshalling.stringEnumCodecOf(PipelineSource)
}

sealed abstract class PipelineScope(val name: String) extends Product with Serializable

object PipelineScope {

  case object Pending extends PipelineScope("pending")

  case object Running extends PipelineScope("running")

  case object Finished extends PipelineScope("finished")

  case object Branches extends PipelineScope("branches")

  case object Tags extends PipelineScope("tags")

}

case class PipelineShort(
    id: BigInt,
    iid: BigInt,
    project_id: BigInt,
    sha: String,
    ref: String,
    status: PipelineStatus,
    created_at: ZonedDateTime,
    updated_at: ZonedDateTime,
    web_url: String,
    source: PipelineSource,
)

object PipelineShort {
  implicit val PipelineShortCodec: Codec[PipelineShort] = MissingPropertiesLogger.loggingCodec(deriveCodec[PipelineShort])
}

case class PipelineFullInfo(
    id: BigInt,
    iid: BigInt,
    project_id: BigInt,
    sha: String,
    ref: String,
    name: Option[String],
    status: PipelineStatus,
    created_at: ZonedDateTime,
    updated_at: ZonedDateTime,
    web_url: String,
    before_sha: String,
    tag: Boolean,
    yaml_errors: Option[String],
    user: Option[GitlabUser],
    started_at: Option[ZonedDateTime],
    finished_at: Option[ZonedDateTime],
    committed_at: Option[ZonedDateTime],
    duration: Option[Int],
    coverage: Option[String],
    detailed_status: PipelineStatusInfo,
    queued_duration: Option[Int],
    source: PipelineSource,
)

object PipelineFullInfo {
  implicit val PipelineFullInfoCodec: Codec[PipelineFullInfo] = MissingPropertiesLogger.loggingCodec(deriveCodec[PipelineFullInfo])
}

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

object PipelineStatusInfo {
  implicit val PipelineStatusInfoCodec: Codec[PipelineStatusInfo] = MissingPropertiesLogger.loggingCodec(deriveCodec[PipelineStatusInfo])
}

sealed abstract class PipelineVariableType(val name: String) extends Product with Serializable

object PipelineVariableType extends EnumMarshallingGlue[PipelineVariableType] {

  case object File extends PipelineVariableType("file")

  case object Text extends PipelineVariableType("env_var")

  val all: Seq[PipelineVariableType]            = Seq(File, Text)
  val byName: Map[String, PipelineVariableType] = all.map(x => x.name -> x).toMap

  override def rawValue: PipelineVariableType => String = _.name

  implicit val PipelineStatusCirceCodec: Codec[PipelineVariableType] = EnumMarshalling.stringEnumCodecOf(PipelineVariableType)
}

case class PipelineVar(
    key: String,
    value: String,
    variable_type: PipelineVariableType,
)

object PipelineVar {
  def text(name: String, value: String)       = new PipelineVar(name, value, PipelineVariableType.Text)
  def file(name: String, fileContent: String) = new PipelineVar(name, fileContent, PipelineVariableType.File)

  implicit val PipelineVarCodec: Codec[PipelineVar] = deriveCodec[PipelineVar]
}

case class TriggerPipelineRequest(
    ref: String,
    variables: Vector[PipelineVar],
)

object TriggerPipelineRequest {
  implicit val PipelineVarEncoder: Encoder[TriggerPipelineRequest] = deriveEncoder[TriggerPipelineRequest]
}
