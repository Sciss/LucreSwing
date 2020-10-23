/*
 *  IntRangeSliderViewImpl.scala
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

package de.sciss.lucre.swing.impl

import de.sciss.audiowidgets.{DualRangeModel, DualRangeSlider}
import de.sciss.desktop.UndoManager
import de.sciss.lucre.swing.LucreSwing.{deferTx, requireEDT}
import de.sciss.lucre.swing.edit.EditVar
import de.sciss.lucre.swing.{IntRangeSliderView, Observation}
import de.sciss.lucre.{Cursor, IntObj, Txn}
import javax.swing.undo.UndoableEdit

import scala.concurrent.stm.{InTxn, Ref, TxnLocal}
import scala.swing.Swing

object IntRangeSliderViewImpl {
  def apply[T <: Txn[T]](model0: DualRangeModel, name: String, width: Int = 160)
                        (implicit tx: T, cursor: Cursor[T], undoManager: UndoManager): IntRangeSliderView[T] = {

    val res = new Impl[T](model0, name = name, width = width)
    deferTx(res.guiInit())
    res
  }

  private sealed trait ExprState { def isDefined: Boolean; def isEmpty: Boolean = !isDefined }
  private case object ExprNone  extends ExprState { val isDefined = false }
  private case object ExprRead  extends ExprState { val isDefined = true  }
  private case object ExprWrite extends ExprState { val isDefined = true  }

  private final case class RangeState(lo: ExprState, hi: ExprState, ext: ExprState) {
    def visible : Boolean = lo.isDefined    && (hi.isDefined    || ext.isDefined)
    def editable: Boolean = lo == ExprWrite && (hi == ExprWrite || ext == ExprWrite)
  }

  private class Impl[T <: Txn[T]](model0: DualRangeModel, name: String, width: Int)
                                 (implicit cursor: Cursor[T], undo: UndoManager)
    extends IntRangeSliderView[T] with ComponentHolder[DualRangeSlider] {

    type Obs    = Observation[T, IntObj[T]]
    type ObsOpt = Option[Obs]

    private val _value      = Ref(Option.empty[Obs])
    private val _rangeLo    = Ref(Option.empty[Obs])
    private val _rangeHi    = Ref(Option.empty[Obs])
    private val _extent     = Ref(Option.empty[Obs])
    private var _rangeState = RangeState(ExprNone, ExprNone, ExprNone)

    // executes the `now` function on the GUI thread
    private def mkObs(expr: Option[IntObj[T]])(now: Int => Unit)(implicit tx: T) = expr.map { ex =>
      Observation(ex)(tx => upd => if (!eventOrigin.get(tx.peer)) deferTx(now(upd.now))(tx))  // IntelliJ highlight bug
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

    private def examine(expr: Option[IntObj[T]])(implicit tx: T): (Option[Int], Boolean) = {
      val valOpt    = expr.map(_.value)
      val editable  = expr.flatMap(IntObj.Var.unapply).isDefined
      (valOpt, editable)
    }

    def value(implicit tx: T): Option[IntObj[T]] = _value.get(tx.peer).map(_.value())
    def value_=(expr: Option[IntObj[T]])(implicit tx: T): Unit = {
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

    def rangeLo(implicit tx: T): Option[IntObj[T]] = _rangeLo.get(tx.peer).map(_.value())
    def rangeLo_=(expr: Option[IntObj[T]])(implicit tx: T): Unit = {
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

    def rangeHi(implicit tx: T): Option[IntObj[T]] = _rangeHi.get(tx.peer).map(_.value())
    def rangeHi_=(expr: Option[IntObj[T]])(implicit tx: T): Unit = {
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

    def extent(implicit tx: T): Option[IntObj[T]] = _extent.get(tx.peer).map(_.value())
    def extent_=(expr: Option[IntObj[T]])(implicit tx: T): Unit = {
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
        implicit val itx: InTxn = tx.peer
        // Signalize that the event originated in the GUI, so that
        // we don't feed back from Txn to GUI
        eventOrigin.set(true)

        def tryEdit(obsRef: Ref[Option[Obs]], modelVal: Int): Option[UndoableEdit] = obsRef().flatMap { obs =>
          IntObj.Var.unapply(obs.value()).flatMap { vr =>
            if (vr.value != modelVal) {
              val ex   = IntObj.newConst[T](modelVal)
              implicit val intTpe: IntObj.type = IntObj
              val edit = EditVar.Expr[T, Int, IntObj](editName, vr, ex)
              Some(edit)
            }
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

    def dispose()(implicit tx: T): Unit = {
      implicit val itx: InTxn = tx.peer
      _value  .swap(None).foreach(_.dispose())
      _extent .swap(None).foreach(_.dispose())
      _rangeLo.swap(None).foreach(_.dispose())
      _rangeHi.swap(None).foreach(_.dispose())
    }
  }
}
