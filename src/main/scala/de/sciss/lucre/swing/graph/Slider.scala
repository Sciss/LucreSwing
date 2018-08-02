/*
 *  Slider.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2018 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing
package graph

import de.sciss.lucre.event.impl.IGenerator
import de.sciss.lucre.event.{IEvent, IPull, ITargets}
import de.sciss.lucre.expr.graph.Constant
import de.sciss.lucre.expr.{Ex, IExpr, Model}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.model.Change
import javax.swing.event.{ChangeEvent, ChangeListener}

import scala.concurrent.stm.Ref

object Slider {
  def apply(): Slider = Impl()

  private final class Expanded[S <: Sys[S]](protected val w: Slider) extends View[S]
    with ComponentHolder[scala.swing.Slider] with ComponentExpandedImpl[S] {

    type C = scala.swing.Slider

    override def init()(implicit tx: S#Tx, ctx: Ex.Context[S]): this.type = {
      val minOpt    = ctx.getProperty[Ex[Int]](w, keyMin  ).map(_.expand[S].value)
      val maxOpt    = ctx.getProperty[Ex[Int]](w, keyMax  ).map(_.expand[S].value)
      val valueOpt  = ctx.getProperty[Ex[Int]](w, keyValue).map(_.expand[S].value)

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

      super.init()
    }

//    override def dispose()(implicit tx: S#Tx): Unit = super.dispose()
  }

  private final class ValueExpanded[S <: Sys[S]](ws: View.T[S, scala.swing.Slider], value0: Int)
                                                (implicit protected val targets: ITargets[S], cursor: stm.Cursor[S])
    extends IExpr[S, Int]
      with IGenerator[S, Change[Int]] {

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

    def value(implicit tx: S#Tx): Int = txValue.get(tx.peer)

    def changed: IEvent[S, Change[Int]] = this

    private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Change[Int]] =
      Some(pull.resolve[Change[Int]])

    def init()(implicit tx: S#Tx): this.type = {
      deferTx {
        val c = ws.component
        guiValue = c.value
        c.peer.addChangeListener(listener)
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = {
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
    override def productPrefix: String = s"Slider$$Value" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = {
      import ctx.{cursor, targets}
      val ws        = w.expand[S]
      val valueOpt  = ctx.getProperty[Ex[Int]](w, keyValue)
      val value0    = valueOpt.fold[Int](defaultValue)(_.expand[S].value)
      new ValueExpanded[S](ws, value0).init()
    }
  }

  final case class Min(w: Slider) extends Ex[Int] {
    override def productPrefix: String = s"Slider$$Min" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMin)
      valueOpt.getOrElse(Constant(defaultMin)).expand[S]
    }
  }

  final case class Max(w: Slider) extends Ex[Int] {
    override def productPrefix: String = s"Slider$$Max" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMax)
      valueOpt.getOrElse(Constant(defaultMax)).expand[S]
    }
  }

  private final case class Impl() extends Slider with ComponentImpl { w =>
    override def productPrefix = "Slider"   // serialization

    protected def mkControl[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).init()

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

  var min   : Ex[Int]
  var max   : Ex[Int]

  def value : Model[Int]
}