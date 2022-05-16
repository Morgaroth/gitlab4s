package io.gitlab.mateuszjaje.gitlabclient
package models

import maintenance.MissingPropertiesLogger
import marshalling.{EnumMarshalling, EnumMarshallingGlue}

import io.circe.generic.semiauto.{deriveCodec, deriveEncoder}
import io.circe.{Codec, Encoder}

import java.time.ZonedDateTime

sealed trait NamespaceKind {
  def repr: String
}

class NamespaceKindBase(val repr: String) extends NamespaceKind

object NamespaceKind {
  val Group                                   = new NamespaceKindBase("group")
  val User                                    = new NamespaceKindBase("user")
  val allValues: Seq[NamespaceKind]           = Seq(Group, User)
  val all: Map[String, NamespaceKind]         = allValues.map(x => x.repr -> x).toMap
  val byName: String => Option[NamespaceKind] = all.get
}

sealed abstract class MergeStrategy(val name: String) extends Product with Serializable

object MergeStrategy extends EnumMarshallingGlue[MergeStrategy] {

  final case object FastForward extends MergeStrategy("ff")

  final case object RebaseMerge extends MergeStrategy("rebase_merge")

  final case object MergeCommit extends MergeStrategy("merge")

  val all: Seq[MergeStrategy]            = Seq(MergeCommit, RebaseMerge, FastForward)
  val byName: Map[String, MergeStrategy] = all.map(x => x.name -> x).toMap
  val rawValue: MergeStrategy => String  = _.name

  implicit val MergeRequestStateCirceCodec: Codec[MergeStrategy] = EnumMarshalling.stringEnumCodecOf(MergeStrategy)

}

sealed abstract class SquashOption(val name: String) extends Product with Serializable

object SquashOption extends EnumMarshallingGlue[SquashOption] {

  final case object NotAllow extends SquashOption("never")

  final case object Allow extends SquashOption("default_off")

  final case object Encourage extends SquashOption("default_on")

  final case object Require extends SquashOption("always")

  val all: Seq[SquashOption]            = Seq(Require, Encourage, Allow, NotAllow)
  val byName: Map[String, SquashOption] = all.map(x => x.name -> x).toMap
  val rawValue: SquashOption => String  = _.name

  implicit val SquashOptionCirceCodec: Codec[SquashOption] = EnumMarshalling.stringEnumCodecOf(SquashOption)

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
  implicit val GitlabNamespaceCodec: Codec[GitlabNamespace] = deriveCodec[GitlabNamespace]
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
  implicit val ProjectLinksCodec: Codec[ProjectLinks] = deriveCodec[ProjectLinks]
}

case class ContainerExpirationPolicy(
    cadence: String,
    enabled: Boolean,
    keep_n: Option[Int],
    older_than: Option[String],
    name_regex: Option[String],
    name_regex_keep: Option[String],
    next_run_at: ZonedDateTime,
)

object ContainerExpirationPolicy {
  implicit val ContainerExpirationPolicyCodec: Codec[ContainerExpirationPolicy] =
    MissingPropertiesLogger.loggingCodec(deriveCodec[ContainerExpirationPolicy])

}

case class PermissionEntry(
    access_level: Int,
    notification_level: Int,
)

object PermissionEntry {
  implicit val PermissionEntryCodec: Codec[PermissionEntry] = deriveCodec[PermissionEntry]
}

case class ProjectPermissions(
    project_access: Option[PermissionEntry],
    group_access: Option[PermissionEntry],
)

object ProjectPermissions {
  implicit val ProjectPermissionsCodec: Codec[ProjectPermissions] = MissingPropertiesLogger.loggingCodec(deriveCodec[ProjectPermissions])
}

case class SharedGroup(
    group_id: BigInt,
    group_name: String,
    group_full_path: String,
    group_access_level: Int,
    expires_at: Option[ZonedDateTime],
)

object SharedGroup {
  implicit val SharedGroupCodec: Codec[SharedGroup] = MissingPropertiesLogger.loggingCodec(deriveCodec[SharedGroup])

}

sealed abstract class ContainerRegistryAccessLevel(val name: String) extends Product with Serializable

object ContainerRegistryAccessLevel extends EnumMarshallingGlue[ContainerRegistryAccessLevel] {

  final case object Enabled extends ContainerRegistryAccessLevel("enabled")
  final case object Private extends ContainerRegistryAccessLevel("private")

  final case object Disabled extends ContainerRegistryAccessLevel("disabled")

