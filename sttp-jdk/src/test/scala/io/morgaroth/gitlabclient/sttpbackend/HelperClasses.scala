package io.morgaroth.gitlabclient.sttpbackend

import cats.data.EitherT
import cats.syntax.either._
import org.scalatest.Assertions.fail
import org.scalatest.concurrent.PatienceConfiguration.Timeout

trait HelperClasses {

  implicit class RightValueable[E, V](either: Either[E, V]) {
    def rightValue: V =
      either.valueOr(_ => fail(s"either is $either"))

  }

  implicit class execable[E, V](either: EitherT[cats.Id, E, V]) {

    def exec(): V =
      either.value.rightValue

    def exec(t: Timeout): V =
      either.value.rightValue

  }

}
