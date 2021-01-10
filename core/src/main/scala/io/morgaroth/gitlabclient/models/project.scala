package io.morgaroth.gitlabclient.models

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import java.time.ZonedDateTime

sealed trait NamespaceKind {
  def repr: String
}

class NamespaceKindBase(val repr: String) extends NamespaceKind

object NamespaceKind {
  val Group = new NamespaceKindBase("group")
  val User = new NamespaceKindBase("user")
  val allValues: Seq[NamespaceKind] = Seq(Group, User)
  val all: Map[String, NamespaceKind] = allValues.map(x => x.repr -> x).toMap
  val byName: String => Option[NamespaceKind] = all.get
}

case class GitlabNamespace(
                            id: BigInt,
                            name: String,
                            path: String,
                            kind: String,
                            full_path: String,
                            parent_id: Option[Long],
                            avatar_url: Option[String],
                            web_url: Option[String],
                          )

object GitlabNamespace {
  implicit val GitlabNamespaceDecoder: Decoder[GitlabNamespace] = deriveDecoder[GitlabNamespace]
}

case class ProjectLinks(
                         self: String,
                         issues: Option[String],
                         merge_requests: Option[String],
                         repo_branches: String,
                         labels: String,
                         events: String,
                         members: String,
                       )

object ProjectLinks {
  implicit val ProjectLinksDecoder: Decoder[ProjectLinks] = deriveDecoder[ProjectLinks]
}

case class ProjectInfo(
                        id: BigInt,
                        description: Option[String],
                        name: String,
                        name_with_namespace: String,
                        path: String,
                        path_with_namespace: String,
                        created_at: ZonedDateTime,
                        default_branch: Option[String],
                        tag_list: Vector[String],
                        ssh_url_to_repo: String,
                        http_url_to_repo: String,
                        web_url: String,
                        readme_url: Option[String],
                        avatar_url: Option[String],
                        star_count: Int,
                        forks_count: Int,
                        last_activity_at: ZonedDateTime,
                        namespace: GitlabNamespace,
                        _links: ProjectLinks,
                        empty_repo: Option[Boolean],
                        archived: Boolean,
                        visibility: String,
                        owner: Option[GitlabUser],
                        container_registry_enabled: Option[Boolean],
                        issues_enabled: Boolean,
                        merge_requests_enabled: Boolean,
                        wiki_enabled: Boolean,
                        jobs_enabled: Boolean,
                        snippets_enabled: Boolean,
                        issues_access_level: Option[String],
                        repository_access_level: Option[String],
                        merge_requests_access_level: Option[String],
                        wiki_access_level: Option[String],
                        builds_access_level: Option[String],
                        snippets_access_level: Option[String],
                        shared_runners_enabled: Boolean,
                        lfs_enabled: Boolean,
                        creator_id: BigInt,
                        merge_method: String,
                      )

object ProjectInfo {
  implicit val ProjectInfoDecoder: Decoder[ProjectInfo] = deriveDecoder[ProjectInfo]
}