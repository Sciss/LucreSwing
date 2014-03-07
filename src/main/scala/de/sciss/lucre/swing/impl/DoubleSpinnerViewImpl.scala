/*
 *  DoubleSpinnerViewImpl.scala
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

package de.sciss.lucre
package swing
package impl

import de.sciss.lucre.event.Sys
import de.sciss.lucre.stm.Disposable
import de.sciss.swingplus.Spinner
import de.sciss.lucre.expr.Expr
import de.sciss.desktop.UndoManager
import de.sciss.lucre.{expr, stm}
import javax.swing.{JSpinner, SpinnerNumberModel}
import scala.swing.TextComponent
import scala.swing.event.ValueChanged
import de.sciss.lucre.swing.edit.{EditExprVar, EditExprMap}
import de.sciss.model.Change
import de.sciss.serial.Serializer
import javax.swing.undo.UndoableEdit

object DoubleSpinnerViewImpl {
  //  def apply[S <: Sys[S]](expr: Expr[S, Double], name: String, width: Int = 160)
  //                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): DoubleExprEditor[S]

  def fromExpr[S <: Sys[S]](_expr: Expr[S, Double], name: String, width: Int)
                           (implicit tx: S#Tx, cursor: stm.Cursor[S],
                            undoManager: UndoManager): DoubleSpinnerView[S] = {

    import expr.Double.{serializer, varSerializer}
    val exprVarHOpt = Expr.Var.unapply(_expr).map(tx.newHandle(_))
    val value0      = _expr.value
    val res         = new Impl[S](value0 = value0, maxWidth = width) {
      protected def editable = exprVarHOpt.isDefined

      protected def doCommit(newValue: Double)(implicit tx: S#Tx): UndoableEdit = {
        val exprVarH = exprVarHOpt.get
        import expr.Double.newConst
        implicit val d = expr.Double
        EditExprVar[S, Double](s"Change $name", expr = exprVarH(), value = newConst[S](newValue))
      }
    }
    res.observer  = _expr.changed.react {
      implicit tx => upd => res.update(upd.now)
    }

    deferTx(res.guiInit())
    res
  }

  def fromMap[S <: Sys[S], A](map: expr.Map[S, A, Expr[S, Double], Change[Double]], key: A, default: Double,
                             name: String, width: Int)
                            (implicit tx: S#Tx, keySerializer: Serializer[S#Tx, S#Acc, A],
                             cursor: stm.Cursor[S], undoManager: UndoManager): DoubleSpinnerView[S] = {
    implicit val valueSer  = expr.Double.serializer[S]
    val mapHOpt   = map.modifiableOption.map(tx.newHandle(_))
    val value0    = map.get(key).map(_.value).getOrElse(default)
    val res       = new Impl[S](value0 = value0, maxWidth = width) {
      protected def editable = mapHOpt.isDefined

      protected def doCommit(newValue: Double)(implicit tx: S#Tx): UndoableEdit = {
        val mapH = mapHOpt.get
        import expr.Double.newConst
        implicit val d = expr.Double
        EditExprMap[S, A, Double](s"Change $name", map = mapH(), key = key, value = Some(newConst[S](newValue)))
      }
    }
    res.observer  = map.changed.react {
      implicit tx => upd => upd.changes.foreach {
        case expr.Map.Added  (`key`, expr)                  => res.update(expr.value)
        // case expr.Map.Removed(`key`, expr)                  => res.update(ha?)
        case expr.Map.Element(`key`, expr, Change(_, now))  => res.update(now       )
        case _ =>
      }
    }

    deferTx(res.guiInit())
    res
  }

  private abstract class Impl[S <: Sys[S]](value0: Double, maxWidth: Int)
                                          (implicit cursor: stm.Cursor[S], undoManager: UndoManager)
    extends DoubleSpinnerView[S] with ExprEditor[S, Double, Spinner] {

    final protected var value = value0

    final var observer: Disposable[S#Tx] = _

    final protected val tpe = expr.Double

    protected def editable: Boolean

    final protected def valueToComponent(): Unit =
      if (sp.value != value) {
        // println("valueToComponent()")
        sp.value = value
      }

    private var sp: Spinner = _

    protected def doCommit(newValue: Double)(implicit tx: S#Tx): UndoableEdit

    final protected def commit(newValue: Double): Unit = {
      if (value != newValue) cursor.step { implicit tx =>
        val edit = doCommit(newValue)
        undoManager.add(edit)
        value = newValue
      }
      clearDirty()
    }

    final protected def createComponent(): Spinner = {
      val spm   = new SpinnerNumberModel(value, Double.MinValue, Double.MaxValue, 0.1)
      sp        = new Spinner(spm)
      val d1    = sp.preferredSize
      d1.width  = math.min(d1.width, maxWidth) // XXX TODO WTF
      sp.preferredSize = d1
      val d2    = sp.maximumSize
      d2.width  = math.min(d2.width, maxWidth)
      sp.maximumSize   = d2
      val d3    = sp.minimumSize
      d3.width  = math.min(d3.width, maxWidth)
      sp.minimumSize = d3
      sp.peer.getEditor match {
        case e: JSpinner.DefaultEditor =>
          val txt = new TextComponent { override lazy val peer = e.getTextField }
          dirty   = Some(DirtyBorder(txt))
        // THIS SHIT JUST DOESN'T WORK, FUCK YOU SWING
        // observeDirty(txt)
        case _ =>
      }

      if (editable) {
        sp.listenTo(sp)
        sp.reactions += {
          case ValueChanged(_) =>
            sp.value match {
              case v: Double => commit(v)
              case _ =>
            }
        }
      }

      sp
    }
  }
}