package io.morgaroth.gitlabclient

import io.morgaroth.gitlabclient.query.{Method, Methods}

import scala.language.implicitConversions

package object sttpbackend {


  implicit def convertJiraMethodToSttpMethod(in: Method): sttp.model.Method = {
    if (in == Methods.Get) sttp.model.Method.GET else {
      if (in == Methods.Post) sttp.model.Method.POST else {
        if (in == Methods.Put) sttp.model.Method.PUT else {
          if (in == Methods.Delete) sttp.model.Method.DELETE else {
            ???
          }
        }
      }
    }
  }
}
