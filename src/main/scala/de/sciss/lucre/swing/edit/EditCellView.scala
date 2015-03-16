/*
 *  EditExprView.scala
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

object EditCellView {
  def apply[S <: Sys[S], A](name: String, cell: CellView.Var[S, A], value: A)
                           (implicit tx: S#Tx, cursor: stm.Cursor[S]): UndoableEdit = {
    import cell.serializer
    val beforeH = tx.newHandle(cell.repr)
    val res     = new Impl[S, A, cell.Repr](name, cell)(beforeH = beforeH, now = value)
    res.perform()
    res
  }

  private final class Impl[S <: Sys[S], A, Repr0](name: String, cell: CellView.Var[S, A] { type Repr = Repr0 })
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

    override def getPresentationName = name
  }
}