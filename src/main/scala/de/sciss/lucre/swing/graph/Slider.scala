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
import de.sciss.lucre.event.{IEvent, IPull, ITargets}
import de.sciss.lucre.event.impl.IEventImpl
import de.sciss.lucre.expr.{Ex, IExpr}
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.model.Change

object Slider {
  def apply(): Slider = Impl()

  def mk(configure: Slider => Unit): Slider = {
    val w = apply()
    configure(w)
    w
  }

  private final class Expanded[S <: Sys[S]] extends View[S]
    with ComponentHolder[scala.swing.Slider] {

    def init()(implicit tx: S#Tx): this.type = {
      deferTx {
        component = new scala.swing.Slider
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

  private final class ValueExpanded[S <: Sys[S]](implicit protected val targets: ITargets[S])
    extends IExpr[S, Int]
      with IEventImpl[S, Change[Int]] {

    def value(implicit tx: S#Tx): Int = ???

    def changed: IEvent[S, Change[Int]] = this

    private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Change[Int]] =
      pull.resolve

    def dispose()(implicit tx: S#Tx): Unit = ???
  }

  final case class Value(w: Slider) extends Ex[Int] {
    override def productPrefix: String = s"Slider$$Value" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = {
      import ctx.targets
      new ValueExpanded[S]
    }

    def aux: List[Aux] = Nil
  }

  private final case class Impl() extends Slider {
    override def productPrefix: String = "Slider"   // serialization

    protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View[S] =
      new Expanded[S].init()

    def enabled: Ex[Boolean] = ???

    def enabled_=(x: Ex[Boolean]): Unit = ???

    def min: Ex[Int] = ???

    def min_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, "min", x)
    }

    def max: Ex[Int] = ???

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
  var enabled: Ex[Boolean]

  var min: Ex[Int]
  var max: Ex[Int]

//  def value: Ex.Var[Int]

  var value: Ex[Int]
}