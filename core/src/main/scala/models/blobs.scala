package io.gitlab.mateuszjaje.gitlabclient
package models

import maintenance.MissingPropertiesLogger

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class BlobInfo(
    project_id: BigInt,
    startline: Long,
    path: String,
    ref: String,
    basename: String,
    data: String,
    filename: String,
    id: Option[BigInt], // no idea what it is
)

object BlobInfo {
  implicit val BlobInfoCodec: Codec[BlobInfo] = MissingPropertiesLogger.loggingCodec(deriveCodec[BlobInfo])
}
