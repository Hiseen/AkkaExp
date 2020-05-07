package Engine.Common

import scala.concurrent.duration._

object Constants {
  val defaultBatchSize = 400
  var defaultNumWorkers = 0
  var dataset = 0
  var masterNodeAddr:String = null

  var numWorkerPerNode = 2
  var dataVolumePerNode = 10
  var defaultTau: FiniteDuration = 10.milliseconds
}
