package io.gitlab.mateuszjaje.gitlabclient

import query.{Method, Methods}

package object sttpbackend {

  implicit def convertThisProjectMethodToSttpMethod(in: Method): sttp.model.Method = {
    in match {
      case Methods.Get    => sttp.model.Method.GET
      case Methods.Post   => sttp.model.Method.POST
      case Methods.Put    => sttp.model.Method.PUT
      case Methods.Delete => sttp.model.Method.DELETE
      case _              => ???
    }
  }

}
