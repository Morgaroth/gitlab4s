package io.morgaroth.gitlabclient.sttpbackend

import io.morgaroth.gitlabclient.{GitlabConfig, GitlabRestAPIConfig}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Minutes, Span}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class PrivateGitlabAPISpec extends FlatSpec with Matchers with ScalaFutures {


  override implicit def patienceConfig: PatienceConfig = PatienceConfig(Span(1, Minutes))

  private val maybeAccessToken = Option(System.getenv("gitlab-access-token"))
  private val maybeAddress = Option(System.getenv("gitlab-address"))
  assume(maybeAccessToken.isDefined, "gitlab-private-token env must be set for this test")
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
    val result = client.getProject("be/services/be-betting-service").value.futureValue
    result shouldBe Symbol("right")
  }

  it should "list PRs" in {
    val result = client.getMergeRequests("be/services/be-betting-service").value.futureValue
    result shouldBe Symbol("right")
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
    val result = client.getBranches("be/services/be-trading-service", Some("WHUSP-1950")).value.futureValue
    result shouldBe Symbol("right")
//    val result2 = client.getBranches("be/services/be-trading-service", None).value.futureValue
//    result2 shouldBe Symbol("right")
  }
}
