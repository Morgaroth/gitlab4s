package io.morgaroth.gitlabclient.models

import java.time.ZonedDateTime

import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Codec, Decoder}
import io.morgaroth.gitlabclient.marshalling.{EnumMarshalling, EnumMarshallingGlue}

sealed abstract class DeploymentStatus(val name: String) extends Product with Serializable

object DeploymentStatus extends EnumMarshallingGlue[DeploymentStatus] {

  case object Created extends DeploymentStatus("created")

  case object Running extends DeploymentStatus("running")

  case object success extends DeploymentStatus("success")

  case object failed extends DeploymentStatus("failed")

  case object canceled extends DeploymentStatus("canceled")

  val all: Seq[DeploymentStatus] = Seq(Created, Running, success, failed, canceled)
  val byName: Map[String, DeploymentStatus] = all.map(x => x.name -> x).toMap

  override def rawValue: DeploymentStatus => String = _.name

  implicit val DeploymentStatusCirceCodec: Codec[DeploymentStatus] = EnumMarshalling.stringEnumCodecOf(DeploymentStatus)

}

case class DeploymentInfo(
                           created_at: ZonedDateTime,
                           updated_at: ZonedDateTime,
                           status: DeploymentStatus,
                           id: BigInt,
                           iid: BigInt,
                           ref: String,
                           sha: String,
                           user: GitlabUser,
                           // if someone delete pipeline, deployment still exists, but deployable doesn't
                           deployable: Option[DeploymentDeployable],
                         )
object DeploymentInfo {
  implicit val DeploymentInfoDecoder: Decoder[DeploymentInfo] = deriveDecoder[DeploymentInfo]
}

case class EnvironmentInfo(
                            id: BigInt,
                            name: String,
                            slug: String,
                            external_url: Option[String],
                          )

case class DeploymentDeployable(
                                 id: BigInt,
                                 status: String,
                                 stage: String, // of pipeline
                                 name: String, // of job step
                                 tag: Boolean,
                                 created_at: ZonedDateTime,
                                 started_at: Option[ZonedDateTime],
                                 finished_at: Option[ZonedDateTime],
                                 duration: Option[Double],
                                 user: GitlabUser,
                                 commit: CommitSimple,
                                 pipeline: PipelineShort,
                                 web_url: String,
                                 artifacts: List[PipelineArtifactSimple],
                                 artifacts_file: Option[ArtifactFile],
                                 artifacts_expire_at: Option[ZonedDateTime],
                               )
object DeploymentDeployable {
  implicit val DeploymentDeployableDecoder: Decoder[DeploymentDeployable] = deriveDecoder[DeploymentDeployable]
}

case class ArtifactFile(
                         filename: String,
                         size: Long,
                       )
object ArtifactFile {
  implicit val ArtifactFileDecoder: Decoder[ArtifactFile] = deriveDecoder[ArtifactFile]
}

case class PipelineArtifactSimple(
                                   file_type: String,
                                   size: Long,
                                   filename: String,
                                   file_format: Option[String],
                                 )
object PipelineArtifactSimple {
  implicit val PipelineArtifactSimpleDecoder: Decoder[PipelineArtifactSimple] = deriveDecoder[PipelineArtifactSimple]
}