package io.morgaroth.gitlabclient.models

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import java.time.ZonedDateTime

case class GitlabBranchInfo(
    name: String,
    merged: Boolean,
    `protected`: Boolean,
    default: Boolean,
    developers_can_push: Boolean,
    developers_can_merge: Boolean,
    can_push: Boolean,
    commit: GitlabCommitInfo,
)

object GitlabBranchInfo {
  implicit val GitlabBranchInfoDecoder: Decoder[GitlabBranchInfo] = deriveDecoder[GitlabBranchInfo]
}

case class GitlabCommitInfo(
    author_email: String,
    author_name: String,
    authored_date: ZonedDateTime,
    committed_date: ZonedDateTime,
    committer_email: String,
    committer_name: String,
    id: String,
    short_id: String,
    title: String,
    message: String,
    parent_ids: Option[Vector[String]],
)

object GitlabCommitInfo {
  implicit val GitlabCommitInfoDecoder: Decoder[GitlabCommitInfo] = deriveDecoder[GitlabCommitInfo]
}
