/*
 *  IntRangeSliderViewImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2015 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing
package impl

import javax.swing.undo.UndoableEdit

import de.sciss.audiowidgets.{DualRangeModel, DualRangeSlider}
import de.sciss.desktop.UndoManager
import de.sciss.lucre
import de.sciss.lucre.event.Sys
import de.sciss.lucre.expr.Expr
import de.sciss.lucre.stm
import de.sciss.lucre.swing.edit.EditVar

import scala.concurrent.stm.{Ref, TxnLocal}
import scala.swing.Swing

object IntRangeSliderViewImpl {
  def apply[S <: Sys[S]](model0: DualRangeModel, name: String, width: Int = 160)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): IntRangeSliderView[S] = {

    val res = new Impl[S](model0, name = name, width = width)
    deferTx(res.guiInit())
    res
  }

  private sealed trait ExprState { def isDefined: Boolean; def isEmpty = !isDefined }
  private case object ExprNone  extends ExprState { val isDefined = false }
  private case object ExprRead  extends ExprState { val isDefined = true  }
  private case object ExprWrite extends ExprState { val isDefined = true  }

  private final case class RangeState(lo: ExprState, hi: ExprState, ext: ExprState) {
    def visible : Boolean = lo.isDefined    && (hi.isDefined    || ext.isDefined)
    def editable: Boolean = lo == ExprWrite && (hi == ExprWrite || ext == ExprWrite)
  }

  private class Impl[S <: Sys[S]](model0: DualRangeModel, name: String, width: Int)
                                 (implicit cursor: stm.Cursor[S], undo: UndoManager)
    extends IntRangeSliderView[S] with ComponentHolder[DualRangeSlider] {

    import de.sciss.lucre.expr.Int.{serializer, varSerializer}

    type Obs    = Observation[S, Expr[S, Int]]
    type ObsOpt = Option[Obs]

    private val _value      = Ref(Option.empty[Obs])
    private val _rangeLo    = Ref(Option.empty[Obs])
    private val _rangeHi    = Ref(Option.empty[Obs])
    private val _extent     = Ref(Option.empty[Obs])
    private var _rangeState = RangeState(ExprNone, ExprNone, ExprNone)

    // executes the `now` function on the GUI thread
    private def mkObs(expr: Option[Expr[S, Int]])(now: Int => Unit)(implicit tx: S#Tx) = expr.map { ex =>
      Observation(ex)(tx => upd => if (!eventOrigin.get(tx.peer)) deferTx(now(upd.now))(tx))
    }

    private def withoutListening[A](thunk: => A): A = {
      requireEDT()
      model0.removeChangeListener(modelListener)
      try {
        thunk
      } finally {
        model0.addChangeListener(modelListener)
      }
    }

    // gui reaction to txn change
    private def setValue(value: Int): Unit = withoutListening {
      component.value = value
    }

    private def examine(expr: Option[Expr[S, Int]])(implicit tx: S#Tx): (Option[Int], Boolean) = {
      val valOpt    = expr.map(_.value)
      val editable  = expr.flatMap(Expr.Var.unapply).isDefined
      (valOpt, editable)
    }

    def value(implicit tx: S#Tx): Option[Expr[S, Int]] = _value.get(tx.peer).map(_.value())
    def value_=(expr: Option[Expr[S, Int]])(implicit tx: S#Tx): Unit = {
      val newObs = mkObs(expr)(setValue)
      _value.swap(newObs)(tx.peer).foreach(_.dispose())
      val (valOpt, editable) = examine(expr)

      deferTx {
        component.valueVisible  = valOpt.isDefined
        component.valueEditable = editable
        valOpt.foreach(setValue)
      }
    }

    // gui reaction to txn change
    private def setRangeLo(value: Int): Unit = withoutListening {
      model0.range = if (_rangeState.hi.isDefined) {
        (value, model0.range._2)
      } else {
        (value, value + model0.extent)
      }
    }

    def rangeLo(implicit tx: S#Tx): Option[Expr[S, Int]] = _rangeLo.get(tx.peer).map(_.value())
    def rangeLo_=(expr: Option[Expr[S, Int]])(implicit tx: S#Tx): Unit = {
      val newObs = mkObs(expr)(setRangeLo)
      _rangeLo.swap(newObs)(tx.peer).foreach(_.dispose())
      val (valOpt, editable) = examine(expr)

      deferTx {
        _rangeState = _rangeState.copy(lo = if (valOpt.isEmpty) ExprNone else if (editable) ExprWrite else ExprRead)
        component.rangeVisible  = _rangeState.visible
        component.rangeEditable = _rangeState.editable
        valOpt.foreach(setRangeLo)
      }
    }

    // gui reaction to txn change
    private def setRangeHi(value: Int): Unit = withoutListening {
      model0.range = (model0.range._1, value)
    }

    def rangeHi(implicit tx: S#Tx): Option[Expr[S, Int]] = _rangeHi.get(tx.peer).map(_.value())
    def rangeHi_=(expr: Option[Expr[S, Int]])(implicit tx: S#Tx): Unit = {
      val newObs = mkObs(expr)(setRangeHi)
      if (expr.isDefined) _extent.swap(None)(tx.peer).foreach(_.dispose())
      _rangeHi.swap(newObs)(tx.peer).foreach(_.dispose())
      val (valOpt, editable) = examine(expr)

      deferTx {
        _rangeState = _rangeState.copy(
          hi  = if (valOpt.isEmpty  ) ExprNone else if (editable) ExprWrite else ExprRead,
          ext = if (valOpt.isDefined) ExprNone else _rangeState.ext // clear if necessary
        )
        component.rangeVisible  = _rangeState.visible
        component.rangeEditable = _rangeState.editable
        valOpt.foreach(setRangeHi)
      }
    }

    // gui reaction to txn change
    private def setRangeExt(value: Int): Unit = withoutListening {
      model0.extent = value
    }

    def extent(implicit tx: S#Tx): Option[Expr[S, Int]] = _extent.get(tx.peer).map(_.value())
    def extent_=(expr: Option[Expr[S, Int]])(implicit tx: S#Tx): Unit = {
      val newObs = mkObs(expr)(setRangeExt)
      if (expr.isDefined) _rangeHi.swap(None)(tx.peer).foreach(_.dispose())
      _extent.swap(newObs)(tx.peer).foreach(_.dispose())
      val (valOpt, editable) = examine(expr)

      deferTx {
        _rangeState = _rangeState.copy(
          ext = if (valOpt.isEmpty  ) ExprNone else if (editable) ExprWrite else ExprRead,
          hi  = if (valOpt.isDefined) ExprNone else _rangeState.hi  // clear if necessary
        )
        component.rangeVisible  = _rangeState.visible
        component.rangeEditable = _rangeState.editable
        valOpt.foreach(setRangeExt)
      }
    }

    private val eventOrigin = TxnLocal(false)

    private val editName = s"Change $name"

    private lazy val modelListener = Swing.ChangeListener { _ =>
      // println(s"adjusting? ${model0.adjusting}")
      val editOpt = cursor.step { implicit tx =>
        implicit val itx = tx.peer
        // Signalize that the event originated in the GUI, so that
        // we don't feed back from Txn to GUI
        eventOrigin.set(true)

        def tryEdit(obsRef: Ref[Option[Obs]], modelVal: Int): Option[UndoableEdit] = obsRef().flatMap { obs =>
          Expr.Var.unapply(obs.value()).flatMap { vr =>
            if (vr.value != modelVal)
              Some(EditVar.Expr[S, Int](editName, vr, lucre.expr.Int.newConst(modelVal)))
            else None
          }
        }

        val editValueOpt = tryEdit(_value, model0.value)

        val editLoOpt = if (_rangeState.lo == ExprWrite) tryEdit(_rangeLo, model0.range._1) else None

        val editHiExtOpt = if (_rangeState.hi == ExprWrite)
          tryEdit(_rangeHi, model0.range._2)
        else if (_rangeState.ext == ExprWrite)
          tryEdit(_extent, model0.extent)
        else None

        eventOrigin.set(false)
        editValueOpt orElse editLoOpt orElse editHiExtOpt // XXX TODO: make a compound edit of more than one defined
      }

      editOpt.foreach(undo.add)
    }

    def guiInit(): Unit = {
      val sl          = new DualRangeSlider(model0)
      sl.valueVisible = false
      sl.rangeVisible = false

      model0.addChangeListener(modelListener)

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
