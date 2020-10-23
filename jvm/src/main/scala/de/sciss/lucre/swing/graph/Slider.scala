/*
 *  Slider.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2020 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.graph

import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, Graph, IControl, Model}
import de.sciss.lucre.impl.IChangeGeneratorEvent
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{Cursor, IChangeEvent, IExpr, IPull, ITargets, Txn}
import de.sciss.model.Change
import javax.swing.event.{ChangeEvent, ChangeListener}

import scala.concurrent.stm.Ref

object Slider {
  def apply(): Slider = Impl()

  private final class Expanded[T <: Txn[T]](protected val peer: Slider) extends View[T]
    with ComponentHolder[scala.swing.Slider] with ComponentExpandedImpl[T] {

    type C = scala.swing.Slider

    override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
      val minOpt    = ctx.getProperty[Ex[Int]](peer, keyMin  ).map(_.expand[T].value)
      val maxOpt    = ctx.getProperty[Ex[Int]](peer, keyMax  ).map(_.expand[T].value)
      val valueOpt  = ctx.getProperty[Ex[Int]](peer, keyValue).map(_.expand[T].value)

      deferTx {
        val c = new scala.swing.Slider
        minOpt  .foreach(c.min   = _)
        maxOpt  .foreach(c.max   = _)
        valueOpt.foreach(c.value = _)
        component = c
      }

      initProperty(keyMin   , defaultMin  )(component.min   = _)
      initProperty(keyMax   , defaultMax  )(component.max   = _)
      initProperty(keyValue , defaultValue)(component.value = _)

      super.initComponent()
    }

//    override def dispose()(implicit tx: T): Unit = super.dispose()
  }

  private final class ValueExpanded[T <: Txn[T]](ws: View.T[T, scala.swing.Slider], value0: Int)
                                                (implicit protected val targets: ITargets[T], cursor: Cursor[T])
    extends IExpr[T, Int]
      with IChangeGeneratorEvent[T, Int] {

    private[this] val listener = new ChangeListener {
      def stateChanged(e: ChangeEvent): Unit = {
        val c       = ws.component
        val before  = guiValue
        val now     = c.value
        val ch      = Change(before, now)
        if (ch.isSignificant) {
          guiValue    = now
          cursor.step { implicit tx =>
            txValue.set(now)(tx.peer)
            fire(ch)
          }
        }
      }
    }

    private[this] var guiValue: Int = _
    private[this] val txValue = Ref(value0)

    def value(implicit tx: T): Int = txValue.get(tx.peer)

    def changed: IChangeEvent[T, Int] = this

    private[lucre] def pullChange(pull: IPull[T])(implicit tx: T, phase: IPull.Phase): Int =
      pull.resolveExpr(this)

    def init()(implicit tx: T): this.type = {
      deferTx {
        val c = ws.component
        guiValue = c.value
        c.peer.addChangeListener(listener)
      }
      this
    }

    def dispose()(implicit tx: T): Unit = {
      deferTx {
        val c = ws.component
        c.peer.removeChangeListener(listener)
      }
    }
  }

  private final val defaultValue  =  50
  private final val defaultMin    =   0
  private final val defaultMax    = 100
  
  private final val keyValue      = "value"
  private final val keyMin        = "min"
  private final val keyMax        = "max"

  final case class Value(w: Slider) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"Slider$$Value" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      import ctx.targets
      val ws        = w.expand[T]
      val valueOpt  = ctx.getProperty[Ex[Int]](w, keyValue)
      val value0    = valueOpt.fold[Int](defaultValue)(_.expand[T].value)
      import ctx.cursor
      new ValueExpanded[T](ws, value0).init()
    }
  }

  final case class Min(w: Slider) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"Slider$$Min" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMin)
      valueOpt.getOrElse(Const(defaultMin)).expand[T]
    }
  }

  final case class Max(w: Slider) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"Slider$$Max" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMax)
      valueOpt.getOrElse(Const(defaultMax)).expand[T]
    }
  }

  private final case class Impl() extends Slider with ComponentImpl { w =>
    override def productPrefix = "Slider"   // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new Expanded[T](this).initComponent()

    def min: Ex[Int] = Min(this)

    def min_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyMin, x)
    }

    def max: Ex[Int] = Max(this)

    def max_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyMax, x)
    }

    object value extends Model[Int] {
      def apply(): Ex[Int] = Value(w)

      def update(x: Ex[Int]): Unit = {
        val b = Graph.builder
        b.putProperty(w, keyValue, x)
      }
    }
  }
}
trait Slider extends Component {
  type C = scala.swing.Slider

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]

  var min   : Ex[Int]
  var max   : Ex[Int]

  def value : Model[Int]
}