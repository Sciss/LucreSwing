/*
 *  BooleanCheckBoxViewImpl.scala
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

package de.sciss.lucre
package swing
package impl

import de.sciss.desktop.UndoManager
import de.sciss.lucre.event.Sys
import de.sciss.lucre.expr.Expr
import de.sciss.lucre.stm.Disposable
import de.sciss.model.Change
import de.sciss.serial.Serializer

import scala.swing.CheckBox
import scala.swing.event.ButtonClicked

object BooleanCheckBoxViewImpl extends ExprViewFactory[Boolean] {
  def fromExpr[S <: Sys[S]](_expr: Expr[S, Boolean], name: String)
                           (implicit tx: S#Tx, cursor: stm.Cursor[S],
                            undoManager: UndoManager): BooleanCheckBoxView[S] = {
    // implicit val tpe: ExprType[Int] = expr.Int
    val res = new Impl[S](editName = name) {
      impl =>
      protected var (value, committer)          = mkExprCommitter(_expr, name)(tx, cursor, expr.Boolean)
      protected val observer: Disposable[S#Tx]  = mkExprObserver (_expr, impl)
    }

    deferTx(res.guiInit())
    res
  }

  def fromMap[S <: Sys[S], A](map: expr.Map[S, A, Expr[S, Boolean], Change[Boolean]], key: A, default: Boolean,
                              name: String)
                             (implicit tx: S#Tx, keySerializer: Serializer[S#Tx, S#Acc, A],
                              cursor: stm.Cursor[S], undoManager: UndoManager): BooleanCheckBoxView[S] = {
    val res = new Impl[S](editName = name) {
      impl =>
      protected var (value, committer)          = mkMapCommitter(map, key, default, name)(
        tx, cursor, keySerializer, expr.Boolean)
      protected val observer: Disposable[S#Tx]  = mkMapObserver (map, key, impl)
    }

    deferTx(res.guiInit())
    res
  }

  private abstract class Impl[S <: Sys[S]](editName: String)
                                          (implicit cursor: stm.Cursor[S], undoManager: UndoManager)
    extends BooleanCheckBoxView[S] with ExprEditor[S, Boolean, CheckBox] {

    protected def observer: Disposable[S#Tx]

    protected def committer: Option[ExprViewFactory.Committer[S, Boolean]]

    protected def valueToComponent(): Unit = if (component.selected != value) component.selected = value

    protected def createComponent(): CheckBox = {
      val cb        = new CheckBox(editName)
      cb.selected   = value
      committer.foreach { com =>
        cb.listenTo(cb)
        cb.reactions += {
          case ButtonClicked(_) =>
            val newValue = cb.selected
            if (value != newValue) {
              val edit = cursor.step { implicit tx =>
                com.commit(newValue)
              }
              undoManager.add(edit)
              value = newValue
            }
            // clearDirty()
        }
        // observeDirty(cb)
      }
      cb
    }
  }
}