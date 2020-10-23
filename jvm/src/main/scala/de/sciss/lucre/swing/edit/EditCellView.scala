/*
 *  EditExprView.scala
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

package de.sciss.lucre.swing.edit

import de.sciss.lucre.{Cursor, Source, Txn}
import de.sciss.lucre.expr.CellView
import javax.swing.undo.{AbstractUndoableEdit, UndoableEdit}

object EditCellView {
  @deprecated("Try to transition to stm.UndoManager", since = "1.17.0")
  def apply[T <: Txn[T], A](name: String, cell: CellView.VarR[T, A], value: A)
                           (implicit tx: T, cursor: Cursor[T]): UndoableEdit = {
    import cell.format
    val beforeH = tx.newHandle(cell.repr)
    val res     = new Impl[T, A, cell.Repr](name, cell)(beforeH = beforeH, now = value) // IntelliJ highlight bug
    res.perform()
    res
  }

  private final class Impl[T <: Txn[T], A, Repr0](name: String, cell: CellView.VarR[T, A] { type Repr = Repr0 })
                                                 (beforeH: Source[T, Repr0], now: A)
                                                 (implicit cursor: Cursor[T])
    extends AbstractUndoableEdit {

    override def undo(): Unit = {
      super.undo()
      cursor.step { implicit tx => cell.repr = beforeH() }
    }

    override def redo(): Unit = {
      super.redo()
      cursor.step { implicit tx => perform() }
    }

    def perform()(implicit tx: T): Unit = cell.repr = cell.lift(now)

    override def getPresentationName: String = name
  }
}