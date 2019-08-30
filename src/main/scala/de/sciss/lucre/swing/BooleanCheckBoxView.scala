/*
 *  BooleanCheckBoxView.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import de.sciss.desktop.UndoManager
import de.sciss.lucre.expr.{BooleanObj, CellView}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.impl.{BooleanCheckBoxViewImpl => Impl}

object BooleanCheckBoxView {
  /** Creates a new view from an expression. The check box's label will initially be set to `name`. */
  def apply[S <: Sys[S]](expr: BooleanObj[S], name: String)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): BooleanCheckBoxView[S] = {
    implicit val tpe: BooleanObj.type = BooleanObj
    Impl(CellView.expr[S, Boolean, BooleanObj](expr), name = name)
  }

  def cell[S <: Sys[S]](cell: CellView[S#Tx, Boolean], name: String)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): BooleanCheckBoxView[S] =
    Impl(cell, name = name)

  def optional[S <: Sys[S]](cell: CellView[S#Tx, Option[Boolean]], name: String, default: Boolean)
                           (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): BooleanCheckBoxView[S] =
    Impl.optional(cell, name = name, default = default)
}
trait BooleanCheckBoxView[S <: Sys[S]] extends View[S] {
  type C = scala.swing.CheckBox
}