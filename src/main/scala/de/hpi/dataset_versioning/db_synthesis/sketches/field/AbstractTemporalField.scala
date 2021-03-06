package de.hpi.dataset_versioning.db_synthesis.sketches.field

import java.time.LocalDate
import de.hpi.dataset_versioning.data.change.temporal_tables.time.{TimeInterval, TimeIntervalSequence}
import de.hpi.dataset_versioning.data.change.temporal_tables.tuple.ValueLineage
import de.hpi.dataset_versioning.db_synthesis.baseline.matching.TupleReference
import de.hpi.dataset_versioning.db_synthesis.change_counting.surrogate_based.FieldChangeCounter
import de.hpi.dataset_versioning.db_synthesis.sketches.field.AbstractTemporalField.MUTUAL_INFORMATION

import scala.collection.mutable

@SerialVersionUID(3L)
abstract class AbstractTemporalField[A] extends TemporalFieldTrait[A] {

  override def countChanges(changeCounter: FieldChangeCounter): (Int,Int) = {
    changeCounter.countFieldChanges(this)
  }

  override def toIntervalRepresentation:mutable.TreeMap[TimeInterval,A] = {
    val asLineage = getValueLineage.toIndexedSeq
    mutable.TreeMap[TimeInterval,A]() ++ (0 until asLineage.size).map( i=> {
      val (ts,value) = asLineage(i)
      if(i==asLineage.size-1)
        (TimeInterval(ts,None),value)
      else
        (TimeInterval(ts,Some(asLineage(i+1)._1.minusDays(1))),value)
    })
  }

  def valuesAreCompatible(myValue: A, otherValue: A):Boolean

  def getCompatibleValue(myValue: A, otherValue: A): A

  def getOverlapInterval(a: (TimeInterval, A), b: (TimeInterval, A)): (TimeInterval, A) = {
    assert(a._1.begin==b._1.begin)
    assert(valuesAreCompatible(a._2,b._2))
    val earliestEnd = Seq(a._1.endOrMax,b._1.endOrMax).minBy(_.toEpochDay)
    val endTime = if(earliestEnd==LocalDate.MAX) None else Some(earliestEnd)
    (TimeInterval(a._1.begin,endTime),getCompatibleValue(a._2,b._2))
  }

  def fromValueLineage[V<:TemporalFieldTrait[A]](lineage: ValueLineage):V

  def fromTimestampToValue[V<:TemporalFieldTrait[A]](asTree: mutable.TreeMap[LocalDate, A]):V

  override def tryMergeWithConsistent[V <: TemporalFieldTrait[A]](other: V): Option[V] = {
    val myLineage = this.toIntervalRepresentation.toBuffer
    val otherLineage = other.toIntervalRepresentation.toBuffer
    if(myLineage.isEmpty){
      return if(otherLineage.isEmpty) Some(fromValueLineage[V](ValueLineage())) else None
    } else if(otherLineage.isEmpty){
      return if(myLineage.isEmpty) Some(fromValueLineage[V](ValueLineage())) else None
    }
    val newLineage = mutable.ArrayBuffer[(TimeInterval,A)]()
    var myIndex = 0
    var otherIndex = 0
    var incompatible = false
    while((myIndex < myLineage.size || otherIndex < otherLineage.size ) && !incompatible) {
      assert(myIndex < myLineage.size && otherIndex < otherLineage.size)
      val (myInterval,myValue) = myLineage(myIndex)
      val (otherInterval,otherValue) = otherLineage(otherIndex)
      assert(myInterval.begin == otherInterval.begin)
      var toAppend:(TimeInterval,A) = null
      if(myInterval==otherInterval){
        if(!valuesAreCompatible(myValue,otherValue)){
          incompatible=true
        } else {
          toAppend = (myInterval, getCompatibleValue(myValue, otherValue))
          myIndex += 1
          otherIndex += 1
        }
      } else if(myInterval<otherInterval){
        if(!valuesAreCompatible(myLineage(myIndex)._2,otherLineage(otherIndex)._2)){
          incompatible = true
        } else {
          toAppend = getOverlapInterval(myLineage(myIndex), otherLineage(otherIndex))
          //replace old interval with newer interval with begin set to myInterval.end+1
          otherLineage(otherIndex) = (TimeInterval(myInterval.end.get.plusDays(1), otherInterval.`end`), otherValue)
          myIndex += 1
        }
      } else{
        assert(otherInterval<myInterval)
        if(!valuesAreCompatible(myLineage(myIndex)._2,otherLineage(otherIndex)._2)){
          incompatible = true
        } else {
          toAppend = getOverlapInterval(myLineage(myIndex), otherLineage(otherIndex))
          myLineage(myIndex) = (TimeInterval(otherInterval.end.get.plusDays(1), myInterval.`end`), myValue)
          otherIndex += 1
        }
      }
      if(!incompatible) {
        if (!newLineage.isEmpty && newLineage.last._2 == toAppend._2) {
          //we replace the old interval by a longer one
          newLineage(newLineage.size - 1) = (TimeInterval(newLineage.last._1.begin, toAppend._1.`end`), toAppend._2)
        } else {
          //we simply append the new interval:
          newLineage += toAppend
        }
      }
    }
    if(incompatible)
      None
    else {
      val asTree = mutable.TreeMap[LocalDate, A]() ++ newLineage.map(t => (t._1.begin, t._2))
      Some(fromTimestampToValue[V](asTree))
    }
  }

