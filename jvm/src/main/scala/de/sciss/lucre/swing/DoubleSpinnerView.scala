/*
 *  DoubleSpinnerView.scala
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
import de.sciss.lucre.swing.impl.{DoubleSpinnerViewImpl => Impl}
import de.sciss.lucre.{Cursor, DoubleObj, Txn}
import de.sciss.swingplus.Spinner

object DoubleSpinnerView {
  def cell[T <: Txn[T]](cell: CellView[T, Double], name: String, width: Int = 160)
                        (implicit tx: T, cursor: Cursor[T], undoManager: UndoManager): DoubleSpinnerView[T] =
    Impl(cell, name = name, width = width)

  def apply[T <: Txn[T]](expr: DoubleObj[T], name: String, width: Int = 160)
                        (implicit tx: T, cursor: Cursor[T], undoManager: UndoManager): DoubleSpinnerView[T] = {
    implicit val tpe: DoubleObj.type = DoubleObj
    Impl(CellView.expr[T, Double, DoubleObj](expr), name = name, width = width)
  }

  def optional[T <: Txn[T]](cell: CellView[T, Option[Double]], name: String, width: Int = 160,
                            default: Option[Double] = None)
                         (implicit tx: T, cursor: Cursor[T], undoManager: UndoManager): Optional[T] =
    Impl.optional(cell, name = name, width = width, default0 = default)

  trait Optional[T <: Txn[T]] extends DoubleSpinnerView[T] {
    /** Sets a default value to be displayed when the model value is absent.
      * This must be called on the EDT.
      */
    var default: Option[Double]
  }
}
trait DoubleSpinnerView[T <: Txn[T]] extends View[T] {
  type C = Spinner
}