package io.morgaroth.gitlabclient.marshalling

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

trait CommonDateSerializer {
  def print(d: ZonedDateTime): String = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
    formatter.format(d)
  }

  implicit def datetimePrinter(d: ZonedDateTime): {} = new {
    def dateString: String = print(d)
  }

}

object CommonDateSerializer extends CommonDateSerializer
