package io.gitlab.mateuszjaje.gitlabclient
package helpers

import marshalling.Gitlab4SMarshalling
import models.PushRules

import cats.syntax.either._
import io.circe.parser.parse
import org.scalatest.DoNotDiscover
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

@DoNotDiscover
class FindUnmappedFields extends AnyFlatSpec with Matchers with Gitlab4SMarshalling {

  behavior of "FindUnmappedFields"

  it should "work" in {
    val file       = Source.fromResource("debug.json").mkString
    val parsedKeys = MJson.encode(MJson.read[PushRules](file).valueOrDie).asObject.get.keys.toSet
    val parsedData = parse(file).valueOrDie.asObject.get
    val loadedKeys = parsedData.keys.toSet
    val missedKeys = loadedKeys.diff(parsedKeys)
    missedKeys.foreach(key => println(s"$key needs to be covered in models ${parsedData.apply(key)}"))
  }

  implicit class EitherOrDie[A](e: Either[io.circe.Error, A]) {
    def valueOrDie = e.valueOr(throw _)
  }

}
