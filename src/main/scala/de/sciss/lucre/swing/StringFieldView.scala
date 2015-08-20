/*
 *  StringFieldView.scala
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

package de.sciss.lucre.swing

import de.sciss.desktop.UndoManager
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.expr.Expr
import de.sciss.lucre.stm
import de.sciss.lucre.swing.impl.{StringFieldViewImpl => Impl}

import scala.swing.TextField

object StringFieldView {
  def apply[S <: Sys[S]](expr: Expr[S, String], name: String, columns: Int = 16)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): StringFieldView[S] = {
    implicit val stringEx = de.sciss.lucre.expr.String
    Impl(CellView.expr(expr), name = name, columns = columns)
  }

  def cell[S <: Sys[S]](cell: CellView[S#Tx, String], name: String, columns: Int = 16)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): StringFieldView[S] =
    Impl(cell, name = name, columns = columns)
}
trait StringFieldView[S <: Sys[S]] extends View[S] {
  override def component: TextField
}