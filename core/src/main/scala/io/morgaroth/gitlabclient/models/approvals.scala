package io.morgaroth.gitlabclient.models

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Codec, Decoder, Encoder}
import io.morgaroth.gitlabclient.marshalling.{EnumMarshalling, EnumMarshallingGlue}

import java.time.ZonedDateTime


case class ApprovedBy(
                       user: GitlabUser,
                       // group: GitlabGroup, but I couldn't find an example...
                     )

object ApprovedBy {
  implicit val ApprovedByDecoder: Decoder[ApprovedBy] = deriveDecoder[ApprovedBy]
}

sealed abstract class RuleType(val name: String) extends Product with Serializable

object RuleType extends EnumMarshallingGlue[RuleType] {

  final case object Regular extends RuleType("regular")

  final case object AnyApprover extends RuleType("any_approver")

  val all: Seq[RuleType] = Seq(Regular, AnyApprover)
  val byName: Map[String, RuleType] = all.map(x => x.name -> x).toMap

  override def rawValue: RuleType => String = _.name

  implicit val RuleTypeCirceCodec: Codec[RuleType] = EnumMarshalling.stringEnumCodecOf(RuleType)
}

case class ApprovalRule(
                         id: BigInt,
                         name: String,
                         rule_type: RuleType,
                       )

object ApprovalRule {
  implicit val ApprovalRuleDecoder: Decoder[ApprovalRule] = deriveDecoder[ApprovalRule]
}

case class MergeRequestApprovals(
                                  id: BigInt,
                                  iid: BigInt,
                                  project_id: BigInt,
                                  title: String,
                                  description: String,
                                  state: MergeRequestState,
                                  created_at: ZonedDateTime,
                                  updated_at: ZonedDateTime,
                                  merge_status: MergeStatus,
                                  approvals_required: Int,
                                  approvals_left: Int,
                                  approved_by: Vector[ApprovedBy],
                                  suggested_approvers: Vector[GitlabUser],
                                  user_has_approved: Boolean,
                                  user_can_approve: Boolean,
                                  approval_rules_left: Vector[ApprovalRule],
                                  has_approval_rules: Boolean,
                                  merge_request_approvers_available: Boolean,
                                  multiple_approval_rules_available: Boolean,
                                  require_password_to_approve: Option[Boolean],
                                )

object MergeRequestApprovals {
  implicit val MergeRequestApprovalsDecoder: Decoder[MergeRequestApprovals] = deriveDecoder[MergeRequestApprovals]
}

case class SourceApprovalRuleInfo(
                                   approvals_required: Int,
                                 )

object SourceApprovalRuleInfo {
  implicit val SourceApprovalRuleInfoDecoder: Decoder[SourceApprovalRuleInfo] = deriveDecoder[SourceApprovalRuleInfo]
}

case class MergeRequestApprovalRule(
                                     id: BigInt,
                                     name: String,
                                     rule_type: RuleType,
                                     eligible_approvers: Vector[GitlabUser],
                                     approvals_required: Int,
                                     source_rule: Option[SourceApprovalRuleInfo],
                                     users: Vector[GitlabUser],
                                     groups: Vector[GitlabGroup],
                                     contains_hidden_groups: Boolean,
                                   )

object MergeRequestApprovalRule {
  implicit val MergeRequestApprovalRuleDecoder: Decoder[MergeRequestApprovalRule] = deriveDecoder[MergeRequestApprovalRule]
}

case class MergeRequestApprovalRules(
                                      approval_rules_overwritten: Boolean,
                                      rules: Vector[MergeRequestApprovalRule],
                                    )

case class CreateMergeRequestApprovalRule(
                                           name: String,
                                           approvals_required: Int,
                                           approval_project_rule_id: Option[BigInt],
                                           user_ids: Option[Vector[BigInt]],
                                           group_ids: Option[Vector[BigInt]],
                                         )

object CreateMergeRequestApprovalRule {
  implicit val CreateMergeRequestApprovalRuleEncoder: Encoder[CreateMergeRequestApprovalRule] = deriveEncoder[CreateMergeRequestApprovalRule]

  def oneOf(name: String, userId: BigInt*): CreateMergeRequestApprovalRule = new CreateMergeRequestApprovalRule(name, 1, None, Some(userId.toVector), None)
}