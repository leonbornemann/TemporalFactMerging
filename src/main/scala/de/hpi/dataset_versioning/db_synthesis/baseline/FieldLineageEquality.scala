package de.hpi.dataset_versioning.db_synthesis.baseline

import de.hpi.dataset_versioning.data.change.FieldLineageCompatibility
import de.hpi.dataset_versioning.data.change.temporal_tables.FieldLineageReference

class FieldLineageEquality() extends FieldLineageCompatibility{

  override def isCompatible(fl1: FieldLineageReference, fl2: FieldLineageReference): Boolean = {
    val values1 = fl1.lineage
    val values2 = fl2.lineage
    values1.lineage == values2.lineage
  }
}
