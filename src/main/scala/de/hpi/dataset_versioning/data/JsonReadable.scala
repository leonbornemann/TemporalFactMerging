package de.hpi.dataset_versioning.data

import de.hpi.dataset_versioning.data.json.custom_serializer.{DatasetInstanceKeySerializer, LocalDateKeySerializer, LocalDateSerializer}
import de.hpi.dataset_versioning.data.metadata.Provenance
import org.json4s.ext.EnumNameSerializer
import org.json4s.jackson.JsonMethods.parse
import org.json4s.{DefaultFormats, _}

import java.io.{File, FileInputStream}
import scala.io.Source

trait JsonReadable[T<:AnyRef] {

  implicit def formats = (DefaultFormats.preservingEmptyValues
    + new EnumNameSerializer(Provenance)
    + LocalDateSerializer
    + DatasetInstanceKeySerializer
    + LocalDateKeySerializer)


  def fromJsonString(json: String)(implicit m:Manifest[T]) = {
    parse(json).extract[T]
  }

  def fromJsonFile(path: String)(implicit m:Manifest[T]) = {
    //val string = Source.fromFile(path).getLines().mkString("\n")
    val file = new FileInputStream( new File(path))
    val json = parse(file)
    json.extract[T]
  }

  def fromJsonObjectPerLineFile(path:String)(implicit m:Manifest[T]):collection.Seq[T] = {
    val result = scala.collection.mutable.ArrayBuffer[T]()
    Source.fromFile(path).getLines()
      .foreach(l => {
        result.addOne(fromJsonString(l))
      })
    result
  }
}
