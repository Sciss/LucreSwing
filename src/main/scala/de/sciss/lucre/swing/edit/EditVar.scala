/*
 *  EditVar.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2017 Hanns Holger Rutz. All rights reserved.
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
import de.sciss.lucre.expr.Type
import lucre.stm.Sys
import lucre.stm
import javax.swing.undo.{UndoableEdit, AbstractUndoableEdit}
import de.sciss.serial

import scala.language.higherKinds

object EditVar {
  def Expr[S <: Sys[S], A, Ex[~ <: Sys[~]] <: expr.Expr[~, A]](name: String, expr: Ex[S] with stm.Var[S#Tx, Ex[S]],
                                                               value: Ex[S])
                          (implicit tx: S#Tx, cursor: stm.Cursor[S], tpe: Type.Expr[A, Ex]): UndoableEdit =
    apply[S, Ex[S], Ex[S] with stm.Var[S#Tx, Ex[S]]](name, expr, value)(tx, cursor, tpe.serializer, tpe.varSerializer)

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