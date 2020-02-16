package io.morgaroth.gitlabclient

import java.net.URLEncoder

import scala.language.implicitConversions

trait EntityId {
  def toStringId: String
}

case class NumericEntityIdId(id: BigInt) extends EntityId {
  val toStringId: String = id.toString()
}

case class StringEntityId(path: String) extends EntityId {
  val toStringId: String = URLEncoder.encode(path, "utf-8")
}

object EntityId {
  implicit def wrapFromBigInt(value: BigInt): EntityId = NumericEntityIdId(value)

  implicit def wrapFromInt(value: Int): EntityId = NumericEntityIdId(value)

  implicit def wrapFromPth(value: String): EntityId = StringEntityId(value)
}
