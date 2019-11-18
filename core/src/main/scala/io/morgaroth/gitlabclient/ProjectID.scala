package io.morgaroth.gitlabclient

import java.net.URLEncoder

import scala.language.implicitConversions

trait ProjectID {
  def toStringId: String
}

case class RawNumericProjectId(id: BigInt) extends ProjectID {
  val toStringId: String = id.toString()
}

case class PathProjectId(path: String) extends ProjectID {
  val toStringId: String = URLEncoder.encode(path, "utf-8")
}

object ProjectID {
  implicit def wrapFromBigInt(value: BigInt): ProjectID = RawNumericProjectId(value)

  implicit def wrapFromPth(value: String): ProjectID = PathProjectId(value)
}
