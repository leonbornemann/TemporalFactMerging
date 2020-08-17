package de.hpi.dataset_versioning.data.json.helper

import java.time.LocalDate

import de.hpi.dataset_versioning.data.metadata.custom.schemaHistory.AttributeLineageWithHashMap
import de.hpi.dataset_versioning.data.simplified.Attribute
import de.hpi.dataset_versioning.data.{JsonReadable, JsonWritable}
import de.hpi.dataset_versioning.db_synthesis.baseline.decomposition.{DecomposedTemporalTable, DecomposedTemporalTableIdentifier}

import scala.collection.mutable

case class DecomposedTemporalTableHelper(id:DecomposedTemporalTableIdentifier,
                                         attributes: collection.IndexedSeq[AttributeLineageWithHashMap],
                                         originalFDLHS: collection.Set[AttributeLineageWithHashMap],
                                         primaryKeyByVersion: Map[LocalDate,collection.Set[Attribute]],
                                         referencedTables:mutable.HashSet[DecomposedTemporalTableIdentifier])  extends JsonWritable[DecomposedTemporalTableHelper]{

  def toDecomposedTemporalTable = DecomposedTemporalTable(id,mutable.ArrayBuffer() ++ attributes.map(_.toDecomposedTemporalTable),
    originalFDLHS.map(_.toDecomposedTemporalTable),
    primaryKeyByVersion,
    referencedTables)
}

object DecomposedTemporalTableHelper extends JsonReadable[DecomposedTemporalTableHelper]
