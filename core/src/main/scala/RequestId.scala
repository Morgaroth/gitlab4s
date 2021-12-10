package io.gitlab.mateuszjaje.gitlabclient

import java.util.UUID

case class RequestId(id: String, kind: String) {
  override lazy val toString = s"$id($kind)"
}

object RequestId {
  def newOne(kind: String) = new RequestId(UUID.randomUUID().toString, kind)
}
