package io.morgaroth.gitlabclient.sttpbackend

import io.morgaroth.gitlabclient.{GitlabConfig, GitlabRestAPIConfig}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

class SttpGitlabAPISpec extends AnyFlatSpec with Matchers with ScalaFutures {

  private val maybeAccessToken = Option(System.getenv("gitlab-access-token"))
  assume(maybeAccessToken.isDefined, "gitlab-private-token env must be set for this test")

  val apiToken = maybeAccessToken.get
  val cfg      = GitlabConfig(apiToken, "https://gitlab.com")
  val client   = new SttpGitlabAPI(cfg, GitlabRestAPIConfig())

  "SttpGitlabAPI" should "fetch current user" in {
    val result = client.getCurrentUser.value.futureValue
    result.isRight shouldBe true
  }

  it should "private publicly visible project" in {
    val result = client.getProject("morgaroth/bitbucket4s").value.futureValue
    result.isRight shouldBe true
  }

  it should "fetch public another project" in {
    val result = client.getProject("tortoisegit/tortoisegit").value.futureValue
    result.isRight shouldBe true
  }

  it should "fetch branches" in {
    val result = client.getBranches("tortoisegit/tortoisegit", None).value.futureValue
    result.isRight shouldBe true
  }

}
