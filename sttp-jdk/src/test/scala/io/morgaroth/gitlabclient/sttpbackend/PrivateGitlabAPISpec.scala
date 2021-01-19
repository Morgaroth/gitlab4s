package io.morgaroth.gitlabclient.sttpbackend

import com.typesafe.scalalogging.LazyLogging
import io.morgaroth.gitlabclient._
import io.morgaroth.gitlabclient.models._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{ZoneOffset, ZonedDateTime}

class PrivateGitlabAPISpec extends AnyFlatSpec with Matchers with LazyLogging with HelperClasses {

  private val maybeAccessToken = Option(System.getenv("gitlab-access-token"))
  private val maybeAddress     = Option(System.getenv("gitlab-address"))
  assume(maybeAccessToken.isDefined, "gitlab-access-token env must be set for this test")
  assume(maybeAddress.isDefined, "gitlab-address env must be set for this test")

  private val cfg = GitlabConfig(maybeAccessToken.get, maybeAddress.get, ignoreSslErrors = true)
  val client      = new SttpGitlabAPISync(cfg, GitlabRestAPIConfig(true))

  behavior of "SttpGitlabAPI"

  it should "fetch current user" in {
    val result = client.getCurrentUser.value
    result shouldBe Symbol("right")
  }

  it should "private publicly visible project" in {
    val result = client.getProject("mjaje/jira-creative-rights").value
    result shouldBe Symbol("right")
  }

  it should "fetch public another project" in {
    val result = client.getProject(14414).value // b
    result shouldBe Symbol("right")
  }

  it should "list PRs" in {
    val result = client.getMergeRequests(14415).value // t
    result shouldBe Symbol("right")
    result.rightValue.size should be > 50
  }

  it should "execute mr search" in {
    val result = client.groupSearchMrs("be", "WHUSP-3279").value
    result shouldBe Symbol("right")
  }

  it should "execute commits search" in {
    val result = client.groupSearchCommits("be", "default-formatter").value
    result shouldBe Symbol("right")
  }

  it should "fetch branches" in {
    val result = client.getBranches(14415, Some("WHUSP-1950")).value // t
    result shouldBe Symbol("right")
  }

  it should "find awardable emojis of a merge requests" in {
    val result = client.getMergeRequestEmoji(14415, 74).value // t
    result shouldBe Symbol("right")
  }

  it should "list merge request approvals" in {
    val result = client.getApprovals(14470, 3).value // bon
    result shouldBe Symbol("right")
  }

  it should "return lot of merge requests" in {
    val result = client.getGroupMergeRequests(1905, MergeRequestStates.All, paging = EntitiesCount(100)).value // global
    result shouldBe Symbol("right")
  }

  it should "return merge request notes" in {
    val result = client.getMergeRequestNotes(14415, 74).value
    result shouldBe Symbol("right")
  }

  it should "return merge request discussions" in {
    //    val result = client.getMergeRequestDiscussions(14415, 74).value
    val result = client.getMergeRequestDiscussions(13605, 539).value
    result shouldBe Symbol("right")
    val result2 = result.rightValue
    result2.count(_.individual_note == false) shouldBe 98
  }

  it should "return commit" in {
    val result = client.getCommit(14504, "03dbf882").value
    result shouldBe Symbol("right")
  }

  it should "return commits" in {
    val result = client.getCommits(14415, paging = EntitiesCount(300)).value
    result shouldBe Symbol("right")
  }

  it should "fetch some diffs" in {
    val result = client.getDiffOfACommit(14415, "1c7ea4367f4bf20bf3f10d7bae97d75a3956bf7d").value
    result shouldBe Symbol("right")
  }

