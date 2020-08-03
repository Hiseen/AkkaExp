package texera.common.workflow

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import texera.common.workflow.TexeraBreakpointCondition.Condition
import texera.operators.scan.TexeraLocalFileScan
import texera.operators.sink.TexeraAdhocSink
import texera.operators.sleep.TexeraSleepOperator

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
@JsonSubTypes(
  Array(
    new Type(value = classOf[TexeraConditionBreakpoint], name = "ConditionBreakpoint"),
    new Type(value = classOf[TexeraCountBreakpoint], name = "CountBreakpoint")
  )
)
trait TexeraBreakpoint {}

object TexeraBreakpointCondition extends Enumeration {
  type Condition = Value
  val EQ: Condition = Value("=")
  val LT: Condition = Value("<")
  val LE: Condition = Value("<=")
  val GT: Condition = Value(">")
  val GE: Condition = Value(">=")
  val NE: Condition = Value("!=")
  val CONTAINS: Condition = Value("contains")
}

class ConditionType extends TypeReference[TexeraBreakpointCondition.type]

case class TexeraConditionBreakpoint(
    column: Integer,
    @JsonScalaEnumeration(classOf[ConditionType]) condition: TexeraBreakpointCondition.Condition,
    value: String
) extends TexeraBreakpoint

case class TexeraCountBreakpoint(count: Long) extends TexeraBreakpoint
