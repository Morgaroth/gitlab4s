package io.morgaroth.gitlabclient.sttpbackend

import java.time.{ZoneOffset, ZonedDateTime}

import cats.syntax.either._
import io.morgaroth.gitlabclient.models.{CreateMergeRequestApprovalRule, MergeRequestStates}
import io.morgaroth.gitlabclient.{EntitiesCount, GitlabConfig, GitlabRestAPIConfig}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Minutes, Span}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class PrivateGitlabAPISpec extends FlatSpec with Matchers with ScalaFutures {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(Span(1, Minutes))

  private val maybeAccessToken = Option(System.getenv("gitlab-access-token"))
  private val maybeAddress = Option(System.getenv("gitlab-address"))
  assume(maybeAccessToken.isDefined, "gitlab-access-token env must be set for this test")
  assume(maybeAddress.isDefined, "gitlab-address env must be set for this test")

  val cfg = GitlabConfig(maybeAccessToken.get, maybeAddress.get, true)
  val client = new SttpGitlabAPI(cfg, GitlabRestAPIConfig(true))

  behavior of "SttpGitlabAPI"

  it should "fetch current user" in {
    val result = client.getCurrentUser.value.futureValue
    result shouldBe Symbol("right")
  }

  it should "private publicly visible project" in {
    val result = client.getProject("mjaje/jira-creative-rights").value.futureValue
    result shouldBe Symbol("right")
  }

  it should "fetch public another project" in {
    val result = client.getProject(14414).value.futureValue // b
    result shouldBe Symbol("right")
  }

  it should "list PRs" in {
    val result = client.getMergeRequests(14415).value.futureValue // t
    result shouldBe Symbol("right")
    result.getOrElse(throw new IllegalArgumentException).size should be > 50
  }

  it should "execute mr search" in {
    val result = client.groupSearchMrs("be", "WHUSP-3279").value.futureValue
    result shouldBe Symbol("right")
  }

  it should "execute commits search" in {
    val result = client.groupSearchCommits("be", "default-formatter").value.futureValue
    result shouldBe Symbol("right")
  }

  it should "fetch branches" in {
    val result = client.getBranches(14415, Some("WHUSP-1950")).value.futureValue // t
    result shouldBe Symbol("right")
  }

  it should "find awardable emojis of a merge requests" in {
    val result = client.getMergeRequestEmoji(14415, 74).value.futureValue // t
    result shouldBe Symbol("right")
  }

  it should "list merge request approvals" in {
    val result = client.getApprovals(14470, 3).value.futureValue // bon
    result shouldBe Symbol("right")
  }

  it should "return lot of merge requests" in {
    val result = client.getGroupMergeRequests(1905, MergeRequestStates.All, EntitiesCount(100)).value.futureValue // global
    result shouldBe Symbol("right")
  }

  it should "return merge request notes" in {
    val result = client.getMergeRequestNotes(14415, 74).value.futureValue
    result shouldBe Symbol("right")
  }

  it should "return merge request discussions" in {
    val result = client.getMergeRequestDiscussions(14415, 74).value.futureValue
    result shouldBe Symbol("right")
    val result2 = result.valueOr(x => throw new RuntimeException(x.toString))
    result2.count(_.individual_note == false) shouldBe 98
  }

  it should "return commits" in {
    val result = client.getCommits(14415, paging = EntitiesCount(300)).value.futureValue
    result shouldBe Symbol("right")
  }

  it should "fetch some diffs" in {
    val result = client.getDiffOfACommit(14415, "1c7ea4367f4bf20bf3f10d7bae97d75a3956bf7d").value.futureValue
    result shouldBe Symbol("right")
  }

  it should "return commits from given period" in {
    val startTime = ZonedDateTime.of(2019, 8, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2))
    val endTime = ZonedDateTime.of(2020, 2, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2))
    val result = client.getCommits(14415, since = startTime, until = endTime).value.futureValue
    result shouldBe Symbol("right")
    val result2 = result.valueOr(x => throw new RuntimeException(x.toString))
    result2.size shouldBe 120
  }

  it should "read approval rules" in {
    val result = client.getMergeRequestApprovalRules(14414, 23).value.futureValue // b
    result shouldBe Symbol("right")
    val result2 = client.getMergeRequestApprovalRules(14413, 28).value.futureValue // a
    result2 shouldBe Symbol("right")
  }

  it should "create & delete approval rules" ignore { // ignored, as it is unsafe to run at any time
    val result2 = client.createApprovalRule(14413, 28, CreateMergeRequestApprovalRule.oneOf("TEST_APPROVAL_RULE_CREATED_BY_BOT", 1789, 754)).value.futureValue // a
    result2 shouldBe Symbol("right")
    val result3 = client.deleteApprovalRule(14413, 28, result2.getOrElse(throw new RuntimeException(""))).value.futureValue
    result3 shouldBe Symbol("right")
  }
}
