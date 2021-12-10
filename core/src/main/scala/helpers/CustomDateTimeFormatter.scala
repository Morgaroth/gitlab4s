package io.gitlab.mateuszjaje.gitlabclient
package helpers

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset, ZonedDateTime}

object CustomDateTimeFormatter {

  def toISO8601UTC(in: ZonedDateTime): String =
    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(in.withZoneSameInstant(ZoneOffset.UTC).withNano(0))

  def toDate(in: ZonedDateTime): String =
    DateTimeFormatter.ISO_LOCAL_DATE.format(in.withZoneSameInstant(ZoneOffset.UTC).withNano(0))

  def toDate(in: LocalDate): String =
    DateTimeFormatter.ISO_LOCAL_DATE.format(in)

  implicit final class RichZonedDateTime(in: ZonedDateTime) {
    def toISO8601UTC: String = CustomDateTimeFormatter.toISO8601UTC(in)

    def toDateStr: String = CustomDateTimeFormatter.toDate(in)
  }

  implicit final class RichLocalDate(in: LocalDate) {
    def toDateStr: String = CustomDateTimeFormatter.toDate(in)
  }

}
