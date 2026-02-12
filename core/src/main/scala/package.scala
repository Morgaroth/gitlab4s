package io.gitlab.mateuszjaje

package object gitlabclient {
  def wrap[T](value: T): List[T] = Option(value).toList
}
