package io.gitlab.mateuszjaje.gitlabclient
package helpers

sealed abstract class NullableField[+T] {
  def toList: List[T]
}

case object NullValue extends NullableField[Nothing] {
  override def toList: List[Nothing] = Nil
}

case class SomeValue[T](value: T) extends NullableField[T] {
  override def toList: List[T] = List(value)
}

object NullableField {
  implicit def wrapOption[T](value: Option[T])(implicit du: DummyImplicit): NullableField[T] =
    value.map(SomeValue(_)).getOrElse(NullValue)

  implicit def wrapIntoSome[T](value: T): NullableField[T] = SomeValue(value)
}
