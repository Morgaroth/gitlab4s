package io.morgaroth.gitlabclient.models

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class ReleaseShort(
                         tag_name: String,
                         description: String,
                       )

object ReleaseShort {
  implicit val ReleaseShortDecoder: Decoder[ReleaseShort] = deriveDecoder[ReleaseShort]
}

case class TagInfo(
                    commit: CommitSimple,
                    release: Option[ReleaseShort],
                    name: String,
                    target: String,
                    message: Option[String],
                    `protected`: Boolean,
                  )

object TagInfo {
  implicit val TagInfoDecoder: Decoder[TagInfo] = deriveDecoder[TagInfo]
}