  it should "return commits from given period" in {
    val startTime = ZonedDateTime.of(2019, 8, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2))
    val endTime   = ZonedDateTime.of(2020, 2, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2))
    val result    = client.getCommits(14415, since = startTime, until = endTime).exec()
    result.size shouldBe 120
  }

  it should "read approval rules" in {
    val result = client.getMergeRequestApprovalRules(14414, 23).value // b
    result shouldBe Symbol("right")
    val result2 = client.getMergeRequestApprovalRules(14413, 28).value // a
    result2 shouldBe Symbol("right")
  }

  it should "create & delete approval rules" ignore { // ignored, as it is unsafe to run at any time
    val result2 = client
      .createApprovalRule(14413, 28, CreateMergeRequestApprovalRule.oneOf("TEST_APPROVAL_RULE_CREATED_BY_BOT", 1789, 754))
      .value
    // a
    result2 shouldBe Symbol("right")
    val result3 = client.deleteApprovalRule(14413, 28, result2.rightValue).value
    result3 shouldBe Symbol("right")
  }

  it should "return merge request related to commit" in {
    val result2 = client.getMergeRequestsOfCommit(16395, "3edf5c2b1cc575af2e3c67d345891dcbf2ed58f5").value // private
    result2.rightValue should have size 1
  }

  it should "return references commit is pushed to" in {
    val result2 = client.getCommitsReferences(16395, "31096d65dfe1a671ce0eca8801b9642a0e5a6c6e").value // private
    result2.rightValue should have size 3
  }

  it should "return merge requests for requested creation times" in {
    val startTime = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2))
    val endTime   = ZonedDateTime.of(2020, 1, 10, 0, 0, 0, 0, ZoneOffset.ofHours(2))
    val result =
      client.getGroupMergeRequests(1905, MergeRequestStates.All, createdAfter = startTime, createdBefore = endTime).exec() // global
    result should have size 51
    result.foreach { entry =>
      entry.created_at.isBefore(endTime) shouldBe true
      entry.created_at.isAfter(startTime) shouldBe true
    }
  }

  it should "return merge requests for requested emoji" in {
    val timeBarrier = ZonedDateTime.of(2020, 3, 12, 0, 0, 0, 0, ZoneOffset.ofHours(2))
    val result =
      client.getGroupMergeRequests(1905, MergeRequestStates.All, myReaction = "eyes", createdBefore = timeBarrier).exec() // global
    result should have size 9
  }

  it should "create, update and delete merge request note" in { // private project
    val note = client.createMergeRequestNote(16395, 3, "a new note!").exec()
    client.getMergeRequestNotes(16395, 3).exec() shouldBe Vector(note)
    val updatedNote = client.updateMergeRequestNote(16395, 3, note.id, "updated note").exec()
    client.getMergeRequestNotes(16395, 3).exec() shouldBe Vector(updatedNote)
    client.deleteMergeRequestNote(16395, 3, note.id).exec()
    client.getMergeRequestNotes(16395, 3).exec() shouldBe empty
  }

  it should "post merge thread" in {
    val mrIIdForTest  = 4
    val mr            = client.getMergeRequestDiff(16395, mrIIdForTest).exec()
    val diffToComment = mr.changes.get.head
    val payload       = CreateMRDiscussion.threadOnNewLine(mr.diff_refs, diffToComment, 7, "some comment")
    val thread        = client.createMergeRequestDiscussion(16395, mrIIdForTest, payload).exec()
    client.getMergeRequestDiscussions(16395, mrIIdForTest).exec() should contain(thread)
    val replyNote = client.createMergeRequestDiscussionNote(16395, mrIIdForTest, thread.id, "no no no").exec()

    val notesAfterCreation = client.getMergeRequestDiscussions(16395, mrIIdForTest).exec()
    notesAfterCreation.find(_.id == thread.id).get.notes should have size 2

    val updatedHead = client
      .updateMergeRequestDiscussionNote(16395, mrIIdForTest, thread.id, thread.notes.head.id, MRDiscussionUpdate.body("updated problem"))
      .exec()
    val updatedReply = client
      .updateMergeRequestDiscussionNote(16395, mrIIdForTest, thread.id, replyNote.id, MRDiscussionUpdate.body("updated no no no"))
      .exec()

    val notesAfterUpdates = client.getMergeRequestDiscussion(16395, mrIIdForTest, thread.id).exec()
    notesAfterUpdates.notes should contain theSameElementsAs Vector(updatedHead, updatedReply)

    client.updateMergeRequestNote(16395, mrIIdForTest, replyNote.id, "another update no no").exec()
    client.updateMergeRequestNote(16395, mrIIdForTest, thread.notes.head.id, "another head update").exec()
    val reUpdatedNote =
      client.updateMergeRequestDiscussionNote(16395, mrIIdForTest, thread.id, replyNote.id, MRDiscussionUpdate.resolve(true)).exec()

    val currentThread = client.resolveMergeRequestDiscussion(16395, mrIIdForTest, thread.id, resolved = true).exec()

    currentThread.notes should contain(reUpdatedNote)

    client.deleteMergeRequestNote(16395, mrIIdForTest, replyNote.id).exec()
    client.deleteMergeRequestNote(16395, mrIIdForTest, thread.notes.head.id).exec()
  }

  it should "return merge request diff" in {
    client.getMergeRequestDiff(14415, 74).exec()  // t
    client.getMergeRequestDiff(14414, 187).exec() // b
  }

  it should "return tags" in {
    client.getProjectTags(14415).exec() should have size 3
  }

  it should "create and delete tags" in {
    val tag = client.createTag(16395, "3.0.0", "ad82f5a3f8f37d3f0315b76383f495054eb74afb", None, None).exec()
    client.getProjectTags(16395).exec() should contain(tag)
    client.getTag(16395, "3.0.0").exec() shouldBe tag
    client.deleteTag(16395, "3.0.0").exec()
    client.getProjectTags(16395).exec() should not contain tag
  }

  it should "return environments list" in {
    //    val data = client.getProjectDeployments(14903).exec()
    //    val data = client.getProjectDeployments(14285).exec()
    val data = client.getProjectDeployments(16568, "E2E_Api_Tests").exec()
    data.length should not be 0
  }

  it should "read events" in {
    val startTime = UtcDate.of(ZonedDateTime.of(2020, 10, 1, 12, 0, 0, 0, ZoneOffset.ofHours(1)))
    //    val endTime   = ZonedDateTime.of(2020, 7, 3, 17, 0, 0, 0, ZoneOffset.ofHours(2))
    val result = client.getEvents(startTime, sort = Sorting(EventsSort.CreatedAt, Desc)).exec()

    result.foreach(println)
  }

  it should "return jobs of a pipeline" in {
    val result2 = client.getProjectPipelines(14414).exec()
    result2.foreach { pipeline =>
      client.getPipelineJobs(14414, pipeline.id).exec()
      client.getPipeline(14414, pipeline.id).exec()
    }
    val result = client.getPipelineJobs(16568, 846230).exec()
  }

  it should "fetch artifact" in {
    val data = client.downloadJobArtifacts(16568, 3589070).exec()
    println(data.filename)
  }

  it should "load push rules" in {
    //    val data = client.getPushRules(14414).exec() // b
    val data = client.getPushRules(14413).exec() // a
    println(data)
  }

  it should "change push rules" in {
    val data = client.getProject(14417).exec() // n
    println(data)
    //    client.editPushRules(14417, EditPushRuleRequest.Builder.withCommitMessageRegex("[a-z]+")).exec()
  }

//  var mrsChecked: Set[(BigInt, BigInt)] = Fi.readFile("checked-prs.log")
//  it should "get full merge request info for all MRs" in {
//    try {
//      client.getGroupMergeRequests(1905, paging = AllPages)
//        .exec()
//        .map(_.project_id)
//        .distinct
//        .foreach { mr =>
//          client.getProject(mr).exec()
//        }
//    } finally {
//      Fi.writeFile("checked.log", mrsChecked)
//    }
//  }
}
