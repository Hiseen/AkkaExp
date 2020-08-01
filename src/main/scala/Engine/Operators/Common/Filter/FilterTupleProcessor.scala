package Engine.Operators.Common.Filter

import Engine.Common.AmberTag.LayerTag
import Engine.Common.AmberTuple.Tuple
import Engine.Common.TupleProcessor

import scala.Function1
import scala.reflect.ClassTag

class FilterTupleProcessor(
  val filterFunc: (Tuple => Boolean) with java.io.Serializable
) extends TupleProcessor {
  private var tuple: Tuple = _
  private var nextFlag = false

  override def accept(tuple: Tuple): Unit = {
    this.nextFlag = filterFunc.apply(tuple)
    if (this.nextFlag) this.tuple = tuple
    else this.tuple = null
  }

  override def onUpstreamChanged(from: LayerTag): Unit = {}

  override def onUpstreamExhausted(from: LayerTag): Unit = {}

  override def noMore(): Unit = {}

  override def initialize(): Unit = {}

  override def hasNext: Boolean = nextFlag

  override def next(): Tuple = {
    nextFlag = false
    tuple
  }

  override def dispose(): Unit = {}
}
