package de.hpi.dataset_versioning.db_synthesis.baseline.matching

import com.typesafe.scalalogging.StrictLogging
import de.hpi.dataset_versioning.data.change.temporal_tables.attribute.AttributeLineage
import de.hpi.dataset_versioning.db_synthesis.baseline.database.TemporalDatabaseTableTrait

class DataBasedMatchCalculator extends MatchCalculator with StrictLogging{

  val schemaMapper = new TemporalSchemaMapper()

  def getNonWildCardOverlapForMatchedAttributes(left: Set[AttributeLineage], right: Set[AttributeLineage]) = {
    val nonWildCardLeft = unionAllActiveTimes(left)
    val nonWildCardRight = unionAllActiveTimes(right)
    nonWildCardLeft.intersect(nonWildCardRight)
  }

  private def unionAllActiveTimes(lineages: Set[AttributeLineage]) = {
    if (lineages.size == 1) lineages.head.activeTimeIntervals else lineages.map(_.activeTimeIntervals).reduce((a, b) => {
      assert(a.intersect(b).isEmpty)
      val res = a.union(b)
      res
    })
  }

  def getNonWildCardOverlap(mapping: collection.Map[Set[AttributeLineage], Set[AttributeLineage]]) = {
    val mappingToOverlap = mapping.map(t => (t,getNonWildCardOverlapForMatchedAttributes(t._1,t._2)))
    val attrIdToNonWildCardOverlapLeft = mapping.flatMap(t => {
      val overlap = mappingToOverlap(t)
      t._1.map( al => (al.attrId,overlap.intersect(al.activeTimeIntervals)))
    })
    val attrIdToNonWildCardOverlapRight = mapping.flatMap(t => {
      val overlap = mappingToOverlap(t)
      t._2.map( al => (al.attrId,overlap.intersect(al.activeTimeIntervals)))
    })
    (mappingToOverlap,attrIdToNonWildCardOverlapLeft,attrIdToNonWildCardOverlapRight)
  }

  override def calculateMatch[A](tableA: TemporalDatabaseTableTrait[A], tableB: TemporalDatabaseTableTrait[A]): TableUnionMatch[A] = {
    calculateMatch(tableA,tableB,false)
  }

  def calculateMatch[A](tableA: TemporalDatabaseTableTrait[A], tableB: TemporalDatabaseTableTrait[A],includeTupleMapping:Boolean=false): TableUnionMatch[A] = {
    val sketchA = tableA
    val sketchB = tableB
    //enumerate all schema mappings:
    val schemaMappings = schemaMapper.enumerateAllValidSchemaMappings(tableA,tableB)
    var bestEvidence = 0
    var curBestMapping:collection.Map[Set[AttributeLineage], Set[AttributeLineage]] = null
    var curTupleMatching:TupleSetMatching[A] = null
    for(mapping <- schemaMappings){
      //TODO: build an index on the overlap of each attribute (?)
      //for now we just do it on the non-key attributes:
//      val indexBuilder = new MostDistinctTimestampIndexBuilder[A](Set(sketchA,sketchB),false)
//      val index = indexBuilder.buildTableIndexOnNonKeyColumns()
      val tupleMapper = new PairwiseTupleMapper(sketchA,sketchB,mapping)
      val tupleMapping = new TupleSetMatching(tableA,tableB,scala.collection.mutable.ArrayBuffer() ++ tupleMapper.getOptimalMapping())
      var bestPossibleEvidence = tupleMapping.totalEvidence
      if(bestPossibleEvidence > bestEvidence){
        if(bestPossibleEvidence>bestEvidence){
          bestEvidence = bestPossibleEvidence
          curBestMapping = mapping
          curTupleMatching = tupleMapping
        }
      }
    }
    val tupleMapping = if(includeTupleMapping && curTupleMatching!=null) Some(curTupleMatching) else None
    if(curBestMapping==null)
      new TableUnionMatch(tableA,tableB,None,0,(-1,-1),true,tupleMapping)
    else {
      val totalChangeBenefit = curTupleMatching.totalChangeBenefit
      new TableUnionMatch(tableA,tableB,Some(curBestMapping),bestEvidence,totalChangeBenefit,true,tupleMapping)
    }
  }
}
