package io.morgaroth.gitlabclient.helpers

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

object CustomDateTimeFormatter {

  def toISO8601UTC(in: ZonedDateTime): String = {
    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(in.withZoneSameInstant(ZoneOffset.UTC).withNano(0))
  }

  implicit final class RichZonedDateTime(in: ZonedDateTime) {
    def toISO8601UTC: String = CustomDateTimeFormatter.toISO8601UTC(in)
  }

}
