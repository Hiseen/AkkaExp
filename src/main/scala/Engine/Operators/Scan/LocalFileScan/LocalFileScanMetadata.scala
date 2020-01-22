package Engine.Operators.Scan.LocalFileScan

import java.io.File

import Engine.Architecture.Breakpoint.GlobalBreakpoint.GlobalBreakpoint
import Engine.Architecture.DeploySemantics.DeployStrategy.{RandomDeployment, RoundRobinDeployment}
import Engine.Architecture.DeploySemantics.DeploymentFilter.UseAll
import Engine.Architecture.DeploySemantics.Layer.{ActorLayer, GeneratorWorkerLayer}
import Engine.Architecture.LinkSemantics.LinkStrategy
import Engine.Architecture.Worker.WorkerState
import Engine.Common.AmberTag.{AmberTag, LayerTag, OperatorTag}
import Engine.Common.TableMetadata
import Engine.Operators.OperatorMetadata
import Engine.Operators.Scan.FileScanMetadata
import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.util.Timeout

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext

class LocalFileScanMetadata(tag:OperatorTag, numWorkers:Int, filePath:String, delimiter: String, indicesToKeep:Array[Int], tableMetadata: TableMetadata) extends FileScanMetadata(tag,numWorkers,filePath,delimiter,indicesToKeep,tableMetadata) {
  override val totalBytes: Long = new File(filePath).length()
  override lazy val topology: Topology = {
    new Topology(Array(new GeneratorWorkerLayer(
      LayerTag(tag,"main"),
      i =>{
        val endOffset = if(i != numWorkers-1) totalBytes/numWorkers*(i+1) else totalBytes
        new LocalFileScanTupleProducer(filePath,totalBytes/numWorkers*i,endOffset,delimiter,indicesToKeep,tableMetadata)
      },
      numWorkers,
      UseAll(), // it's source operator
      RoundRobinDeployment())),
      Array(),
      Map())
  }


  override def assignBreakpoint(topology: Array[ActorLayer], states: mutable.AnyRefMap[ActorRef, WorkerState.Value], breakpoint: GlobalBreakpoint)(implicit timeout:Timeout, ec:ExecutionContext, log:LoggingAdapter): Unit = {
    breakpoint.partition(topology(0).layer.filter(states(_)!= WorkerState.Completed))
  }
}
