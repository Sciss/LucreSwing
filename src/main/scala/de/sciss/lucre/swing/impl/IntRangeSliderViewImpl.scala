/*
 *  IntRangeSliderViewImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing
package impl

import de.sciss.lucre.event.Sys
import de.sciss.audiowidgets.{DualRangeSlider, DualRangeModel}
import de.sciss.lucre.stm
import de.sciss.desktop.UndoManager
import de.sciss.lucre.expr.Expr
import scala.concurrent.stm.Ref
import de.sciss.model.Change
import de.sciss.lucre

object IntRangeSliderViewImpl {
  def apply[S <: Sys[S]](model0: DualRangeModel, name: String, width: Int = 160)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): IntRangeSliderView[S] = {

    val res = new Impl[S](model0, name = name, width = width)
    deferTx(res.guiInit())
    res
  }

  private class Impl[S <: Sys[S]](model0: DualRangeModel, name: String, width: Int)
    extends IntRangeSliderView[S] with ComponentHolder[DualRangeSlider] {

    type Obs    = Observation[S, Expr[S, Int]]
    type ObsOpt = Option[Obs]

    private val _value    = Ref(Option.empty[Obs])
    private val _rangeLo  = Ref(Option.empty[Obs])
    private val _rangeHi  = Ref(Option.empty[Obs])
    private val _extent   = Ref(Option.empty[Obs])

    private def mkObs(expr: Option[Expr[S, Int]])(now: Int => Unit)(implicit tx: S#Tx) = expr.map { ex =>
      import lucre.expr.Int.serializer
      Observation(ex)(_ => upd => now(upd.now))
    }

    def value(implicit tx: S#Tx): Option[Expr[S, Int]] = _value.get(tx.peer).map(_.value())
    def value_=(expr: Option[Expr[S, Int]])(implicit tx: S#Tx): Unit = {
      val newObs = mkObs(expr) { now =>
        deferTx(component.value = now)
      }
      _value.swap(newObs)(tx.peer).foreach(_.dispose())
      val valOpt    = expr.map(_.value)
      val editable  = expr.flatMap(Expr.Var.unapply).isDefined

      deferTx {
        component.valueVisible  = valOpt.isDefined
        component.valueEditable = editable
        valOpt.foreach(component.value = _)
      }
    }

    def rangeLo(implicit tx: S#Tx): Option[Expr[S, Int]] = _rangeLo.get(tx.peer).map(_.value())
    def rangeLo_=(expr: Option[Expr[S, Int]])(implicit tx: S#Tx): Unit = {
      ???
    }

    def rangeHi(implicit tx: S#Tx): Option[Expr[S, Int]] = _rangeHi.get(tx.peer).map(_.value())
    def rangeHi_=(expr: Option[Expr[S, Int]])(implicit tx: S#Tx): Unit = {
      ???
    }

    def extent(implicit tx: S#Tx): Option[Expr[S, Int]] = _extent.get(tx.peer).map(_.value())
    def extent_=(expr: Option[Expr[S, Int]])(implicit tx: S#Tx): Unit = {
      ???
    }

    def guiInit(): Unit = {
      val sl          = new DualRangeSlider(model0)
      sl.valueVisible = false
      sl.rangeVisible = false
      component       = sl
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      implicit val itx = tx.peer
      _value  .swap(None).foreach(_.dispose())
      _extent .swap(None).foreach(_.dispose())
      _rangeLo.swap(None).foreach(_.dispose())
      _rangeHi.swap(None).foreach(_.dispose())
    }
  }
}
