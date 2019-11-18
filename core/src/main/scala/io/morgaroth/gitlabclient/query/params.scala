package io.morgaroth.gitlabclient.query

import io.morgaroth.gitlabclient.GitlabConfig

import scala.language.implicitConversions

sealed trait ParamQuery {
  def render: String
}

object ParamQuery {

  implicit class PaginationParamsDSL(value: Int) {
    def pageNumParam = new IntKVParam("page", value)

    def pageSizeParam = new IntKVParam("per_page", value)
  }

}

class StringKVParam(key: String, value: String) extends ParamQuery {
  override def render: String = s"$key=$value"
}

class IntKVParam(key: String, value: Int) extends ParamQuery {
  override def render: String = s"$key=$value"
}

trait Method

object Methods {

  object Get extends Method

  object Post extends Method

  object Put extends Method

  object Delete extends Method

}

case class GitlabRequest(server: String,
                         method: Method,
                         path: String,
                         query: Vector[ParamQuery],
                         payload: Option[String],
                        ) {
  def withParams(pageSize: ParamQuery*): GitlabRequest =
    copy(query = query ++ pageSize.toList)

  lazy val render: String = {
    val base = s"${server.stripSuffix("/")}/${path.stripPrefix("/")}"
    if (query.nonEmpty) {
      s"$server/$path?${query.map(_.render).mkString("&")}"
    } else base
  }
}

case class RequestGenerator(cfg: GitlabConfig) {
  def get(path: String) =
    GitlabRequest(cfg.server, Methods.Get, path, Vector.empty, None)

  def get(path: String, query: ParamQuery*) =
    GitlabRequest(cfg.server, Methods.Get, path, query.toVector, None)
}