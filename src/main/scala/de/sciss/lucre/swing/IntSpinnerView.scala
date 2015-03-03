/*
 *  IntSpinnerView.scala
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
import de.sciss.lucre.event.Sys
import de.sciss.lucre.expr.Expr
import de.sciss.lucre.stm
import de.sciss.lucre.swing.impl.{IntSpinnerViewImpl => Impl}
import de.sciss.swingplus.Spinner

object IntSpinnerView {
  def apply[S <: Sys[S]](expr: Expr[S, Int], name: String, width: Int = 160)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): IntSpinnerView[S] = {
    implicit val intEx = de.sciss.lucre.expr.Int
    Impl(CellView.expr(expr), name = name, width = width)
  }

  def cell[S <: Sys[S]](cell: CellView[S#Tx, Int], name: String, width: Int = 160)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): IntSpinnerView[S] =
    Impl(cell, name = name, width = width)
}
trait IntSpinnerView[S <: Sys[S]] extends View[S] {
  override def component: Spinner
}