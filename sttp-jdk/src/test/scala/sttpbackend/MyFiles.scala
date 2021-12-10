package io.gitlab.mateuszjaje.gitlabclient
package sttpbackend

import java.io.{BufferedWriter, File, FileNotFoundException, FileWriter}
import scala.io.Source

object MyFiles {
  def readFile(filename: String): Set[(BigInt, BigInt)] = {
    try {
      val br = Source.fromFile(filename)
      val res = br
        .getLines()
        .map { x =>
          val l :: r :: Nil = x.split(":").toList
          (BigInt(l), BigInt(r))
        }
        .toSet
      br.close()
      res
    } catch {
      case _: FileNotFoundException => Set.empty
    }
  }

  def writeFile(filename: String, lines: Set[(BigInt, BigInt)]): Unit = {
    val file = new File(filename)
    val bw   = new BufferedWriter(new FileWriter(file))
    lines.foreach(line => bw.write(s"${line._1}:${line._2}\n"))
    bw.close()
  }

  def writeFile(filename: String, data: String): Unit = {
    val file = new File(filename)
    val bw   = new BufferedWriter(new FileWriter(file))
    bw.write(data)
    bw.flush()
    bw.close()
  }

}
