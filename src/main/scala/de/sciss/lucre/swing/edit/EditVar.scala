/*
 *  EditVar.scala
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
package edit

import de.sciss.lucre
import lucre.event.Sys
import lucre.stm
import javax.swing.undo.{UndoableEdit, AbstractUndoableEdit}
import de.sciss.serial

object EditVar {
  def Expr[S <: Sys[S], A](name: String, expr: lucre.expr.Expr.Var[S, A], value: lucre.expr.Expr[S, A])
                          (implicit tx: S#Tx, cursor: stm.Cursor[S],
                           serializer   : serial.Serializer[S#Tx, S#Acc, lucre.expr.Expr    [S, A]],
                           varSerializer: serial.Serializer[S#Tx, S#Acc, lucre.expr.Expr.Var[S, A]]): UndoableEdit =
    apply(name, expr, value)

  def apply[S <: Sys[S], Elem, Vr <: stm.Source[S#Tx, Elem] with stm.Sink[S#Tx, Elem]](
    name: String, expr: Vr, value: Elem)(implicit tx: S#Tx, cursor: stm.Cursor[S],
                                         serializer   : serial.Serializer[S#Tx, S#Acc, Elem],
                                         varSerializer: serial.Serializer[S#Tx, S#Acc, Vr  ]): UndoableEdit = {
    val exprH   = tx.newHandle(expr)
    val beforeH = tx.newHandle(expr())
    val nowH    = tx.newHandle(value)
    val res     = new Impl(name, exprH, beforeH, nowH)
    res.perform()
    res
  }

  private final class Impl[S <: Sys[S], Elem, Vr <: stm.Source[S#Tx, Elem] with stm.Sink[S#Tx, Elem]](name: String,
                                                                                                      exprH  : stm.Source[S#Tx, Vr],
                                                                                                      beforeH: stm.Source[S#Tx, Elem],
                                                                                                      nowH   : stm.Source[S#Tx, Elem])(implicit cursor: stm.Cursor[S])
    extends AbstractUndoableEdit {

    override def undo(): Unit = {
      super.undo()
      cursor.step { implicit tx =>
        val expr  = exprH()
        expr()    = beforeH()
      }
    }

    override def redo(): Unit = {
      super.redo()
      cursor.step { implicit tx => perform() }
    }

    def perform()(implicit tx: S#Tx): Unit = {
      val expr  = exprH()
      expr()    = nowH()
    }

    override def getPresentationName = name
  }
}