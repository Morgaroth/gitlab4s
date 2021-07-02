package io.morgaroth.gitlabclient.helpers

sealed abstract class NullableField[+T] {
  def toList: List[T]
}

case object NullValue extends NullableField[Nothing] {
  override def toList = Nil
}

case class SomeValue[T](value: T) extends NullableField[T] {
  override def toList = List(value)
}

object NullableField {
  implicit def wrapOption[T](value: Option[T])(implicit du: DummyImplicit): NullableField[T] =
    value.map(SomeValue(_)).getOrElse(NullValue)

  implicit def wrapIntoSome[T](value: T): NullableField[T] = SomeValue(value)
}