  val all: Seq[ContainerRegistryAccessLevel]            = Seq(Enabled, Private, Disabled)
  val byName: Map[String, ContainerRegistryAccessLevel] = all.map(x => x.name -> x).toMap
  val rawValue: ContainerRegistryAccessLevel => String  = _.name

  implicit val ContainerRegistryAccessLevelCirceCodec: Codec[ContainerRegistryAccessLevel] =
    EnumMarshalling.stringEnumCodecOf(ContainerRegistryAccessLevel)

}

case class ProjectInfo(
    id: BigInt,
    description: Option[String],
    topics: Vector[String],
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
    issues_access_level: Option[String],
    repository_access_level: Option[String],
    merge_requests_access_level: Option[String],
    wiki_access_level: Option[String],
    builds_access_level: Option[String],
    snippets_access_level: Option[String],
    shared_runners_enabled: Boolean,
    lfs_enabled: Boolean,
    creator_id: BigInt,
    merge_method: MergeStrategy,
    squash_option: SquashOption,
    packages_enabled: Option[Boolean],
    service_desk_enabled: Boolean,
    service_desk_address: Option[String],
    can_create_merge_request_in: Boolean,
    forking_access_level: String,
    pages_access_level: String,
    emails_disabled: Option[Boolean],
    import_status: String,
    import_error: Option[String],
    open_issues_count: Option[Int], // if issues are enabled
    issues_template: Option[String],
    runners_token: Option[String],
    ci_default_git_depth: Option[Int],
    public_jobs: Boolean,
    build_git_strategy: Option[String],
    build_timeout: Int,
    auto_cancel_pending_pipelines: String,
    build_coverage_regex: Option[String],
    ci_config_path: Option[String],
    shared_with_groups: Vector[SharedGroup],
    suggestion_commit_message: Option[String],
    auto_devops_deploy_strategy: String,
    mirror: Boolean,
    external_authorization_classification_label: Option[String],
    marked_for_deletion_at: Option[ZonedDateTime],
    marked_for_deletion_on: Option[ZonedDateTime],
    compliance_frameworks: Vector[String],
    runner_token_expiration_interval: Option[String],
    // flags
    request_access_enabled: Boolean,
    container_registry_enabled: Option[Boolean],
    security_and_compliance_enabled: Boolean,
    requirements_enabled: Boolean,
    restrict_user_defined_variables: Boolean,
    merge_requests_enabled: Boolean,
    issues_enabled: Boolean,
    wiki_enabled: Boolean,
    jobs_enabled: Boolean,
    snippets_enabled: Boolean,
    auto_devops_enabled: Boolean,
    ci_forward_deployment_enabled: Boolean,
    ci_separated_caches: Option[Boolean],
    merge_pipelines_enabled: Boolean,
    merge_trains_enabled: Boolean,
    security_and_compliance_access_level: Option[String],
    // merge requests
    only_allow_merge_if_all_discussions_are_resolved: Boolean,
    remove_source_branch_after_merge: Option[Boolean],
    autoclose_referenced_issues: Boolean,
    approvals_before_merge: Int,
    merge_requests_template: Option[String],
    only_allow_merge_if_pipeline_succeeds: Boolean,
    allow_merge_on_skipped_pipeline: Option[Boolean],
    resolve_outdated_diff_discussions: Boolean,
    printing_merge_request_link_enabled: Boolean,
    operations_access_level: Option[String],
    analytics_access_level: Option[String],
    requirements_access_level: Option[String],
    container_expiration_policy: ContainerExpirationPolicy,
    permissions: Option[ProjectPermissions],
    ci_job_token_scope_enabled: Boolean,
    keep_latest_artifact: Boolean,
    // container registry
    container_registry_image_prefix: String,
    container_registry_access_level: ContainerRegistryAccessLevel,
    squash_commit_template: Option[String],
    merge_commit_template: Option[String],
    // import
    import_type: Option[String],
    import_url: Option[String],
    // other
    enforce_auth_checks_on_uploads: Option[Boolean],
)

object ProjectInfo {
  implicit val ProjectInfoCodec: Codec[ProjectInfo] = MissingPropertiesLogger.loggingCodec(deriveCodec[ProjectInfo])
}

object EditProjectRequest {
  implicit val EditProjectReqEncoder: Encoder[EditProjectRequest] = deriveEncoder[EditProjectRequest]

  val Builder = new EditProjectRequest()
}

