package Engine.Architecture.DeploySemantics.Layer

import Engine.Architecture.DeploySemantics.DeployStrategy.DeployStrategy
import Engine.Architecture.DeploySemantics.DeploymentFilter.DeploymentFilter
import Engine.Architecture.Worker.Generator
import Engine.Common.AmberTag.{LayerTag, WorkerTag}
import Engine.Common.TupleProducer
import Engine.Operators.OperatorMetadata
import akka.actor.{ActorContext, ActorRef, Address, Deploy}
import akka.remote.RemoteScope

class GeneratorWorkerLayer(tag:LayerTag, val metadata: Int => TupleProducer, _numWorkers:Int, df: DeploymentFilter, ds: DeployStrategy ) extends ActorLayer(tag,_numWorkers,df,ds) {

  override def clone(): AnyRef = {
    val res = new GeneratorWorkerLayer(tag,metadata,numWorkers,df,ds)
    res.layer = layer.clone()
    res
  }

  def build(prev:Array[(OperatorMetadata,ActorLayer)],all:Array[Address])(implicit context:ActorContext): Unit ={
    deployStrategy.initialize(deploymentFilter.filter(prev, all, context.self.path.address))
    layer = new Array[ActorRef](numWorkers)
    var idx = 0
    for(i <- 0 until numWorkers){
      try{
        val workerTag = WorkerTag(tag,i)
        layer(idx)=context.actorOf(Generator.props(metadata(i),workerTag).withDeploy(Deploy(scope = RemoteScope(deployStrategy.next()))))
        idx += 1
      }
      catch{
        case e:Exception => println(e)
      }
    }
    if(idx != numWorkers-1){
      layer = layer.take(idx)
      numWorkers = idx+1
    }

  }

}
