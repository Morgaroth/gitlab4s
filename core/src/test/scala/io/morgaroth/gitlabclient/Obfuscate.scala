package io.morgaroth.gitlabclient

import io.morgaroth.gitlabclient.marshalling.Gitlab4SMarshalling
import org.scalatest.{DoNotDiscover, FlatSpec, Matchers}

import scala.io.Source
import scala.util.Random

@DoNotDiscover
class Obfuscate extends FlatSpec with Matchers with Gitlab4SMarshalling {

  behavior of "Obfuscate"

  val textFields = Set("title", "body", "source_branch", "target_branch", "diff", "a_mode", "b_mode",
    "reference", "description", "message", "full_name", "full_path", "path", "old_path", "new_path")
  val numberFields = Set("id", "project_id", "iid", "source_project_id", "target_project_id", "noteable_iid", "noteable_id")
  val dateFields = Set("created_at", "updated_at", "authored_date", "committed_date", "created_at")
  val shaFields = Set("base_sha", "sha", "start_sha", "head_sha", "id", "short_id")
  val urlFields = Set("avatar_url", "web_url")
  val emailFields = Set("author_email", "committer_email")
  val usernameFields = Set("username")
  val fullUserNameFields = Set("author_name", "committer_name", "name")

  it should "work" in {
    val resourceName = "commit_diffs_1.json"
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
        data.replaceAll(s""""$name": ".+"""", s""""$name": "$randomSha"""")
    }
    val result5 = urlFields.foldLeft(result4) {
      case (data, name) =>
        data.replaceAll(s""""$name": ".+"""", s""""$name": "https://google.com?q=${Random.alphanumeric.take(10).mkString}"""")
    }
    val result6 = emailFields.foldLeft(result5) {
      case (data, name) =>
        data.replaceAll(s""""$name": ".+"""", s""""$name": "$randomEmail"""")
    }
    val result7 = fullUserNameFields.foldLeft(result6) {
      case (data, name) =>
        data.replaceAll(s""""$name": ".+"""", s""""$name": "$randomFullName"""")
    }
    val result8 = usernameFields.foldLeft(result7) {
      case (data, name) =>
        data.replaceAll(s""""$name": ".+"""", s""""$name": "$randomUsername"""")
    }

    println(result8)
  }

  private val FirstNames = Source.fromResource("firstNames.csv").getLines().toVector
  private val LastNames = Source.fromResource("lastNames.csv").getLines().toVector

  private def randomName = Option(FirstNames(Random.nextInt(FirstNames.length)), LastNames(Random.nextInt(LastNames.length)))

  private def randomFullName = randomName.map(x => s"${x._1} ${x._2}").get

  private def randomEmail = randomName.map(x => s"${x._1}.${x._2}@koszmail.com".toLowerCase).get

  private def randomSha = Random.alphanumeric.filter(x => (x >= 'a' && x <= 'f') || (x >= '0' && x <= '9')).take(50).mkString

  private def randomUsername = randomName.map(x => s"${x._1.head}${x._2}".toLowerCase).get
}
