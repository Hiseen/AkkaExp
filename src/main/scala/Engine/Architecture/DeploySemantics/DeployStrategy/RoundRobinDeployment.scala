package Engine.Architecture.DeploySemantics.DeployStrategy
import akka.actor.Address


object RoundRobinDeployment{
  def apply() = new RoundRobinDeployment()
}



class RoundRobinDeployment extends DeployStrategy {
  var available:Array[Address] = _
  var index = 0
  override def initialize(available: Array[Address]): Unit = {
    this.available = available
    println("RoundRobin has "+available.length+" nodes")
  }

  override def next(): Address = {
    val i = index
    index = (index+1)%available.length
    available(i)
  }
}
