/*
 *  BooleanCheckBoxView.scala
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

package de.sciss.lucre.swing

import de.sciss.desktop.UndoManager
import de.sciss.lucre.expr.CellView
import de.sciss.lucre.swing.impl.{BooleanCheckBoxViewImpl => Impl}
import de.sciss.lucre.{BooleanObj, Cursor, Txn}

object BooleanCheckBoxView {
  /** Creates a new view from an expression. The check box's label will initially be set to `name`. */
  def apply[T <: Txn[T]](expr: BooleanObj[T], name: String)
                        (implicit tx: T, cursor: Cursor[T], undoManager: UndoManager): BooleanCheckBoxView[T] = {
    implicit val tpe: BooleanObj.type = BooleanObj
    Impl(CellView.expr[T, Boolean, BooleanObj](expr), name = name)
  }

  def cell[T <: Txn[T]](cell: CellView[T, Boolean], name: String)
                        (implicit tx: T, cursor: Cursor[T], undoManager: UndoManager): BooleanCheckBoxView[T] =
    Impl(cell, name = name)

  def optional[T <: Txn[T]](cell: CellView[T, Option[Boolean]], name: String, default: Boolean)
                           (implicit tx: T, cursor: Cursor[T], undoManager: UndoManager): BooleanCheckBoxView[T] =
    Impl.optional(cell, name = name, default = default)
}
trait BooleanCheckBoxView[T <: Txn[T]] extends View[T] {
  type C = scala.swing.CheckBox
}