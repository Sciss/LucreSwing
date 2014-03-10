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
import de.sciss.lucre.expr.{ExprType, Expr}
import de.sciss.desktop.UndoManager
import de.sciss.lucre.{expr, stm}
import javax.swing.{JSpinner, SpinnerNumberModel}
import scala.swing.TextComponent
import scala.swing.event.ValueChanged
import de.sciss.lucre.swing.edit.{EditExprVar, EditExprMap}
import de.sciss.model.Change
import de.sciss.serial.Serializer
import javax.swing.undo.UndoableEdit

object DoubleSpinnerViewImpl extends ExprViewFactory[Double] {
  def fromExpr[S <: Sys[S]](_expr: Expr[S, Double], name: String, width: Int)
                           (implicit tx: S#Tx, cursor: stm.Cursor[S],
                            undoManager: UndoManager): DoubleSpinnerView[S] = {
    // implicit val tpe: ExprType[Double] = expr.Double
    val res = new Impl[S](maxWidth = width) {
      impl =>
      protected var (value, committer)          = mkExprCommitter(_expr, name)(tx, cursor, expr.Double)
      protected val observer: Disposable[S#Tx]  = mkExprObserver (_expr, impl)
    }

    deferTx(res.guiInit())
    res
  }

  def fromMap[S <: Sys[S], A](map: expr.Map[S, A, Expr[S, Double], Change[Double]], key: A, default: Double,
                             name: String, width: Int)
                            (implicit tx: S#Tx, keySerializer: Serializer[S#Tx, S#Acc, A],
                             cursor: stm.Cursor[S], undoManager: UndoManager): DoubleSpinnerView[S] = {
    val res = new Impl[S](maxWidth = width) {
      impl =>
      protected var (value, committer)          = mkMapCommitter(map, key, default, name)(
                                                    tx, cursor, keySerializer, expr.Double)
      protected val observer: Disposable[S#Tx]  = mkMapObserver (map, key, impl)
    }

    deferTx(res.guiInit())
    res
  }

  private abstract class Impl[S <: Sys[S]](maxWidth: Int)
                                          (implicit cursor: stm.Cursor[S], undoManager: UndoManager)
    extends DoubleSpinnerView[S] with ExprEditor[S, Double, Spinner] {

    // current display value (GUI threaded)
    protected var value: Double

    // reactive observer (will be disposed with component)
    protected def observer: Disposable[S#Tx]

    final protected val tpe = expr.Double

    protected def committer: Option[ExprViewFactory.Committer[S, Double]]

    final protected def valueToComponent(): Unit =
      if (sp.value != value) {
        // println("valueToComponent()")
        sp.value = value
      }

    private var sp: Spinner = _

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

      committer.foreach { com =>
        sp.listenTo(sp)
        sp.reactions += {
          case ValueChanged(_) =>
            sp.value match {
              case newValue: Double =>
                if (value != newValue) cursor.step { implicit tx =>
                  val edit = com.commit(newValue)
                  undoManager.add(edit)
                  value = newValue
                }
                clearDirty()
              case _ =>
            }
        }
      }

      sp
    }
  }
}