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

import de.sciss.lucre.expr.CellView
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import javax.swing.undo.{AbstractUndoableEdit, UndoableEdit}

object EditCellView {
  @deprecated("Try to transition to stm.UndoManager", since = "1.17.0")
  def apply[S <: Sys[S], A](name: String, cell: CellView.VarR[S, A], value: A)
                           (implicit tx: S#Tx, cursor: stm.Cursor[S]): UndoableEdit = {
    import cell.serializer
    val beforeH = tx.newHandle(cell.repr)
    val res     = new Impl[S, A, cell.Repr](name, cell)(beforeH = beforeH, now = value) // IntelliJ highlight bug
    res.perform()
    res
  }

  private final class Impl[S <: Sys[S], A, Repr0](name: String, cell: CellView.VarR[S, A] { type Repr = Repr0 })
                                                 (beforeH: stm.Source[S#Tx, Repr0], now: A)
                                                 (implicit cursor: stm.Cursor[S])
    extends AbstractUndoableEdit {

    override def undo(): Unit = {
      super.undo()
      cursor.step { implicit tx => cell.repr = beforeH() }
    }

    override def redo(): Unit = {
      super.redo()
      cursor.step { implicit tx => perform() }
    }

    def perform()(implicit tx: S#Tx): Unit = cell.repr = cell.lift(now)

    override def getPresentationName: String = name
  }
}