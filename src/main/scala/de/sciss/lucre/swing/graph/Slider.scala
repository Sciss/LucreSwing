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

import de.sciss.lucre.aux.Aux
import de.sciss.lucre.event.impl.IGenerator
import de.sciss.lucre.event.{IEvent, IPull, ITargets}
import de.sciss.lucre.expr.graph.Constant
import de.sciss.lucre.expr.{Ex, IExpr}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.model.Change
import javax.swing.event.{ChangeEvent, ChangeListener}

import scala.concurrent.stm.Ref

object Slider {
  def apply(): Slider = Impl()

  def mk(configure: Slider => Unit): Slider = {
    val w = apply()
    configure(w)
    w
  }

  private final class Expanded[S <: Sys[S]](w: Widget) extends View[S]
    with ComponentHolder[scala.swing.Slider] {

    type C = scala.swing.Slider

    def init()(implicit b: Widget.Builder[S], tx: S#Tx): this.type = {
      val minOpt    = b.getProperty[Ex[Int]](w, "min"  ).map(_.expand[S].value)
      val maxOpt    = b.getProperty[Ex[Int]](w, "max"  ).map(_.expand[S].value)
      val valueOpt  = b.getProperty[Ex[Int]](w, "value").map(_.expand[S].value)

      deferTx {
        val sl = new scala.swing.Slider
        minOpt  .foreach(sl.min   = _)
        maxOpt  .foreach(sl.max   = _)
        valueOpt.foreach(sl.value = _)
        component = sl
      }
//      obs = text.changed.react { implicit tx => ch =>
//        deferTx {
//          component.text = ch.now
//        }
//      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit =
      () // obs.dispose()
  }

  private final class ValueExpanded[S <: Sys[S]](ws: View.T[S, scala.swing.Slider], value0: Int)
                                                (implicit protected val targets: ITargets[S], cursor: stm.Cursor[S])
    extends IExpr[S, Int]
      with IGenerator[S, Change[Int]] {

    private[this] val listener = new ChangeListener {
      def stateChanged(e: ChangeEvent): Unit = {
        val sl      = ws.component
        val before  = guiValue
        val now     = sl.value
        val ch      = Change(before, now)
//        println(s"HERE: $ch")
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
        val sl = ws.component
        guiValue = sl.value
        sl.peer.addChangeListener(listener)
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      deferTx {
        val sl = ws.component
        sl.peer.removeChangeListener(listener)
      }
    }
  }

  private def defaultValue  =  50
  private def defaultMin    =   0
  private def defaultMax    = 100

  final case class Value(w: Slider) extends Ex[Int] {
    override def productPrefix: String = s"Slider$$Value" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = ctx match {
      case b: Widget.Builder[S] =>
        import b.{cursor, targets}
        val ws        = w.expand[S](b, tx)
        val valueOpt  = b.getProperty[Ex[Int]](w, "value")
        val value0    = valueOpt.fold[Int](defaultValue)(_.expand[S].value)
        new ValueExpanded[S](ws, value0).init()
    }

    def aux: List[Aux] = Nil
  }

  final case class Min(w: Slider) extends Ex[Int] {
    override def productPrefix: String = s"Slider$$Min" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = ctx match {
      case b: Widget.Builder[S] =>
        val valueOpt = b.getProperty[Ex[Int]](w, "min")
        valueOpt.getOrElse(Constant(defaultMin)).expand[S]
    }

    def aux: List[Aux] = Nil
  }

  final case class Max(w: Slider) extends Ex[Int] {
    override def productPrefix: String = s"Slider$$Max" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = ctx match {
      case b: Widget.Builder[S] =>
        val valueOpt = b.getProperty[Ex[Int]](w, "max")
        valueOpt.getOrElse(Constant(defaultMax)).expand[S]
    }

    def aux: List[Aux] = Nil
  }

  private final case class Impl() extends Slider {
    override def productPrefix: String = "Slider"   // serialization

    protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View.T[S, C] =
      new Expanded[S](this).init()

    def min: Ex[Int] = Min(this)

    def min_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, "min", x)
    }

    def max: Ex[Int] = Max(this)

    def max_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, "max", x)
    }

    def value: Ex[Int] = Value(this)

    def value_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, "value", x)
    }
  }
}
trait Slider extends Widget {
  type C = scala.swing.Slider

  var min   : Ex[Int]
  var max   : Ex[Int]
  var value : Ex[Int]
}