package de.hpi.dataset_versioning.db_synthesis.optimization

import de.hpi.dataset_versioning.data.{JsonReadable, JsonWritable}
import de.hpi.dataset_versioning.db_synthesis.baseline.matching.{IDBasedTupleReference, TupleReference}
import de.hpi.dataset_versioning.io.DBSynthesis_IOService.{EVALUATION_RESULT_DIR, FIELD_MERGE_RESULT_DIR, createParentDirs}

import java.io.File

case class TupleMerge(clique:Set[IDBasedTupleReference],score:Double) extends JsonWritable[TupleMerge]{

}

object TupleMerge extends JsonReadable[TupleMerge] {
  def loadIncorrectMerges(methodName: String) = fromJsonObjectPerLineFile(getIncorrectMergeFile(methodName).getAbsolutePath)

  def loadCorrectMerges(methodName: String) = fromJsonObjectPerLineFile(getCorrectMergeFile(methodName).getAbsolutePath)


  def getCorrectMergeFile(methodName: String) = createParentDirs(new File(EVALUATION_RESULT_DIR(methodName) + "/correctMerges.json"))
  def getIncorrectMergeFile(methodName: String) = createParentDirs(new File(EVALUATION_RESULT_DIR(methodName) + "/incorrectMerges.json"))


  def getStandardObjectPerLineFiles(methodName:String) = {
    createParentDirs(new File(FIELD_MERGE_RESULT_DIR + s"/$methodName/")).listFiles()
  }

  def getStandardJsonObjectPerLineFile(componentFileName: String, methodName:String) = {
    createParentDirs(new File(FIELD_MERGE_RESULT_DIR + s"/$methodName/" + componentFileName + ".json"))
  }
}
