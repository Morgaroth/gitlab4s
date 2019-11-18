package io.morgaroth.gitlabclient.models

import java.time.ZonedDateTime
import java.util.UUID

import cats.syntax.option._

case class GitlabUser(
                       id: Long,
                       name: String,
                       username: String,
                       state: String,
                       avatar_url: String,
                       web_url: String,
                     )

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

case class PaginatedResponse[A](
                                 size: Option[Int],
                                 page: Int,
                                 pagelen: Int,
                                 next: Option[String],
                                 previous: Option[String],
                                 values: Vector[A],
                               )
