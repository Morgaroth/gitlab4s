package io.morgaroth.gitlabclient.models

import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Codec, Decoder}
import io.morgaroth.gitlabclient.marshalling.{EnumMarshalling, EnumMarshallingGlue}

import java.time.ZonedDateTime


sealed abstract class PipelineStatus(val name: String) extends Product with Serializable

object PipelineStatus extends EnumMarshallingGlue[PipelineStatus] {

  case object Success extends PipelineStatus("success")

  case object Skipped extends PipelineStatus("skipped")

  case object Failed extends PipelineStatus("failed")

  case object Canceled extends PipelineStatus("canceled")

  case object Running extends PipelineStatus("running")

  case object Scheduled extends PipelineStatus("scheduled")

  case object Manual extends PipelineStatus("manual")

  val all: Seq[PipelineStatus] = Seq(Success, Skipped, Failed, Canceled, Running, Scheduled, Manual)
  val byName: Map[String, PipelineStatus] = all.map(x => x.name -> x).toMap

  override def rawValue: PipelineStatus => String = _.name

  implicit val PipelineStatusCirceCodec: Codec[PipelineStatus] = EnumMarshalling.stringEnumCodecOf(PipelineStatus)
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
                          sha: String,
                          ref: String,
                          status: PipelineStatus,
                          created_at: ZonedDateTime,
                          updated_at: ZonedDateTime,
                          web_url: String,
                        )

object PipelineShort {
  implicit val PipelineShort: Decoder[PipelineShort] = deriveDecoder[PipelineShort]
}

case class PipelineFullInfo(
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

object PipelineFullInfo {
  implicit val PipelineFullInfoDecoder: Decoder[PipelineFullInfo] = deriveDecoder[PipelineFullInfo]
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
  implicit val PipelineStatusInfoDecoder: Decoder[PipelineStatusInfo] = deriveDecoder[PipelineStatusInfo]
}