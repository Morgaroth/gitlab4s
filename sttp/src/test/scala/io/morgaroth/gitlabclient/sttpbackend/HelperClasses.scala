package io.morgaroth.gitlabclient.sttpbackend

import cats.data.EitherT
import cats.syntax.either._
import org.scalatest.Assertions.fail
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

trait HelperClasses {
  this: ScalaFutures =>

  implicit class RightValueable[E, V](either: Either[E, V]) {
    def rightValue: V = {
      either.valueOr(_ => fail(s"either is $either"))
    }
  }

  implicit class execable[E, V](either: EitherT[Future, E, V]) {

    def exec(): V = {
      either.value.futureValue.rightValue
    }

    def exec(t: Timeout): V = {
      either.value.futureValue(t).rightValue
    }
  }

}
