/*
 *  DoubleSpinnerView.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2018 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import de.sciss.desktop.UndoManager
import de.sciss.lucre.expr.DoubleObj
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.impl.{DoubleSpinnerViewImpl => Impl}
import de.sciss.swingplus.Spinner

object DoubleSpinnerView {
  def cell[S <: Sys[S]](cell: CellView[S#Tx, Double], name: String, width: Int = 160)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): DoubleSpinnerView[S] =
    Impl(cell, name = name, width = width)

  def apply[S <: Sys[S]](expr: DoubleObj[S], name: String, width: Int = 160)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): DoubleSpinnerView[S] = {
    implicit val tpe = DoubleObj
    Impl(CellView.expr[S, Double, DoubleObj](expr), name = name, width = width)
  }

  def optional[S <: Sys[S]](cell: CellView[S#Tx, Option[Double]], name: String, width: Int = 160,
                            default: Option[Double] = None)
                         (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): Optional[S] =
    Impl.optional(cell, name = name, width = width, default0 = default)

  trait Optional[S <: Sys[S]] extends DoubleSpinnerView[S] {
    /** Sets a default value to be displayed when the model value is absent.
      * This must be called on the EDT.
      */
    var default: Option[Double]
  }
}
trait DoubleSpinnerView[S <: Sys[S]] extends View[S] {
  override def component: Spinner
}