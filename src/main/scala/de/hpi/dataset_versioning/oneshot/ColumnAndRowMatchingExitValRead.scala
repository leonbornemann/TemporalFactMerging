package de.hpi.dataset_versioning.oneshot

import java.io.PrintWriter

import scala.io.Source

object ColumnAndRowMatchingExitValRead extends App {

  val file = args(0)
  val a = Source.fromFile(file)
    .getLines()
    .toSeq
    .tail
    .map(_.split("\t"))
    .filter(l => l(6).toInt!=0)
    .foreach(a => println(getID(a(8))))

  def getID(str: String) = {
    str.split("\\s")(8)
  }

}
