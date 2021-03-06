package de.hpi.dataset_versioning.db_synthesis.graph.association

import de.hpi.dataset_versioning.io.IOService

object AssociationMergeabilityGraphCreationMain extends App {

  IOService.socrataDir = args(0)
  val subdomain = args(1)
  val associationMergeabilityGraph = AssociationMergeabilityGraph.readFromSingleEdgeFiles(subdomain)
  associationMergeabilityGraph.writeToStandardFile(subdomain)


}
