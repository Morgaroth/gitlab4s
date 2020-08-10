package io.morgaroth.gitlabclient.models

import java.time.ZonedDateTime

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class GitlabUser(
                       id: Long,
                       name: String,
                       username: String,
                       state: String,
                       avatar_url: String,
                       web_url: String,
                     )
object GitlabUser {
  implicit val GitlabUserDecoder = deriveDecoder[GitlabUser]
}

case class GitlabFullUser(
                           id: BigInt,
                           name: String,
                           username: String,
                           state: String,
                           avatar_url: String,
                           web_url: String,
                           created_at: ZonedDateTime,
                           bio: String,
                           location: String,
                           public_email: Option[String],
                           email: String,
                         )

object GitlabFullUser {
  implicit val GitlabFullUserDecoder: Decoder[GitlabFullUser] = deriveDecoder[GitlabFullUser]
}

case class GitlabGroup(
                        id: BigInt,
                        web_url: String,
                        name: String,
                        path: String,
                        description: String,
                        visibility: String,
                        avatar_url: String,
                        request_access_enabled: Boolean,
                        full_name: String,
                        full_path: String,
                      )
object GitlabGroup {
  implicit val GitlabGroupDecoder: Decoder[GitlabGroup] = deriveDecoder[GitlabGroup]
}

case class PaginatedResponse[A](
                                 size: Option[Int],
                                 page: Int,
                                 pagelen: Int,
                                 next: Option[String],
                                 previous: Option[String],
                                 values: Vector[A],
                               )
