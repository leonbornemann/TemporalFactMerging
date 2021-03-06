package de.hpi.dataset_versioning.db_synthesis.baseline.matching.field_graph

import com.typesafe.scalalogging.StrictLogging
import de.hpi.dataset_versioning.db_synthesis.baseline.index.BipartiteTupleIndex
import de.hpi.dataset_versioning.db_synthesis.baseline.matching.TupleReference

import java.time.LocalDate
import de.hpi.dataset_versioning.util.RuntimeMeasurementUtil.executionTimeInSeconds


class BipartiteFieldLineageMatchGraph[A](tuplesLeft: IndexedSeq[TupleReference[A]],tuplesRight: IndexedSeq[TupleReference[A]]) extends FieldLineageGraph[A] with StrictLogging{

  val detailedLogging:Boolean = false
  var totalIndexTime:Double = 0.0
  var totalMatchExecutionTime = 0.0
  var lastIndexTimeReport = 0.0
  var lastMatchTimeReport = 0.0
  val programStartInMs = System.currentTimeMillis()
  var computedMatches = 0
  init()

  def init() = {
    val (index,indexTime) = executionTimeInSeconds(new BipartiteTupleIndex[A](tuplesLeft,tuplesRight,IndexedSeq(),IndexedSeq(),true))
    totalIndexTime += indexTime
    if(detailedLogging){
      val topLevelPairwiseComputations = index.getBipartiteTupleGroupIterator().toIndexedSeq.map(_.totalComputationsIfPairwise)
      logger.debug(s"Constructed Top-Level Index with ${topLevelPairwiseComputations.size} top-level nodes, total possible match computations: ${topLevelPairwiseComputations.map(_.toLong).sum}")
    }
    buildGraph(tuplesLeft,tuplesRight,index)
    reportRunTimes()
  }

  def productTooBig(size: Int, size1: Int): Boolean = {
    size*size1>50
  }

  def buildGraph(originalInputLeft:IndexedSeq[TupleReference[A]], originalInputRight:IndexedSeq[TupleReference[A]], index: BipartiteTupleIndex[A]):Unit = {
    if(!index.indexFailed){
      index.getBipartiteTupleGroupIterator().foreach{case g => {
        val tuplesLeft = g.tuplesLeft
        val tuplesRight = g.tuplesRight
        val wildcardTuplesLeft = g.wildcardTuplesLeft
        val wildCardTuplesRight = g.wildcardTuplesRight
        assert(g.tuplesLeft.toSet.intersect(wildcardTuplesLeft.toSet).isEmpty && g.tuplesRight.toSet.intersect(wildCardTuplesRight.toSet).isEmpty)
        buildGraphRecursively(g.chosenTimestamps.toIndexedSeq,g.valuesAtTimestamps, tuplesLeft, tuplesRight)
        //TODO: process Wildcards to others:
        buildGraphRecursively(g.chosenTimestamps.toIndexedSeq,g.valuesAtTimestamps,wildcardTuplesLeft,g.tuplesRight)
        buildGraphRecursively(g.chosenTimestamps.toIndexedSeq,g.valuesAtTimestamps,g.tuplesLeft,wildCardTuplesRight)
        //Wildcards to Wildcards:
      }}
      buildGraphRecursively(index.parentTimestamps,index.parentKeyValues,index.wildcardsLeft,index.wildcardsRight)
    } else {
      val (_,time) = executionTimeInSeconds(doPairwiseMatching(originalInputLeft,originalInputRight))
      totalMatchExecutionTime += time
    }
  }

  def reportRunTimes() = {
    val timeNow = System.currentTimeMillis()
    logger.debug("----------------------------------------------------------------------------------------------")
    logger.debug(s"Total Index Time: ${totalIndexTime}s")
    logger.debug(s"Total Match Execution Time: ${totalMatchExecutionTime}s")
    val timePassedInSeconds = (timeNow - programStartInMs) / 1000.0
    logger.debug(s"Total Time since constructor: ${timePassedInSeconds}s")
    logger.debug(s"Unaccounted Time: ${timePassedInSeconds - totalMatchExecutionTime - totalIndexTime}s")
    logger.debug(s"Computed Matches: $computedMatches (${computedMatches / timePassedInSeconds} matches per second)")
    logger.debug("----------------------------------------------------------------------------------------------")
    lastMatchTimeReport = totalMatchExecutionTime
    lastIndexTimeReport = totalIndexTime
  }

  def indexTimeReportDue: Boolean = totalIndexTime - lastIndexTimeReport > 10
  def matchTimeReportDue: Boolean = totalMatchExecutionTime - lastMatchTimeReport > 10

  private def buildGraphRecursively(parentTimestamps:IndexedSeq[LocalDate], parentValues:IndexedSeq[A], tuplesLeft: IndexedSeq[TupleReference[A]], tuplesRight: IndexedSeq[TupleReference[A]]) = {
    if (productTooBig(tuplesLeft.size, tuplesRight.size)) {
      //further index this: new Index
      val (newIndexForSubNode,newIndexTime) = executionTimeInSeconds(new BipartiteTupleIndex[A](tuplesLeft, tuplesRight, parentTimestamps, parentValues, true))
      totalIndexTime += newIndexTime
      if(indexTimeReportDue && detailedLogging){
        reportRunTimes()
      }
      buildGraph(tuplesLeft, tuplesRight, newIndexForSubNode)
    } else {
      val (_,time) = executionTimeInSeconds(doPairwiseMatching(tuplesLeft, tuplesRight))
      totalMatchExecutionTime += time
      if(matchTimeReportDue && detailedLogging){
        reportRunTimes()
      }
    }
  }

  private def doPairwiseMatching(tuplesLeft: IndexedSeq[TupleReference[A]], tuplesRight:IndexedSeq[TupleReference[A]]) = {
    //we construct a graph as an adjacency list:
    //pairwise matching to find out the edge-weights:
    if(tuplesLeft.size>0 && tuplesRight.size>0) {
      for (i <- 0 until tuplesLeft.size) {
        for (j <- 0 until tuplesRight.size) {
          val ref1 = tuplesLeft(i)
          val ref2 = tuplesRight(j)
          val edge = getTupleMatchOption(ref1, ref2)
          computedMatches +=1
          if (edge.isDefined)
            edges.add(edge.get)
        }
      }
    }
  }

}
