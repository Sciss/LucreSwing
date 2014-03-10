/*
 *  StringFieldViewImpl.scala
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

import scala.swing.TextField
import de.sciss.lucre.event.Sys
import de.sciss.lucre.stm.Disposable
import de.sciss.lucre.stm
import de.sciss.desktop.UndoManager
import scala.swing.event.EditDone
import de.sciss.lucre.expr
import expr.Expr
import de.sciss.model.Change
import de.sciss.serial.Serializer

object StringFieldViewImpl extends ExprViewFactory[String] {
  def fromExpr[S <: Sys[S]](_expr: Expr[S, String], name: String, columns: Int)
                           (implicit tx: S#Tx, cursor: stm.Cursor[S],
                            undoManager: UndoManager): StringFieldView[S] = {
    // implicit val tpe: ExprType[Int] = expr.Int
    val res = new Impl[S](editName = name, columns0 = columns) {
      impl =>
      protected var (value, committer)          = mkExprCommitter(_expr, name)(tx, cursor, expr.String)
      protected val observer: Disposable[S#Tx]  = mkExprObserver (_expr, impl)
    }

    deferTx(res.guiInit())
    res
  }

  def fromMap[S <: Sys[S], A](map: expr.Map[S, A, Expr[S, String], Change[String]], key: A, default: String,
                              name: String, columns: Int)
                             (implicit tx: S#Tx, keySerializer: Serializer[S#Tx, S#Acc, A],
                              cursor: stm.Cursor[S], undoManager: UndoManager): StringFieldView[S] = {
    val res = new Impl[S](editName = name, columns0 = columns) {
      impl =>
      protected var (value, committer)          = mkMapCommitter(map, key, default, name)(
        tx, cursor, keySerializer, expr.String)
      protected val observer: Disposable[S#Tx]  = mkMapObserver (map, key, impl)
    }

    deferTx(res.guiInit())
    res
  }

  private abstract class Impl[S <: Sys[S]](editName: String, columns0: Int)
                                       (implicit cursor: stm.Cursor[S], undoManager: UndoManager)
    extends StringFieldView[S] with ExprEditor[S, String, TextField] {

    protected def observer: Disposable[S#Tx]

    protected def committer: Option[ExprViewFactory.Committer[S, String]]

    protected def valueToComponent(): Unit = if (component.text != value) component.text = value

    protected def createComponent(): TextField = {
      val txt     = new TextField(value, columns0)
      dirty       = Some(DirtyBorder(txt))

      committer.foreach { com =>
        txt.listenTo(txt)
        txt.reactions += {
          case EditDone(_) =>
            val newValue = txt.text
            if (value != newValue) {
              val edit = cursor.step { implicit tx =>
                com.commit(newValue)
              }
              undoManager.add(edit)
              value = newValue
            }
            clearDirty()
        }
        observeDirty(txt)
      }
      txt
    }
  }
}