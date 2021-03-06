package de.hpi.dataset_versioning.data.simplified

import de.hpi.dataset_versioning.io.IOService

import java.time.LocalDate

object MinimalHistoryCreationMain extends App {

  IOService.socrataDir = args(0)
  val beginDate = LocalDate.parse(args(1))
  val endDate = LocalDate.parse(args(2))
  IOService.extractMinimalHistoryInRange(beginDate,endDate)


}
