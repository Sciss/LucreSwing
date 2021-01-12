/*
 *  EditVar.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2021 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.edit

import de.sciss.lucre.{Cursor, Expr => _Expr, Sink, Source, Txn, Var => LVar}
import de.sciss.serial
import javax.swing.undo.{AbstractUndoableEdit, UndoableEdit}

object EditVar {
  @deprecated("Try to transition to stm.UndoManager", since = "1.17.0")
  def Expr[T <: Txn[T], A, Ex[~ <: Txn[~]] <: _Expr[~, A]](name: String, expr: Ex[T] with LVar[T, Ex[T]],
                                                               value: Ex[T])
                          (implicit tx: T, cursor: Cursor[T], tpe: _Expr.Type[A, Ex]): UndoableEdit =
    apply[T, Ex[T], Ex[T] with LVar[T, Ex[T]]](name, expr, value)(tx, cursor, tpe.format, tpe.varFormat)

  @deprecated("Try to transition to stm.UndoManager", since = "1.17.0")
  def apply[T <: Txn[T], Elem, Vr <: Source[T, Elem] with Sink[T, Elem]](
    name: String, expr: Vr, value: Elem)(implicit tx: T, cursor: Cursor[T],
                                         format   : serial.TFormat[T, Elem],
                                         varFormat: serial.TFormat[T, Vr  ]): UndoableEdit = {
    val exprH   = tx.newHandle(expr)
    val beforeH = tx.newHandle(expr())
    val nowH    = tx.newHandle(value)
    val res     = new Impl[T, Elem, Vr](name, exprH, beforeH, nowH)
    res.perform()
    res
  }

  private final class Impl[T <: Txn[T], Elem, Vr <: Source[T, Elem] with Sink[T, Elem]](name: String,
                                                                                        exprH  : Source[T, Vr],
                                                                                        beforeH: Source[T, Elem],
                                                                                        nowH   : Source[T, Elem])
                                                                                       (implicit cursor: Cursor[T])
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

    def perform()(implicit tx: T): Unit = {
      val expr  = exprH()
      expr()    = nowH()
    }

    override def getPresentationName: String = name
  }
}