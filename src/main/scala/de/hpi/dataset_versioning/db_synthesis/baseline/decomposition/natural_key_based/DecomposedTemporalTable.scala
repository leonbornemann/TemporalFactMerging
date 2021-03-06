package de.hpi.dataset_versioning.db_synthesis.baseline.decomposition.natural_key_based

import de.hpi.dataset_versioning.data.change.temporal_tables.attribute.AttributeLineage
import de.hpi.dataset_versioning.data.change.temporal_tables.time.TimeIntervalSequence
import de.hpi.dataset_versioning.data.simplified.Attribute
import de.hpi.dataset_versioning.db_synthesis.baseline.decomposition.DecomposedTemporalTableIdentifier

import java.time.LocalDate
import scala.collection.mutable

@SerialVersionUID(3L)
case class DecomposedTemporalTable(id: DecomposedTemporalTableIdentifier,
                                   containedAttrLineages: mutable.ArrayBuffer[AttributeLineage],
                                   originalFDLHS: collection.Set[AttributeLineage],
                                   primaryKeyByVersion: Map[LocalDate,collection.Set[Attribute]],
                                   referencedTables:mutable.HashSet[DecomposedTemporalTableIdentifier]) extends Serializable{
  def writeToStandardFile() = ???


  def informativeTableName: String = id.compositeID + "(" + containedAttrLineages.map(_.lastName).mkString(",") + ")"

  private var activeTime:Option[TimeIntervalSequence] = None

  def getActiveTime = {
    if(activeTime.isDefined) activeTime.get
    else {
      activeTime = Some(containedAttrLineages.map(_.activeTimeIntervals).reduce((a,b) => a.union(b)))
      activeTime.get
    }
  }

  def nonKeyAttributeLineages = {
    val key = primaryKeyByVersion.flatMap(_._2.map(_.id)).toSet
    containedAttrLineages.filter(al => !key.contains(al.attrId)).toSet
  }

  def primaryKey = {
    val key = primaryKeyByVersion.flatMap(_._2.map(_.id)).toSet
    containedAttrLineages.filter(al => key.contains(al.attrId)).toSet
  }


  def isAssociation = id.associationID.isDefined

  def getOriginalDTTBeforeAssociationDecompoistion = {
    if(!isAssociation) {
      throw new AssertionError("Can't get original table of non-association. This is already an original table")
    }
    DecomposedTemporalTableIdentifier(id.subdomain,id.viewID,id.bcnfID,None)
  }

  def furtherDecomposeToAssociations = {
    //assert that every attribute lineage is either always in the primary key or not:
    val byID = containedAttrLineages.map(al => (al.attrId,al)).toMap
    val pkAttrIds = primaryKeyByVersion.values.flatMap(_.map(_.id)).toSet
    val pkAttrIsAlwaysPKIFItExists =  primaryKeyByVersion.keySet.forall(v => {
      pkAttrIds.filter(alID => byID(alID).valueAt(v)._2.exists)
        .forall(alID => primaryKeyByVersion(v).exists(a => a.id==alID))
    })
    assert(pkAttrIsAlwaysPKIFItExists)
    val pkAttributeLineages = containedAttrLineages.filter(al => pkAttrIds.contains(al.attrId))
    val nonPkAttrs = containedAttrLineages.filter(!pkAttributeLineages.contains(_))
    //TODO: we need to add all the references to other more decomposed tables, but what happens to the old ones?
    //I guess we delete them - we still have the connection to the old (non-association table) table to get the connections with that table
    val associationTableIds = mutable.HashSet() ++ ((0 until nonPkAttrs.size)
      .map(i => DecomposedTemporalTableIdentifier(id.subdomain,id.viewID,id.bcnfID,Some(i))))
    nonPkAttrs.zip(associationTableIds).map{case (rhs,associationTableID) => new DecomposedTemporalTable(associationTableID,
      pkAttributeLineages ++ IndexedSeq(rhs),
      originalFDLHS,
      primaryKeyByVersion,
      associationTableIds.filter(_!= associationTableID))} //we refer to everything that is not ourself
  }

  def compositeID: String = id.compositeID

  def schemaAt(v: LocalDate) = containedAttrLineages
    .withFilter(_.valueAt(v)._2.exists)
    .map(_.valueAt(v)._2.attr.get)
    .sortBy(_.position.get)


//  def writeToStandardFile() = {
//    val file = DBSynthesis_IOService.getDecomposedTemporalTableFile(id)
//    val helper = DecomposedTemporalTableHelper(id,
//      containedAttrLineages.map(AttributeLineageWithHashMap.from(_)),
//      originalFDLHS.map(AttributeLineageWithHashMap.from(_)),
//      primaryKeyByVersion,
//      referencedTables)
//    helper.toJsonFile(file)
//  }

  def allActiveVersions = containedAttrLineages.flatMap(_.lineage.keySet).toSet

  def getSchemaString = id.viewID + "_" +id.bcnfID + "(" +
    primaryKey.toIndexedSeq.map(pk => pk.lastName).sorted.mkString(",") + "  ->  " +
    nonKeyAttributeLineages.toIndexedSeq.map(_.lastName).sorted.mkString(",") + ")"

  def getSchemaStringWithIds: String = id.viewID + "_" +id.bcnfID + "(" +
    primaryKey.toIndexedSeq.map(pk => pk.lastName + s"[${pk.attrId}]").sorted.mkString(",") + "  ->  " +
    nonKeyAttributeLineages.toIndexedSeq.map(nk => nk.lastName  + s"[${nk.attrId}]").sorted.mkString(",") + ")"

}

object DecomposedTemporalTable {

//  def loadAllDecomposedTemporalTables(subdomain: String, originalID: String) = {
//    val dir = DBSynthesis_IOService.getOldDecomposedTemporalTableDir(subdomain,originalID)
//    val ids = dir.listFiles().map(f => DecomposedTemporalTableIdentifier.fromFilename(f.getName))
//    ids.map(id => load(id))
//  }
//
//  def load(id:DecomposedTemporalTableIdentifier) = {
//    val file = DBSynthesis_IOService.getOldDecomposedTemporalTableFile(id)
//    val helper = DecomposedTemporalTableHelper.fromJsonFile(file.getAbsolutePath)
//    helper.toDecomposedTemporalTable
//  }

}
