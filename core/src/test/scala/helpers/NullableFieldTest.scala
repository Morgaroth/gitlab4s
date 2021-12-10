package io.gitlab.mateuszjaje.gitlabclient
package helpers

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NullableFieldTest extends AnyFlatSpec with Matchers {

  "NullableField" should "work" in {
    (123: NullableField[Int]) shouldBe SomeValue(123)
    ("": NullableField[String]) shouldBe SomeValue("")
    (Option("dsadsa"): NullableField[String]) shouldBe SomeValue("dsadsa")
    (None: NullableField[String]) shouldBe NullValue
  }

}
