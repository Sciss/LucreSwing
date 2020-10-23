/*
 *  StringFieldView.scala
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

package de.sciss.lucre.swing

import de.sciss.desktop.UndoManager
import de.sciss.lucre.expr.CellView
import de.sciss.lucre.swing.impl.{StringFieldViewImpl => Impl}
import de.sciss.lucre.{Cursor, StringObj, Txn}

object StringFieldView {
  def apply[T <: Txn[T]](expr: StringObj[T], name: String, columns: Int = 16)
                        (implicit tx: T, cursor: Cursor[T], undoManager: UndoManager): StringFieldView[T] = {
    implicit val tpe: StringObj.type = StringObj
    Impl(CellView.expr[T, String, StringObj](expr), name = name, columns = columns)
  }

  def cell[T <: Txn[T]](cell: CellView[T, String], name: String, columns: Int = 16)
                        (implicit tx: T, cursor: Cursor[T], undoManager: UndoManager): StringFieldView[T] =
    Impl(cell, name = name, columns = columns)
}
trait StringFieldView[T <: Txn[T]] extends View[T] {
  type C = scala.swing.TextField
}