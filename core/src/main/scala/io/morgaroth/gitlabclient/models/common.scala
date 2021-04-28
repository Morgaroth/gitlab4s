package io.morgaroth.gitlabclient.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import io.morgaroth.gitlabclient.maintenance.MissingPropertiesLogger

import java.time.ZonedDateTime

case class GitlabUser(
    id: Long,
    name: String,
    username: String,
    state: String,
    avatar_url: String,
    web_url: String,
)

object GitlabUser {
  implicit val GitlabUserCodec: Codec[GitlabUser] = deriveCodec[GitlabUser]
}

case class GitlabFullUser(
    id: BigInt,
    name: String,
    username: String,
    state: String,
    avatar_url: String,
    web_url: String,
    website_url: String,
    created_at: ZonedDateTime,
    bio: String,
    bio_html: String,
    location: String,
    email: String,
    public_email: Option[String],
    skype: String,
    twitter: String,
    linkedin: String,
    job_title: String,
    organization: String,
)

object GitlabFullUser {
  implicit val GitlabFullUserCodec: Codec[GitlabFullUser] = MissingPropertiesLogger.loggingCodec(deriveCodec[GitlabFullUser])
}

case class GitlabGroup(
    id: BigInt,
    parent_id: BigInt,
    web_url: String,
    name: String,
    path: String,
    description: String,
    visibility: String,
    avatar_url: Option[String],
    request_access_enabled: Boolean,
    lfs_enabled: Boolean,
    emails_disabled: Option[Boolean],
    auto_devops_enabled: Option[Boolean],
    mentions_disabled: Option[Boolean],
    full_name: String,
    full_path: String,
    share_with_group_lock: Boolean,
    two_factor_grace_period: Long,
    require_two_factor_authentication: Boolean,
    project_creation_level: String,
    subgroup_creation_level: String,
    default_branch_protection: Int,
    marked_for_deletion_on: Option[ZonedDateTime],
    created_at: ZonedDateTime,
    ldap_access: Option[Int],
    ldap_cn: Option[Int],
)

object GitlabGroup {
  implicit val GitlabGroupCodec: Codec[GitlabGroup] = MissingPropertiesLogger.loggingCodec(deriveCodec[GitlabGroup])
}

case class PaginatedResponse[A](
    size: Option[Int],
    page: Int,
    pagelen: Int,
    next: Option[String],
    previous: Option[String],
    values: Vector[A],
)
