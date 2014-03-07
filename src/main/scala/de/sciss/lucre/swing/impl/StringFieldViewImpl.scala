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
import edit.EditExprVar

object StringFieldViewImpl {
  def apply[S <: Sys[S]](expr: Expr[S, String], name: String, columns: Int)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): StringFieldView[S] = {
    import de.sciss.lucre.expr.String._
    val exprH     = expr match {
      case Expr.Var(exprV) => Some(tx.newHandle(exprV))
      case _               => None
    }
    val value0    = expr.value
    val res       = new Impl[S](exprH, value0 = value0, editName = name, columns0 = columns)
    res.observer  = expr.changed.react { implicit tx => {
      case Change(_, now) => res.update(now)
    }}

    deferTx(res.guiInit())
    res
  }

  private final class Impl[S <: Sys[S]](exprH: Option[stm.Source[S#Tx, Expr.Var[S, String]]], value0: String,
                                        editName: String, columns0: Int)
                                       (implicit cursor: stm.Cursor[S], undoManager: UndoManager)
    extends StringFieldView[S] with ExprEditor[S, String, TextField] {

    protected var value = value0

    var observer: Disposable[S#Tx] = _

    // protected val tpe: Type[String] = Strings

    protected def valueToComponent(): Unit = if (component.text != value) component.text = value

    // should be called when the GUI component has been edited. this will update `value`,
    // transactionally update the expression (if editable), and register an undoable edit
    protected def commit(newValue: String): Unit = {
      if (value != newValue) {
        exprH.foreach { h =>
          val edit = cursor.step { implicit tx =>
            import expr.String._
            EditExprVar[S, String](s"Change $editName", expr = h(), value = newConst(newValue))
          }
          undoManager.add(edit)
        }
        value = newValue
      }
      clearDirty()
    }

    protected def createComponent(): TextField = {
      val txt     = new TextField(value, columns0)
      dirty       = Some(DirtyBorder(txt))

      txt.listenTo(txt)
      txt.reactions += {
        case EditDone(_) => commit(txt.text)
      }
      observeDirty(txt)
      txt
    }
  }
}