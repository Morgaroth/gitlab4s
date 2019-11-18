package io.morgaroth.gitlabclient

import com.typesafe.config.Config

import scala.util.Try

case class GitlabConfig(
                         privateToken: String,
                         server: String,
                         ignoreSslErrors: Boolean = false,
                       ) {
  assert(privateToken.nonEmpty, "Gitlab credentials empty!")
}

object GitlabConfig {
  def fromConfig(config: Config) = new GitlabConfig(
    config.getString("private-token"),
    config.getString("server"),
    Try(config.getBoolean("ignore-ssl-errors")).getOrElse(false)
  )
}
