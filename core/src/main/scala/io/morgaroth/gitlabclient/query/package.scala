package io.morgaroth.gitlabclient

import io.morgaroth.gitlabclient.models.SearchScope

package object query {
  def Scope(scope: SearchScope) = new StringKVParam("scope", scope.name)

  def Search(value: String) = new StringKVParam("search", value)
}
