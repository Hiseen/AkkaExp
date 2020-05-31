package Engine.Architecture.SendSemantics.Routees


import Engine.Common.AmberMessage.ControlMessage.{AckOfEndSending, AckWithSequenceNumber, GetSkewMetricsFromFlowControl, Pause, ReportTime, RequireAck, Resume}
import Engine.Common.AmberMessage.WorkerMessage.{DataMessage, EndSending}
import Engine.Common.AmberTag.WorkerTag
import akka.actor.{Actor, ActorRef, Cancellable, PoisonPill, Props, Stash}
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import java.text.SimpleDateFormat
import java.util.Date

object FlowControlSenderActor{
  def props(receiver:ActorRef): Props = Props(new FlowControlSenderActor(receiver))

  final val maxWindowSize = 64
  final val minWindowSize = 2
  final val expendThreshold = 4
  final val sendingTimeout:FiniteDuration = 30.seconds
//  final val activateBackPressureThreshold = 512
//  final val deactivateBackPressureThreshold = 32
  final val logicalTimeGap = 16
  final case class EndSendingTimedOut()
  final case class MessageTimedOut(seq:Long)
}


class FlowControlSenderActor(val receiver:ActorRef) extends Actor with Stash{
  import FlowControlSenderActor._

  implicit val timeout:Timeout = 1.second
  implicit val ec:ExecutionContext = context.dispatcher

  var exactlyCount = 0
  var ssThreshold = 16
  var windowSize = 2
  var maxSentSequenceNumber = 0L
  var handleOfEndSending:(Long,Cancellable) = _
//  var backPressureActivated = false

  val messagesOnTheWay = new mutable.LongMap[(Cancellable,DataMessage)]
  val messagesToBeSent = new mutable.Queue[DataMessage]

  var timeTaken = 0L
  var timeStart = 0L
  var countOfMessageTimedOut:Integer = 0
  var countOfMessagesReceived = 0
  val formatter = new SimpleDateFormat("HH:mm:ss.SSS z")

  override def receive: Receive = {
    case msg:DataMessage =>
      timeStart = System.nanoTime()
      countOfMessagesReceived += 1
      if(messagesOnTheWay.size < windowSize){
        maxSentSequenceNumber = Math.max(maxSentSequenceNumber,msg.sequenceNumber)
        messagesOnTheWay(msg.sequenceNumber) = (context.system.scheduler.scheduleOnce(sendingTimeout,self,MessageTimedOut(msg.sequenceNumber)),msg)
        receiver ! RequireAck(msg)
      }else{
        messagesToBeSent.enqueue(msg)
//        if(messagesToBeSent.size >= activateBackPressureThreshold){
//          //producer produces too much data, the network cannot handle it, activate back pressure
//          backPressureActivated = true
//          context.parent ! ActivateBackPressure
//        }
      }
      timeTaken += System.nanoTime()-timeStart
    case msg:EndSending =>
      timeStart = System.nanoTime()
      //always send end-sending message regardless the message queue size
      handleOfEndSending = (msg.sequenceNumber,context.system.scheduler.scheduleOnce(sendingTimeout,self,EndSendingTimedOut))
      receiver ! RequireAck(msg)
      timeTaken += System.nanoTime()-timeStart
    case AckWithSequenceNumber(seq) =>
      timeStart = System.nanoTime()
      if(messagesOnTheWay.contains(seq)) {
        messagesOnTheWay(seq)._1.cancel()
        messagesOnTheWay.remove(seq)
        if (maxSentSequenceNumber-seq < logicalTimeGap) {
          if (windowSize < ssThreshold) {
            windowSize = Math.min(windowSize * 2, ssThreshold)
          } else {
            windowSize += 1
          }
        } else {
          ssThreshold /= 2
          windowSize = Math.max(minWindowSize,Math.min(ssThreshold,maxWindowSize))
        }
        if(messagesOnTheWay.size < windowSize && messagesToBeSent.nonEmpty){
          val msg = messagesToBeSent.dequeue()
          maxSentSequenceNumber = Math.max(maxSentSequenceNumber,msg.sequenceNumber)
          messagesOnTheWay(msg.sequenceNumber) = (context.system.scheduler.scheduleOnce(sendingTimeout,self,MessageTimedOut(msg.sequenceNumber)),msg)
          receiver ! RequireAck(msg)
        }
      }
      timeTaken += System.nanoTime()-timeStart
    case AckOfEndSending =>
      if(handleOfEndSending != null){
        handleOfEndSending._2.cancel()
        handleOfEndSending = null
      }
    case EndSendingTimedOut =>
      if(handleOfEndSending != null){
        handleOfEndSending = (handleOfEndSending._1,context.system.scheduler.scheduleOnce(sendingTimeout,self,EndSendingTimedOut))
        receiver ! RequireAck(EndSending(handleOfEndSending._1))
      }
    case MessageTimedOut(seq) =>
      timeStart = System.nanoTime()
      if(messagesOnTheWay.contains(seq)){
        countOfMessageTimedOut += 1
        //resend the data message
        val msg = messagesOnTheWay(seq)._2
        messagesOnTheWay(msg.sequenceNumber) = (context.system.scheduler.scheduleOnce(sendingTimeout,self,MessageTimedOut(msg.sequenceNumber)),msg)
        receiver ! RequireAck(msg)
      }
      timeTaken += System.nanoTime()-timeStart
    case Resume =>
    case Pause => context.become(paused)
    case ReportTime(tag:WorkerTag, count:Integer) =>
      println(s"${count} FLOW sending to ${tag.getGlobalIdentity} time ${timeTaken/1000000}, messagesReceivedTillNow ${countOfMessagesReceived}, sent ${countOfMessagesReceived - messagesToBeSent.size} at ${formatter.format(new Date(System.currentTimeMillis()))}")

    case GetSkewMetricsFromFlowControl =>
      sender ! (receiver, countOfMessagesReceived, messagesToBeSent.size)
  }

  final def paused:Receive ={
    case Pause =>
    case Resume =>
      context.become(receive)
      unstashAll()
    case msg => stash()
  }
}
