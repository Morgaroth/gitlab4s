package io.gitlab.mateuszjaje.gitlabclient
package models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class ReleaseShort(
    tag_name: String,
    description: String,
)

object ReleaseShort {
  implicit val ReleaseShortCodec: Codec[ReleaseShort] = deriveCodec[ReleaseShort]
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
  implicit val TagInfoCodec: Codec[TagInfo] = deriveCodec[TagInfo]
}