case class EditProjectRequest private (
    allow_merge_on_skipped_pipeline: Option[Boolean] = None,
    analytics_access_level: Option[String] = None,
    approvals_before_merge: Option[Int] = None,
    auto_cancel_pending_pipelines: Option[String] = None,
    auto_devops_deploy_strategy: Option[String] = None,
    auto_devops_enabled: Option[Boolean] = None,
    autoclose_referenced_issues: Option[Boolean] = None,
    // avatar: Option[mixed],  // problematic
    build_coverage_regex: Option[String] = None,
    build_git_strategy: Option[String] = None,
    build_timeout: Option[Int] = None,
    builds_access_level: Option[String] = None,
    ci_config_path: Option[String] = None,
    ci_default_git_depth: Option[Int] = None,
    ci_forward_deployment_enabled: Option[Boolean] = None,
    // container_expiration_policy_attributes: Option[hash], // problematic
    container_registry_enabled: Option[Boolean] = None,
    default_branch: Option[String] = None,
    description: Option[String] = None,
    emails_disabled: Option[Boolean] = None,
    external_authorization_classification_label: Option[String] = None,
    forking_access_level: Option[String] = None,
    import_url: Option[String] = None,
    issues_access_level: Option[String] = None,
    issues_enabled: Option[Boolean] = None,
    jobs_enabled: Option[Boolean] = None,
    lfs_enabled: Option[Boolean] = None,
    merge_commit_template: Option[String] = None,
    merge_method: Option[MergeStrategy] = None,
    merge_requests_access_level: Option[String] = None,
    merge_requests_enabled: Option[Boolean] = None,
    mirror_overwrites_diverged_branches: Option[Boolean] = None,
    mirror_trigger_builds: Option[Boolean] = None,
    mirror_user_id: Option[Int] = None,
    mirror: Option[Boolean] = None,
    name: Option[String] = None,
    operations_access_level: Option[String] = None,
    only_allow_merge_if_all_discussions_are_resolved: Option[Boolean] = None,
    only_allow_merge_if_pipeline_succeeds: Option[Boolean] = None,
    only_mirror_protected_branches: Option[Boolean] = None,
    packages_enabled: Option[Boolean] = None,
    pages_access_level: Option[String] = None,
    requirements_access_level: Option[String] = None,
    path: Option[String] = None,
    public_builds: Option[Boolean] = None,
    remove_source_branch_after_merge: Option[Boolean] = None,
    repository_access_level: Option[String] = None,
    repository_storage: Option[String] = None,
    request_access_enabled: Option[Boolean] = None,
    resolve_outdated_diff_discussions: Option[Boolean] = None,
    service_desk_enabled: Option[Boolean] = None,
    shared_runners_enabled: Option[Boolean] = None,
    show_default_award_emojis: Option[Boolean] = None,
    snippets_access_level: Option[String] = None,
    snippets_enabled: Option[Boolean] = None,
    squash_option: Option[SquashOption] = None,
    suggestion_commit_message: Option[String] = None,
    tag_list: Option[Set[String]] = None,
    visibility: Option[String] = None,
    wiki_access_level: Option[String] = None,
    wiki_enabled: Option[Boolean] = None,
) {
  def withAllowMergeOnSkippedPipeline(value: Boolean) = copy(allow_merge_on_skipped_pipeline = Some(value))

  def withAnalyticsAccessLevel(value: String) = copy(analytics_access_level = Some(value))

  def withApprovalsBeforeMerge(value: Int) = copy(approvals_before_merge = Some(value))

  def withAutoCancelPendingPipelines(value: String) = copy(auto_cancel_pending_pipelines = Some(value))

  def withAutoDevopsDeployStrategy(value: String) = copy(auto_devops_deploy_strategy = Some(value))

  def withAutoDevopsEnabled(value: Boolean) = copy(auto_devops_enabled = Some(value))

  def withAutocloseReferencedIssues(value: Boolean) = copy(autoclose_referenced_issues = Some(value))

  def withBuildCoverageRegex(value: String) = copy(build_coverage_regex = Some(value))

  def withBuildGitStrategy(value: String) = copy(build_git_strategy = Some(value))

  def withBuildTimeout(value: Int) = copy(build_timeout = Some(value))

  def withBuildsAccessLevel(value: String) = copy(builds_access_level = Some(value))

  def withCiConfigPath(value: String) = copy(ci_config_path = Some(value))

  def withCiDefaultGitDepth(value: Int) = copy(ci_default_git_depth = Some(value))

  def withCiForwardDeploymentEnabled(value: Boolean) = copy(ci_forward_deployment_enabled = Some(value))

  def withContainerRegistryEnabled(value: Boolean) = copy(container_registry_enabled = Some(value))

  def withDefaultBranch(value: String) = copy(default_branch = Some(value))

  def withDescription(value: String) = copy(description = Some(value))

  def withEmailsDisabled(value: Boolean) = copy(emails_disabled = Some(value))

  def withExternalAuthorizationClassificationLabel(value: String) = copy(external_authorization_classification_label = Some(value))

  def withForkingAccessLevel(value: String) = copy(forking_access_level = Some(value))

  def withImportUrl(value: String) = copy(import_url = Some(value))

  def withIssuesAccessLevel(value: String) = copy(issues_access_level = Some(value))

  def withIssuesEnabled(value: Boolean) = copy(issues_enabled = Some(value))

  def withJobsEnabled(value: Boolean) = copy(jobs_enabled = Some(value))

  def withLfsEnabled(value: Boolean) = copy(lfs_enabled = Some(value))

  def withMergeMethod(value: MergeStrategy) = copy(merge_method = Some(value))

  def withMergeRequestsAccessLevel(value: String) = copy(merge_requests_access_level = Some(value))

  def withMergeRequestsEnabled(value: Boolean) = copy(merge_requests_enabled = Some(value))

  def withMirrorOverwritesDivergedBranches(value: Boolean) = copy(mirror_overwrites_diverged_branches = Some(value))

  def withMirrorTriggerBuilds(value: Boolean) = copy(mirror_trigger_builds = Some(value))

  def withMirrorUserId(value: Int) = copy(mirror_user_id = Some(value))

  def withMirror(value: Boolean) = copy(mirror = Some(value))

  def withName(value: String) = copy(name = Some(value))

  def withOperationsAccessLevel(value: String) = copy(operations_access_level = Some(value))

  def withOnlyAllowMergeIfAllDiscussionsAreResolved(value: Boolean) = copy(only_allow_merge_if_all_discussions_are_resolved = Some(value))

  def withOnlyAllowMergeIfPipelineSucceeds(value: Boolean) = copy(only_allow_merge_if_pipeline_succeeds = Some(value))

  def withOnlyMirrorProtectedBranches(value: Boolean) = copy(only_mirror_protected_branches = Some(value))

  def withPackagesEnabled(value: Boolean) = copy(packages_enabled = Some(value))

  def withPagesAccessLevel(value: String) = copy(pages_access_level = Some(value))

  def withRequirementsAccessLevel(value: String) = copy(requirements_access_level = Some(value))

  def withPath(value: String) = copy(path = Some(value))

  def withPublicBuilds(value: Boolean) = copy(public_builds = Some(value))

  def withRemoveSourceBranchAfterMerge(value: Boolean) = copy(remove_source_branch_after_merge = Some(value))

  def withRepositoryAccessLevel(value: String) = copy(repository_access_level = Some(value))

  def withRepositoryStorage(value: String) = copy(repository_storage = Some(value))

  def withRequestAccessEnabled(value: Boolean) = copy(request_access_enabled = Some(value))

  def withResolveOutdatedDiffDiscussions(value: Boolean) = copy(resolve_outdated_diff_discussions = Some(value))

  def withServiceDeskEnabled(value: Boolean) = copy(service_desk_enabled = Some(value))

  def withSharedRunnersEnabled(value: Boolean) = copy(shared_runners_enabled = Some(value))

  def withShowDefaultAwardEmojis(value: Boolean) = copy(show_default_award_emojis = Some(value))

  def withSnippetsAccessLevel(value: String) = copy(snippets_access_level = Some(value))

  def withSnippetsEnabled(value: Boolean) = copy(snippets_enabled = Some(value))

  def withSquashOption(value: SquashOption) = copy(squash_option = Some(value))

  def withSuggestionCommitMessage(value: String) = copy(suggestion_commit_message = Some(value))

  def withTagList(value: Set[String]) = copy(tag_list = Some(value))

  def withVisibility(value: String) = copy(visibility = Some(value))

  def withWikiAccessLevel(value: String) = copy(wiki_access_level = Some(value))

  def withWikiEnabled(value: Boolean) = copy(wiki_enabled = Some(value))

  override def toString: String = {
    import io.circe.syntax._
    this.asJson.asObject.get
      .filter(!_._2.isNull)
      .toMap
      .map { case (k, v) => k -> v.asString.getOrElse(v.toString()) }
      .mkString("ProjectUpdates(", ", ", ")")
  }

}
