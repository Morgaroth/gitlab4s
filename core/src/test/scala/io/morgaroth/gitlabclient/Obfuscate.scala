package io.morgaroth.gitlabclient

import io.morgaroth.gitlabclient.marshalling.Gitlab4SMarshalling
import org.scalatest.{DoNotDiscover, FlatSpec, Matchers}

import scala.io.Source
import scala.util.Random

@DoNotDiscover
class Obfuscate extends FlatSpec with Matchers with Gitlab4SMarshalling {

  behavior of "Obfuscate"

  val textFields = Set("name", "title", "body", "source_branch", "target_branch", "username",
    "reference", "description", "author_name", "author_email", "committer_name", "committer_email", "message",
    "full_name", "full_path", "path", "old_path", "new_path")
  val numberFields = Set("id", "project_id", "iid", "source_project_id", "target_project_id", "noteable_iid", "noteable_id")
  val dateFields = Set("created_at", "updated_at", "authored_date", "committed_date", "created_at")
  val shaFields = Set("base_sha", "sha", "start_sha", "head_sha")
  val urlFields = Set("avatar_url", "web_url")

  it should "work" in {
    val resourceName = "merge_request_notes_list_2.json"
    val result = Source.fromResource(resourceName).mkString

    val result1 = textFields.foldLeft(result) {
      case (data, name) =>
        data.replaceAll(s""""$name": ".+"""", s""""$name": "${Random.alphanumeric.take(40).mkString}"""")
    }
    val result2 = numberFields.foldLeft(result1) {
      case (data, name) =>
        data.replaceAll(s""""$name": \\d+""", s""""$name": 111""")
    }
    val result3 = dateFields.foldLeft(result2) {
      case (data, name) =>
        data.replaceAll(s""""$name": ".+"""", s""""$name": "1970-01-01T12:00:00.000+00:00"""")
    }
    val result4 = shaFields.foldLeft(result3) {
      case (data, name) =>
        data.replaceAll(s""""$name": ".+"""", s""""$name": "635c65b158ad752ed05a07589cd14d450c04bfa1"""")
    }
    val result5 = urlFields.foldLeft(result4) {
      case (data, name) =>
        data.replaceAll(s""""$name": ".+"""", s""""$name": "https://google.com?q=${Random.alphanumeric.take(10).mkString}"""")
    }

    println(result5)
  }
}
