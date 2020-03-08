package io.morgaroth.gitlabclient.models

import java.time.ZonedDateTime

import io.circe.Codec
import io.morgaroth.gitlabclient.marshalling.{EnumMarshalling, EnumMarshallingGlue}


case class ApprovedBy(
                       user: GitlabUser,
                       // group: GitlabGroup, but I couldn't find an example...
                     )

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