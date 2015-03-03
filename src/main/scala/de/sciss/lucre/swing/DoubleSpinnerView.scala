/*
 *  DoubleSpinnerView.scala
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

import de.sciss.lucre.event.Sys
import de.sciss.swingplus.Spinner
import impl.{DoubleSpinnerViewImpl => Impl}
import de.sciss.model.Change
import de.sciss.serial.Serializer
import de.sciss.desktop.UndoManager
import de.sciss.lucre.stm
import de.sciss.lucre.expr
import expr.Expr

object DoubleSpinnerView {
  def apply[S <: Sys[S]](cell: CellView[S#Tx, Double], name: String, width: Int = 160)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): DoubleSpinnerView[S] =
    Impl.apply(cell, name = name, width = width)
}
trait DoubleSpinnerView[S <: Sys[S]] extends View[S] {
  override def component: Spinner
}