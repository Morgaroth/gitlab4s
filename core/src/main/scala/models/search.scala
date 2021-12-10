package io.gitlab.mateuszjaje.gitlabclient
package models

sealed abstract class SearchScope(val name: String)

object SearchScope {

  case object MergeRequests extends SearchScope("merge_requests")

  case object WikiBlobs extends SearchScope("wiki_blobs")

  case object Milestones extends SearchScope("milestones")

  case object Issues extends SearchScope("issues")

  case object Projects extends SearchScope("projects")

  case object Commits extends SearchScope("commits")

  case object Blobs extends SearchScope("blobs")

  case object Users extends SearchScope("users")

  val all: Seq[SearchScope]            = Seq(MergeRequests, WikiBlobs, Milestones, Issues, Projects, Commits, Blobs, Users)
  val byName: Map[String, SearchScope] = all.map(x => x.name -> x).toMap
}
