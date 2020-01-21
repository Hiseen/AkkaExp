package Engine.Architecture.SendSemantics.Routees

import Engine.Common.AmberMessage.WorkerMessage.{DataMessage, EndSending}
import Engine.Common.AmberTag.LinkTag
import akka.actor.{Actor, ActorContext, ActorRef}
import akka.event.LoggingAdapter
import akka.util.Timeout

import scala.concurrent.ExecutionContext

abstract class BaseRoutee(val receiver:ActorRef) extends Serializable {

  def initialize(tag:LinkTag)(implicit ac:ActorContext, sender: ActorRef, timeout:Timeout, ec:ExecutionContext, log:LoggingAdapter)

  def schedule(msg:DataMessage)(implicit sender: ActorRef)

  def pause()

  def resume()

  def schedule(msg:EndSending)(implicit sender: ActorRef)

  def dispose()
}
