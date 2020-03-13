package io.morgaroth.gitlabclient.sttpbackend

import java.io.{BufferedWriter, File, FileNotFoundException, FileWriter}
import java.time.{ZoneOffset, ZonedDateTime}

import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import io.morgaroth.gitlabclient.models.{CreateMergeRequestApprovalRule, MergeRequestStates}
import io.morgaroth.gitlabclient.{EntitiesCount, GitlabConfig, GitlabRestAPIConfig}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Minutes, Span}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class PrivateGitlabAPISpec extends FlatSpec with Matchers with ScalaFutures with LazyLogging {

  implicit class RightValueable[E, V](either: Either[E, V]) {
    def rightValue: V = {
      either.valueOr(_ => fail(s"either is $either"))
    }
  }

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(Span(1, Minutes))

  private val maybeAccessToken = Option(System.getenv("gitlab-access-token"))
  private val maybeAddress = Option(System.getenv("gitlab-address"))
  assume(maybeAccessToken.isDefined, "gitlab-access-token env must be set for this test")
  assume(maybeAddress.isDefined, "gitlab-address env must be set for this test")

  val cfg = GitlabConfig(maybeAccessToken.get, maybeAddress.get, ignoreSslErrors = true)
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
    result.rightValue.size should be > 50
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
    val result = client.getGroupMergeRequests(1905, MergeRequestStates.All, paging = EntitiesCount(100)).value.futureValue // global
    result shouldBe Symbol("right")
  }

  it should "return merge request notes" in {
    val result = client.getMergeRequestNotes(14415, 74).value.futureValue
    result shouldBe Symbol("right")
  }

  it should "return merge request discussions" in {
    val result = client.getMergeRequestDiscussions(14415, 74).value.futureValue
    result shouldBe Symbol("right")
    val result2 = result.rightValue
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
    val result = client.getCommits(14415, since = startTime, until = endTime).value.futureValue.rightValue
    result.size shouldBe 120
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
    val result3 = client.deleteApprovalRule(14413, 28, result2.rightValue).value.futureValue
    result3 shouldBe Symbol("right")
  }

  it should "return merge request related to commit" in {
    val result2 = client.getMergeRequestsOfCommit(16395, "3edf5c2b1cc575af2e3c67d345891dcbf2ed58f5").value.futureValue // private
    result2.rightValue should have size 1
  }

  it should "return references commit is pushed to" in {
    val result2 = client.getCommitsReferences(16395, "31096d65dfe1a671ce0eca8801b9642a0e5a6c6e").value.futureValue // private
    result2.rightValue should have size 3
  }

  it should "return merge requests for requested creation times" in {
    val startTime = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2))
    val endTime = ZonedDateTime.of(2020, 1, 10, 0, 0, 0, 0, ZoneOffset.ofHours(2))
    val result = client.getGroupMergeRequests(1905, MergeRequestStates.All, createdAfter = startTime, createdBefore = endTime).value.futureValue.rightValue // global
    result should have size 51
    result.foreach { entry =>
      entry.created_at.isBefore(endTime) shouldBe true
      entry.created_at.isAfter(startTime) shouldBe true
    }
  }

  it should "return merge requests for requested emoji" in {
    val timeBarrier = ZonedDateTime.of(2020, 3, 12, 0, 0, 0, 0, ZoneOffset.ofHours(2))
    val result = client.getGroupMergeRequests(1905, MergeRequestStates.All, myReaction = "eyes", createdBefore = timeBarrier).value.futureValue.rightValue // global
    result should have size 9
  }

  it should "return merge request diff" in {
    client.getMergeRequestDiff(14415, 74).value.futureValue.rightValue // t
    client.getMergeRequestDiff(14414, 187).value.futureValue.rightValue // b
  }

  //  var mrsChecked: Set[(BigInt, BigInt)] = Fi.readFile("checked-prs.log")
  //  it should "get full merge request info for all MRs" in {
  //    try {
  //      client.getGroupMergeRequests(1905, paging = AllPages)
  //        .value.futureValue.rightValue
  //        .filterNot { mr => mrsChecked.contains(mr.project_id -> mr.iid) }
  //        .foreach { mr =>
  //          client.getMergeRequest(mr.project_id, mr.iid).value.futureValue.rightValue
  //          mrsChecked += (mr.project_id -> mr.iid)
  //        }
  //    } finally {
  //      Fi.writeFile("checked.log", mrsChecked)
  //    }
  //  }
}

object Fi {
  def readFile(filename: String): Set[(BigInt, BigInt)] = {
    try {
      val br = Source.fromFile(filename)
      val res = br.getLines().map { x =>
        val l :: r :: Nil = x.split(":").toList
        (BigInt(l), BigInt(r))
      }.toSet
      br.close()
      res
    } catch {
      case _: FileNotFoundException => Set.empty
    }
  }

  def writeFile(filename: String, lines: Set[(BigInt, BigInt)]): Unit = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file))
    lines.foreach(line => bw.write(s"${line._1}:${line._2}\n"))
    bw.close()
  }
}
