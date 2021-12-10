package io.gitlab.mateuszjaje.gitlabclient

import com.typesafe.config.Config

case class GitlabConfig(
    privateToken: String,
    server: String,
    ignoreSslErrors: Boolean = false,
    tokenPerProject: Map[String, String],
) {
  assert(privateToken.nonEmpty, "Gitlab credentials empty!")

  def tokenForPath(projectID: => Option[String]) = {
    if (tokenPerProject.nonEmpty) {
      projectID.fold(privateToken)(tokenPerProject.getOrElse(_, privateToken))
    } else privateToken
  }

}

object GitlabConfig {
  def fromConfig(config: Config) = new GitlabConfig(
    config.getString("private-token"),
    config.getString("server"),
    if (config.hasPath("ignore-ssl-errors")) config.getBoolean("ignore-ssl-errors") else false,
    loadTokenOverrides(config),
  )

  private def loadTokenOverrides(config: Config): Map[String, String] = {
    if (config.hasPath("project-tokens")) {
      val configObject = config.getObject("project-tokens")
      val keys         = configObject.keySet()
      val mm           = scala.collection.mutable.Map.empty[String, String]
      keys.forEach(key => mm.update(key, configObject.toConfig.getString(key)))
      mm.toMap
    } else Map.empty[String, String]
  }

}
