package texera.common.workflow

import Engine.Architecture.Breakpoint.GlobalBreakpoint.{ConditionalGlobalBreakpoint, CountGlobalBreakpoint}
import Engine.Architecture.Controller.Workflow
import Engine.Common.AmberMessage.ControllerMessage.PassBreakpointTo
import Engine.Common.AmberTag.OperatorTag
import Engine.Common.AmberTuple.Tuple
import Engine.Operators.OperatorMetadata
import akka.actor.ActorRef
import texera.common.{TexeraConstraintViolation, TexeraContext}

import scala.collection.mutable

class TexeraWorkflowCompiler(val texeraWorkflow: TexeraWorkflow, val context: TexeraContext) {

  def init(): Unit = {
    this.texeraWorkflow.operators.foreach(initOperator)
  }

  def initOperator(operator: TexeraOperator): Unit = {
    operator.context = context
  }

  def validate: Map[String, Set[TexeraConstraintViolation]] =
    this.texeraWorkflow.operators.map(o => {
      o.operatorID -> {
        o.validate()
      }
    }).toMap.filter(pair => pair._2.nonEmpty)

  def amberWorkflow: Workflow = {
    val amberOperators: mutable.Map[OperatorTag, OperatorMetadata] = mutable.Map()
    texeraWorkflow.operators.foreach(o => {
      val amberOperator = o.amberOperator
      amberOperators.put(amberOperator.tag, amberOperator)
    })

    val outLinks: mutable.Map[OperatorTag, mutable.Set[OperatorTag]] = mutable.Map()
    texeraWorkflow.links.foreach(link => {
      val origin = OperatorTag(this.context.workflowID, link.origin)
      val dest = OperatorTag(this.context.workflowID, link.destination)
      val destSet = outLinks.getOrElse(origin, mutable.Set())
      destSet.add(dest)
      outLinks.update(origin, destSet)
    })

    val outLinksImmutableValue: mutable.Map[OperatorTag, Set[OperatorTag]] = mutable.Map()
    outLinks.foreach(entry => {
      outLinksImmutableValue.update(entry._1, entry._2.toSet)
    })
    val outLinksImmutable: Map[OperatorTag, Set[OperatorTag]] = outLinksImmutableValue.toMap

    new Workflow(amberOperators, outLinksImmutable)
  }

  def initializeBreakpoint(controller: ActorRef): Unit = {
    for (pair <- this.texeraWorkflow.breakpoints) {
      val operatorID = pair.operatorID
      val breakpointID = "breakpoint-" + operatorID
      val breakpoint = pair.breakpoint
      breakpoint match {
        case conditionBp: TexeraConditionBreakpoint =>
          val column = this.context.fieldIndexMapping(conditionBp.column);
          val predicate: Tuple => Boolean = conditionBp.condition match {
            case TexeraBreakpointCondition.EQ =>
              tuple => {
                tuple.get(column).toString.trim == conditionBp.value
              }
            case TexeraBreakpointCondition.LT =>
              tuple => tuple.get(column).toString.trim < conditionBp.value
            case TexeraBreakpointCondition.LE =>
              tuple => tuple.get(column).toString.trim <= conditionBp.value
            case TexeraBreakpointCondition.GT =>
              tuple => tuple.get(column).toString.trim > conditionBp.value
            case TexeraBreakpointCondition.GE =>
              tuple => tuple.get(column).toString.trim >= conditionBp.value
            case TexeraBreakpointCondition.NE =>
              tuple => tuple.get(column).toString.trim != conditionBp.value
            case texera.common.workflow.TexeraBreakpointCondition.CONTAINS =>
              tuple => tuple.get(column).toString.trim.contains(conditionBp.value)
          }
          controller ! PassBreakpointTo(operatorID, new ConditionalGlobalBreakpoint(breakpointID, predicate))
        case countBp: TexeraCountBreakpoint =>
          controller ! PassBreakpointTo(operatorID, new CountGlobalBreakpoint("breakpointID", countBp.count))
      }
    }

  }
}
