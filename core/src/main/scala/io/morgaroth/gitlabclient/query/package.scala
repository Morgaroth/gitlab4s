package io.morgaroth.gitlabclient

import io.morgaroth.gitlabclient.models.SearchScope

package object query {
  def Scope(scope: SearchScope): ParamQuery = new StringKVParam("scope", scope.name)

  def Search(value: String): ParamQuery = new StringKVParam("search", value)
}
