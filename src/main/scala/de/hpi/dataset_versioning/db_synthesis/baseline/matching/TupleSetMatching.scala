package de.hpi.dataset_versioning.db_synthesis.baseline.matching

import com.typesafe.scalalogging.StrictLogging
import de.hpi.dataset_versioning.db_synthesis.baseline.config.GLOBAL_CONFIG
import de.hpi.dataset_versioning.db_synthesis.baseline.database.TemporalDatabaseTableTrait

import scala.collection.mutable.ArrayBuffer

@SerialVersionUID(3L)
class TupleSetMatching[A](val tableA: TemporalDatabaseTableTrait[A],
                          val tableB: TemporalDatabaseTableTrait[A],
                          val matchedTuples: ArrayBuffer[General_Many_To_Many_TupleMatching[A]] = ArrayBuffer[General_Many_To_Many_TupleMatching[A]]()) extends Serializable{

  implicit class TuppleAdd(t: (Int, Int)) {
    def +(p: (Int, Int)) = (p._1 + t._1, p._2 + t._2)
    def -(p: (Int, Int)) = (t._1 - p._1, t._2 - p._2)
  }

  def totalEvidence = matchedTuples.map(_.evidence).sum

  def totalChangeBenefit = {
    val before = GLOBAL_CONFIG.CHANGE_COUNT_METHOD.countChanges(tableA) +
      GLOBAL_CONFIG.CHANGE_COUNT_METHOD.countChanges(tableB)
    val after = GLOBAL_CONFIG.CHANGE_COUNT_METHOD.sumChangeRanges(matchedTuples.map(_.changeRange))
    before-after
  }

}
object TupleSetMatching extends StrictLogging{
}