  override def mergeWithConsistent[V<:TemporalFieldTrait[A]](other: V):V = {
    tryMergeWithConsistent(other).get
  }

  override def append[V<:TemporalFieldTrait[A]](y: V): V = {
    assert(lastTimestamp.isBefore(y.firstTimestamp))
    fromTimestampToValue(this.getValueLineage ++ y.getValueLineage)
  }

  def valuesInInterval(ti: TimeInterval): IterableOnce[(TimeInterval,A)]

  //gets the hash values at the specified time-intervals, substituting missing values with the hash-value of ReservedChangeValues.NOT_EXISTANT_ROW
  override def valuesAt(timeToExtract: TimeIntervalSequence): Map[TimeInterval, A] = {
    val a = timeToExtract.sortedTimeIntervals.flatMap(ti => {
      valuesInInterval(ti)
    }).toMap
    a
  }
}
object AbstractTemporalField{

  def MUTUAL_INFORMATION[A](tr1: TupleReference[A], tr2: TupleReference[A]) = {
    tr1.getDataTuple.head.mutualInformation(tr2.getDataTuple.head)
  }

  def IMPROVED_SCORE[A](tr1: TupleReference[A], tr2: TupleReference[A]) = {
    tr1.getDataTuple.head.newScore(tr2.getDataTuple.head)
  }


  def ENTROPY_REDUCTION[A](tr1: TupleReference[A], tr2: TupleReference[A]) = {
    ENTROPY_REDUCTION_SET(Set(tr1,tr2))
  }

  def ENTROPY_REDUCTION_SET[A](references: Set[TupleReference[A]]) = {
    val asFields = references.map(_.getDataTuple.head)
    ENTROPY_REDUCTION_SET_FIELD(asFields)
  }

  def ENTROPY_REDUCTION_SET_FIELD[A](fields:Set[TemporalFieldTrait[A]]) = {
    val merged = fields.toIndexedSeq
      .reduce((a,b) => a.mergeWithConsistent(b)).getEntropy()
    val prior = fields.toIndexedSeq.map(_.getEntropy()).sum
    if(prior.isNaN || merged.isNaN)
      println("What? - we found NaN")
    prior - merged
  }

  def mergeAll[A](refs:Seq[TupleReference[A]]):TemporalFieldTrait[A] = {
    if(refs.size==1)
      refs.head.getDataTuple.head
    else {
      val toMerge = refs.tail.map(tr => tr.getDataTuple)
      assert(toMerge.head.size == 1)
      var res = refs.head.getDataTuple.head
      (1 until toMerge.size).foreach(i => {
        res = res.mergeWithConsistent(toMerge(i).head)
      })
      res
    }
  }
}
