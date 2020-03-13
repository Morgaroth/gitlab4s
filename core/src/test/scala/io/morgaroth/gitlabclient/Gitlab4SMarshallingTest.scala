package io.morgaroth.gitlabclient

import io.circe.generic.auto._
import io.morgaroth.gitlabclient.marshalling.Gitlab4SMarshalling
import io.morgaroth.gitlabclient.models._
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class Gitlab4SMarshallingTest extends FlatSpec with Matchers with Gitlab4SMarshalling with TableDrivenPropertyChecks {

  behavior of "Gitlab4sMarshalling"

  val currentUserInfoTable = Table(heading = "current_user_info") ++ Seq(
    "current_user_info.json",
  )
  it should "parse full user info" in {
    forAll(currentUserInfoTable) { resourceName =>
      val result = MJson.read[GitlabFullUser](Source.fromResource(resourceName).mkString)
      result shouldBe Symbol("right")
    }
  }

  val projectInfos = Table("project info") ++ Seq(
    "other_project_info.json",
    "own_project_info.json",
    "own_project_info_2.json",
  )
  it should "parse project info" in {
    forAll(projectInfos) { resourceName =>
      val result = MJson.read[ProjectInfo](Source.fromResource(resourceName).mkString)
      result shouldBe Symbol("right")
    }
  }

  val mergeRequests = Table("merge requests list") ++ Seq(
    "merge_requests_list_1.json",
    "merge_requests_list_2.json",
    "merge_requests_list_3.json",
    "merge_requests_list_4.json",
  )
  it should "parse merge requests list" in {
    forAll(mergeRequests) { resourceName =>
      val result = MJson.read[Vector[MergeRequestInfo]](Source.fromResource(resourceName).mkString)
      result shouldBe Symbol("right")
    }
  }

  val mergeRequestsSearch = Table("merge requests search") ++ Seq(
    "global-mr-search-result-1.json"
  )
  it should "parse merge requests search" in {
    forAll(mergeRequestsSearch) { resourceName =>
      val result = MJson.read[Vector[MergeRequestInfo]](Source.fromResource(resourceName).mkString)
      result shouldBe Symbol("right")
    }
  }

  it should "parse branches search" in {
    Vector(
      "project_branches_1.json",
    ).foreach { resourceName =>
      val result = MJson.read[Vector[GitlabBranchInfo]](Source.fromResource(resourceName).mkString)
      result shouldBe Symbol("right")
    }
  }

  it should "parse awardable emojis" in {
    Vector(
      "awards_of_mr_1.json",
    ).foreach { resourceName =>
      val result = MJson.read[Vector[EmojiAward]](Source.fromResource(resourceName).mkString)
      result shouldBe Symbol("right")
    }
  }

  val approvalsListTable = Table("merge request approvals") ++ Seq(
    "approvals_of_mr_1.json",
    "approvals_of_mr_2.json",
  )
  it should "parse approvals list" in {
    forAll(approvalsListTable) { resourceName =>
      val result = MJson.read[MergeRequestApprovals](Source.fromResource(resourceName).mkString)
      result shouldBe Symbol("right")
    }
  }

  val mergeRequestNotesListTable = Table("merge request notes") ++ Seq(
    "merge_request_notes_list_1.json",
    "merge_request_notes_list_2.json",
  )
  it should "parse merge request's notes list" in {
    forAll(mergeRequestNotesListTable) { resourceName =>
      val result = MJson.read[Vector[MergeRequestNote]](Source.fromResource(resourceName).mkString)
      result shouldBe Symbol("right")
    }
  }

  val mergeRequestApprovalRules = Table("merge request approval rules") ++ Seq(
    "approval_rules_of_mr_1.json",
    "approval_rules_of_mr_2.json",
  )
  it should "parse merge request's approval rules" in {
    forAll(mergeRequestApprovalRules) { resourceName =>
      val result = MJson.read[Vector[MergeRequestApprovalRule]](Source.fromResource(resourceName).mkString)
      result shouldBe Symbol("right")
    }
  }
}
