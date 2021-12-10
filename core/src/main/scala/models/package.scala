package io.gitlab.mateuszjaje.gitlabclient

import io.circe.generic.extras.Configuration

package object models {
  implicit val config: Configuration = Configuration.default

}
