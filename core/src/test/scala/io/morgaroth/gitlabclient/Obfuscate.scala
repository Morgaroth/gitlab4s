package io.morgaroth.gitlabclient

import io.morgaroth.gitlabclient.marshalling.Gitlab4SMarshalling
import org.scalatest.DoNotDiscover
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import scala.util.Random

@DoNotDiscover
class Obfuscate extends AnyFlatSpec with Matchers with Gitlab4SMarshalling {

  behavior of "Obfuscate"

  val safeFields = Set(
    "state",
    "action_name",
    "ref_type",
    "action",
    "commit_count",
    "target_type",
    "type",
    "noteable_type",
    "position_type",
    "resolved",
    "system",
    "resolvable",
    "new_line",
    "old_line",
    "additions",
    "deletions",
    "total",
    "resolve_outdated_diff_discussions",
    "container_registry_enabled",
    "visibility",
    "archived",
    "merge_requests_enabled",
    "wiki_enabled",
    "jobs_enabled",
    "snippets_enabled",
    "service_desk_enabled",
    "can_create_merge_request_in",
    "issues_access_level",
    "repository_access_level",
    "merge_requests_access_level",
    "forking_access_level",
    "wiki_access_level",
    "builds_access_level",
    "pages_access_level",
    "shared_runners_enabled",
    "lfs_enabled",
    "auto_devops_deploy_strategy",
    "auto_devops_enabled",
    "printing_merge_request_link_enabled",
    "merge_method",
    "packages_enabled",
    "empty_repo",
    "security_and_compliance_enabled",
    "requirements_enabled",
    "mirror",
    "autoclose_referenced_issues",
    "remove_source_branch_after_merge",
    "restrict_user_defined_variables",
    "only_allow_merge_if_pipeline_succeeds",
    "analytics_access_level",
    "operations_access_level",
    "snippets_access_level",
    "request_access_enabled",
    "issues_enabled",
  ).map(x => s""""$x"""")

  val staticOverrides = Set(
    """"project_id": 111""",
    """"target_id": 111""",
    """"target_iid": 111""",
    """"author_id": 111""",
    """"creator_id": 111""",
    """"id": 111""",
    """"noteable_id": 111""",
    """"noteable_iid": 111""",
    """"created_at": "1970-01-01T12:00:00.000+00:00"""",
    """"updated_at": "1970-01-01T12:00:00.000+00:00"""",
    """"last_activity_at": "1970-01-01T12:00:00.000+00:00"""",
    """"commands_changes": {}""",
  )

  val textFields = Set(
    "title",
    "body",
    "diff",
    "a_mode",
    "b_mode",
    "slug",
    "stage",
    "description",
    "message",
    "full_name",
    "full_path",
    "path",
    "old_path",
    "new_path",
    "linkedin",
    "twitter",
    "skype",
    "bio",
    "job_title",
    "organization",
    "commit_title",
    "target_title",
    "name_with_namespace",
    "path_with_namespace",
    "build_coverage_regex",
    "ci_config_path",
    "auto_cancel_pending_pipelines",
    "build_git_strategy",
    "public_jobs",
    "runners_token",
    "import_status",
  )

  val branchFields = Set("ref", "source_branch", "target_branch", "reference", "default_branch")
  val numberFields = Set(
    "id",
    "project_id",
    "iid",
    "source_project_id",
    "target_project_id",
    "noteable_iid",
    "noteable_id",
    "target_id",
    "target_iid",
    "author_id",
    "forks_count",
    "star_count",
  )

  val dateFields         = Set("created_at", "updated_at", "authored_date", "committed_date", "created_at", "started_at", "finished_at")
  val shaFields          = Set("base_sha", "sha", "start_sha", "head_sha", "id", "short_id", "commit_from", "commit_to")
  val urlFields          = Set("avatar_url", "web_url", "readme_url", "")
  val emailFields        = Set("author_email", "committer_email", "public_email", "service_desk_address")
  val usernameFields     = Set("username", "author_username", "http_url_to_repo", "ssh_url_to_repo")
  val fullUserNameFields = Set("author_name", "committer_name", "name")
  val filenameFields     = Set("filename")
  val ipFields           = Set("ip_address")

  val rand = new Random(new java.util.Random())

  it should "work" in {
    val resourceName = "project_info_1.json"
    val result       = Source.fromResource(resourceName).mkString

    val result1 = textFields.foldLeft(result) { case (data, name) =>
      data.replaceAll(s""""$name": ".+"""", s""""$name": "${rand.alphanumeric.take(40).mkString}"""")
    }
    val result2 = numberFields.foldLeft(result1) { case (data, name) =>
      data.replaceAll(s""""$name": \\d+""", s""""$name": 111""")
    }
    val result3 = dateFields.foldLeft(result2) { case (data, name) =>
      data.replaceAll(s""""$name": ".+"""", s""""$name": "1970-01-01T12:00:00.000+00:00"""")
    }
    val result4 = shaFields.foldLeft(result3) { case (data, name) =>
      data.replaceAll(s""""$name": ".+"""", s""""$name": "$randomSha"""")
    }
    val result5 = urlFields.foldLeft(result4) { case (data, name) =>
      data.replaceAll(s""""$name": ".+"""", s""""$name": "https://google.com?q=${rand.alphanumeric.take(10).mkString}"""")
    }
    val result6 = emailFields.foldLeft(result5) { case (data, name) =>
      data.replaceAll(s""""$name": ".+"""", s""""$name": "$randomEmail"""")
    }
    val result7 = fullUserNameFields.foldLeft(result6) { case (data, name) =>
      data.replaceAll(s""""$name": ".+"""", s""""$name": "$randomFullName"""")
    }
    val result8 = usernameFields.foldLeft(result7) { case (data, name) =>
      data.replaceAll(s""""$name": ".+"""", s""""$name": "$randomUsername"""")
    }
    val result9 = branchFields.foldLeft(result8) { case (data, name) =>
      data.replaceAll(s""""$name": ".+"""", s""""$name": "$randomBranch"""")
    }
    val result10 = filenameFields.foldLeft(result9) { case (data, name) =>
      data.replaceAll(s""""$name": ".+"""", s""""$name": "$randomFilename"""")
    }
    val result11 = ipFields.foldLeft(result10) { case (data, name) =>
      data.replaceAll(s""""$name": ".+"""", s""""$name": "1.1.1.1"""")
    }
    val initialLines = result.split("\n")
    val resultLines  = result11.split("\n")
    val diff = initialLines.zip(resultLines).filter {
      //      case a@(l, r) if {
      //        println(a)
      //        println(l.count(_ == ':'))
      //        println(r.count(_ == ':'))
      //        println(l.trim != r.trim)
      //        false
      //      } => ???
      case (l, r) if l.trim == r.trim && Set("{", "[", "}", "]", "},", "],").contains(l.trim)                      => false
      case (l, r) if l.split(":").head == r.split(":").head && (l.trim.endsWith("{") || l.trim.endsWith("["))      => false
      case (l, r) if l.count(_ == ':') == 0 && r.count(_ == ':') == 0 && l.trim == r.trim                          => true /// ?
      case (l, r) if l.split(":").head == r.split(":").head && Set("null", "null,").contains(l.split(":")(1).trim) => false
      case (l, r) if l.split(":").head == r.split(":").head && safeFields.contains(l.split(":").head.trim)         => false
      case (l, r) if l.split(":").head == r.split(":").head && l.trim != r.trim                                    => false
      case (l, r) if l.split(":").head == r.split(":").head && staticOverrides.contains(l.trim.stripSuffix(","))   => false
      case _                                                                                                       => true
    }
    diff.foreach(println)

    println(result11)
  }

  private val FirstNames = Source.fromResource("firstNames.csv").getLines().toVector
  private val LastNames  = Source.fromResource("lastNames.csv").getLines().toVector

  private def randomName = Option(FirstNames(rand.nextInt(FirstNames.length)), LastNames(rand.nextInt(LastNames.length)))

  private def randomFullName = randomName.map(x => s"${x._1} ${x._2}").get

  private def randomEmail = randomName.map(x => s"${x._1}.${x._2}@koszmail.com".toLowerCase).get

  private def randomSha = rand.alphanumeric.filter(x => (x >= 'a' && x <= 'f') || (x >= '0' && x <= '9')).take(50).mkString

  private def randomUsername = randomName.map(x => s"${x._1.head}${x._2}".toLowerCase).get

  val branchNameChars = "abcdefghijklmnoprstuvwzABCDEFGHIJKLMNOPRSTUWVZ-1234567890"

  private def randomBranch = {
    val p1 = Vector.fill(5)(branchNameChars.apply(rand.nextInt(branchNameChars.length))).mkString
    val p2 = Vector.fill(15)(branchNameChars.apply(rand.nextInt(branchNameChars.length))).mkString
    s"$p1/$p2"
  }

  private def randomFilename = {
    val p1 = Vector.fill(20)(branchNameChars.apply(rand.nextInt(branchNameChars.length))).mkString
    val p2 = Vector.fill(3)(branchNameChars.apply(rand.nextInt(branchNameChars.length))).mkString
    s"$p1.$p2"
  }

}
