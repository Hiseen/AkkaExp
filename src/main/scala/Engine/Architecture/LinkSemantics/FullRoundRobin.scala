package Engine.Architecture.LinkSemantics

import Engine.Architecture.DeploySemantics.Layer.ActorLayer
import Engine.Architecture.SendSemantics.DataTransferPolicy.RoundRobinPolicy
import Engine.Architecture.SendSemantics.Routees.{DirectRoutee, FlowControlRoutee}
import Engine.Common.AdvancedMessageSending
import Engine.Common.AmberMessage.WorkerMessage.{UpdateInputLinking, UpdateOutputLinking}
import akka.event.LoggingAdapter
import akka.util.Timeout

import scala.concurrent.ExecutionContext


class FullRoundRobin(from:ActorLayer, to:ActorLayer, batchSize:Int) extends LinkStrategy(from,to,batchSize) {
  override def link()(implicit timeout:Timeout, ec:ExecutionContext, log:LoggingAdapter): Unit = {
    assert(from.isBuilt && to.isBuilt)
    //TODO:change routee type according to the machine address
    from.layer.foreach(x =>
      AdvancedMessageSending.blockingAskWithRetry(x,
        UpdateOutputLinking(new RoundRobinPolicy(batchSize),tag, to.layer.map(y => if(x.path.address.hostPort == y.path.address.hostPort) new DirectRoutee(y) else new FlowControlRoutee(y))),
        10))
  }
}
