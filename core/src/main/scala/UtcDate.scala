package io.gitlab.mateuszjaje.gitlabclient

import helpers.CustomDateTimeFormatter

import java.time.{LocalDate, ZoneId, ZoneOffset, ZonedDateTime}

class UtcDate(date: LocalDate) {
  def toDateStr = CustomDateTimeFormatter.toDate(date)
}

object UtcDate {
  def of(zonedDateTime: ZonedDateTime): UtcDate =
    new UtcDate(zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDate)

  def of(year: Int, month: Int, day: Int, hour: Int, minute: Int, zone: ZoneId): UtcDate =
    of(ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone))

}
