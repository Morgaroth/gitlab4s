package io.morgaroth.gitlabclient.helpers

import java.time.{ZoneOffset, ZonedDateTime}

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CustomDateTimeFormatterTest extends AnyFlatSpec with Matchers {

  behavior of "DateTimeFormatter"

  it should "print date well" in {
    val date = ZonedDateTime.of(2020, 5, 12, 4, 45, 34, 1234, ZoneOffset.ofHours(2))
    date.toString shouldBe "2020-05-12T04:45:34.000001234+02:00"
    CustomDateTimeFormatter.toISO8601UTC(date) shouldBe "2020-05-12T02:45:34Z"
  }
}
