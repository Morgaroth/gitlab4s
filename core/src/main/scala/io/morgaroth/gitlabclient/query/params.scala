package io.morgaroth.gitlabclient.query

import java.net.URLEncoder
import java.time.{LocalDate, ZonedDateTime}

import io.morgaroth.gitlabclient.helpers.CustomDateTimeFormatter
import io.morgaroth.gitlabclient.models.{MergeRequestState, SearchScope}
import io.morgaroth.gitlabclient.{GitlabConfig, Sorting, SortingFamily}

sealed trait ParamQuery {
  def render: String
}

object ParamQuery {
  def search(value: String) = new StringKVParam("search", value)

  implicit class fromMRState(mr: MergeRequestState) {
    def toParam: ParamQuery = MRStateParam(mr)
  }

  implicit class fromSearchScope(sc: SearchScope) {
    def toParam: ParamQuery = Scope(sc)
  }

  implicit class fromSorting[A <: SortingFamily](sc: Sorting[A]) {
    def toParams: List[ParamQuery] = {
      List("order_by".eqParam(sc.field.property), "sort".eqParam(sc.direction.toString))
    }
  }

  implicit class fromString(paramName: String) {
    def eqParam(value: String): ParamQuery = new StringKVParam(paramName, value)

    def eqParam(value: ZonedDateTime): ParamQuery = new StringKVParam(paramName, CustomDateTimeFormatter.toISO8601UTC(value))

    def eqParam(value: LocalDate): ParamQuery = new StringKVParam(paramName, CustomDateTimeFormatter.toDate(value))

    def eqParam(value: MergeRequestState): ParamQuery = new StringKVParam(paramName, value.name)
  }

  implicit class NumericParams(value: Int) {
    def pageNumParam = new IntKVParam("page", value)

    def pageSizeParam = new IntKVParam("per_page", value)
  }

}

class StringKVParam(key: String, value: String) extends ParamQuery {
  override def render: String = s"$key=${URLEncoder.encode(value, "utf-8")}"
}

class IntKVParam(key: String, value: Int) extends ParamQuery {
  override def render: String = s"$key=$value"
}

object NoParam extends StringKVParam("miss", "it")

case class MRStateParam(s: MergeRequestState) extends StringKVParam("state", s.name)

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
  def withParams(params: ParamQuery*): GitlabRequest =
    copy(query = query ++ params.toList.filterNot(_ == NoParam))

  lazy val render: String = {
    val base = s"${server.stripSuffix("/")}/${path.stripPrefix("/")}"
    if (query.nonEmpty) {
      s"$base?${query.map(_.render).mkString("&")}"
    } else base
  }
}

case class RequestGenerator(cfg: GitlabConfig) {
  def get(path: String): GitlabRequest =
    GitlabRequest(cfg.server, Methods.Get, path, Vector.empty, None)

  def delete(path: String): GitlabRequest =
    GitlabRequest(cfg.server, Methods.Delete, path, Vector.empty, None)

  def delete(path: String, query: ParamQuery*): GitlabRequest =
    GitlabRequest(cfg.server, Methods.Delete, path, query.toVector.filterNot(_ == NoParam), None)

  def get(path: String, query: ParamQuery*): GitlabRequest =
    GitlabRequest(cfg.server, Methods.Get, path, query.toVector.filterNot(_ == NoParam), None)

  def get(path: String, query: List[ParamQuery]): GitlabRequest =
    GitlabRequest(cfg.server, Methods.Get, path, query.toVector.filterNot(_ == NoParam), None)

  def post(path: String, query: ParamQuery*): GitlabRequest =
    GitlabRequest(cfg.server, Methods.Post, path, query.toVector.filterNot(_ == NoParam), None)

  def post(path: String, data: String, query: ParamQuery*): GitlabRequest =
    GitlabRequest(cfg.server, Methods.Post, path, query.toVector.filterNot(_ == NoParam), Some(data))

  def put(path: String, data: String): GitlabRequest =
    GitlabRequest(cfg.server, Methods.Put, path, Vector.empty, Some(data))
}

case class GitlabResponse(
                           headers: Map[String, String],
                           payload: String,
                         )