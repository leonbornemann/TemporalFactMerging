import de.hpi.dataset_versioning.data.change.ReservedChangeValues
import de.hpi.dataset_versioning.data.change.temporal_tables.attribute.{AttributeLineage, AttributeState, SurrogateAttributeLineage}
import de.hpi.dataset_versioning.data.change.temporal_tables.tuple.ValueLineage
import de.hpi.dataset_versioning.data.simplified.Attribute
import de.hpi.dataset_versioning.db_synthesis.baseline.database.surrogate_based.{SurrogateBasedSynthesizedTemporalDatabaseTableAssociationSketch, SurrogateBasedTemporalRowSketch}
import de.hpi.dataset_versioning.db_synthesis.baseline.decomposition.DecomposedTemporalTableIdentifier
import de.hpi.dataset_versioning.db_synthesis.baseline.matching.{AssociationEdgeCandidateFinder, DataBasedMatchCalculator}
import de.hpi.dataset_versioning.db_synthesis.sketches.field.Variant2Sketch
import de.hpi.dataset_versioning.io.IOService

import java.time.LocalDate
import scala.collection.mutable

object IndexingTest extends App {
  IOService.socrataDir = "/home/leon/data/dataset_versioning/socrata/testDir/"

  val lineagesAsStrings = IndexedSeq(
    (0,"AEF"),
    (1,"BEF"),
    (2,"CEF"),
    (3,"DEF"),
    (4,"_EE"),
    (5,"_BE"),
    (6,"_BE"),
    (7,"_GH"),
    (8,"_IJ"),
    (9,"_KL"),
  )
  val subdomain = "dummy"
  val associations = lineagesAsStrings.map{case (i,s) => {
    val id = s"#$i"
    val originalID = DecomposedTemporalTableIdentifier(subdomain,id,0,Some(0))
    val attrID = i
    val attrState = new AttributeState(Some(Attribute(s"attr$i",attrID,None,None)))
    val attrLineage = new AttributeLineage(attrID,mutable.TreeMap(IOService.STANDARD_TIME_FRAME_START ->attrState ))
    val key = IndexedSeq(new SurrogateAttributeLineage(i,attrID))
    val valueSketch = getValueLineage(s)
    val rows = mutable.ArrayBuffer(new SurrogateBasedTemporalRowSketch(IndexedSeq(0),valueSketch,IndexedSeq()))
    new SurrogateBasedSynthesizedTemporalDatabaseTableAssociationSketch(id,
      mutable.HashSet(),
      mutable.HashSet(originalID),
      key,
      attrLineage,
      IndexedSeq[SurrogateAttributeLineage](),
      rows)
  }}
  associations.foreach(r => println(r.rows.head.valueSketch.getValueLineage))
  val clusterer = new AssociationEdgeCandidateFinder(associations.toSet,new DataBasedMatchCalculator(),8,true)

  private def getValueLineage(s: String) = {
    val a = s.zipWithIndex.map { case (char, index) => (IOService.STANDARD_TIME_FRAME_START.plusDays(index), getValue(char)) }
    val filtered = a.zipWithIndex
      .filter{case (t,i) => i==0 || a(i-1)._2!=t._2}
      .map(_._1)
    val b = new ValueLineage(mutable.TreeMap[LocalDate,Any]() ++ filtered)
    Variant2Sketch.fromValueLineage(b)
  }

  private def getValue(char: Char) = {
    if(char == '_') ReservedChangeValues.NOT_EXISTANT_COL else char
  }
}
