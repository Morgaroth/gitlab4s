package io.gitlab.mateuszjaje.gitlabclient
package models

import maintenance.MissingPropertiesLogger

import io.circe.generic.semiauto.{deriveCodec, deriveEncoder}
import io.circe.{Codec, Encoder}

import java.time.ZonedDateTime

case class PushRules(
    id: BigInt,
    project_id: BigInt,
    commit_message_regex: Option[String],
    commit_message_negative_regex: Option[String],
    branch_name_regex: Option[String],
    deny_delete_tag: Option[Boolean],
    created_at: ZonedDateTime,
    member_check: Boolean,
    prevent_secrets: Boolean,
    author_email_regex: Option[String],
    file_name_regex: Option[String],
    max_file_size: Int,
    commit_committer_check: Option[Boolean], // premium gitlab users
    reject_unsigned_commits: Option[Boolean],// premium gitlab users
)

object PushRules {
  implicit val PushRulesCodec: Codec[PushRules] = MissingPropertiesLogger.loggingCodec(deriveCodec[PushRules])
}

case class EditPushRuleRequest (
    author_email_regex: Option[String] = None,
    branch_name_regex: Option[String] = None,
    commit_committer_check: Option[Boolean] = None,
    commit_message_negative_regex: Option[String] = None,
    commit_message_regex: Option[String] = None,
    deny_delete_tag: Option[Boolean] = None,
    file_name_regex: Option[String] = None,
    max_file_size: Option[Int] = None,
    member_check: Option[Boolean] = None,
    prevent_secrets: Option[Boolean] = None,
    reject_unsigned_commits: Option[Boolean] = None,
) {
  def withAuthorEmailRegex(value: String) = copy(author_email_regex = Some(value))

  def withBranchNameRegex(value: String) = copy(branch_name_regex = Some(value))

  def withCommitCommitterCheck(value: Boolean) = copy(commit_committer_check = Some(value))

  def withCommitMessageNegativeRegex(value: String) = copy(commit_message_negative_regex = Some(value))

  def withCommitMessageRegex(value: String) = copy(commit_message_regex = Some(value))

  def withDenyDeleteTag(value: Boolean) = copy(deny_delete_tag = Some(value))

  def withFileNameRegex(value: String) = copy(file_name_regex = Some(value))

  def withMaxFileSize(value: Int) = copy(max_file_size = Some(value))

  def withMemberCheck(value: Boolean) = copy(member_check = Some(value))

  def withPreventSecrets(value: Boolean) = copy(prevent_secrets = Some(value))

  def withRejectUnsignedCommits(value: Boolean) = copy(reject_unsigned_commits = Some(value))

  override def toString: String = {
    import io.circe.syntax._
    this.asJson.asObject.get
      .filter(!_._2.isNull)
      .toMap
      .map { case (k, v) => k -> v.asString.getOrElse(v.toString()) }
      .mkString("PushRulesUpdates(", ", ", ")")
  }

}

object EditPushRuleRequest {
  implicit val PushRulesEncoder: Encoder[EditPushRuleRequest] = deriveEncoder[EditPushRuleRequest]

  val Builder = new EditPushRuleRequest()
}
