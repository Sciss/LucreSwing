/*
 *  IntSpinnerView.scala
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
import de.sciss.lucre.{Cursor, IntObj, Txn}
import de.sciss.lucre.expr.CellView
import de.sciss.lucre.swing.impl.{IntSpinnerViewImpl => Impl}
import de.sciss.swingplus.Spinner

object IntSpinnerView {
  def apply[T <: Txn[T]](expr: IntObj[T], name: String, width: Int = 160)
                        (implicit tx: T, cursor: Cursor[T], undoManager: UndoManager): IntSpinnerView[T] = {
    implicit val tpe: IntObj.type = IntObj
    Impl(CellView.expr[T, Int, IntObj](expr), name = name, width = width)
  }

  def cell[T <: Txn[T]](cell: CellView[T, Int], name: String, width: Int = 160)
                        (implicit tx: T, cursor: Cursor[T], undoManager: UndoManager): IntSpinnerView[T] =
    Impl(cell, name = name, width = width)

  def optional[T <: Txn[T]](cell: CellView[T, Option[Int]], name: String, width: Int = 160,
                            default: Option[Int] = None)
                           (implicit tx: T, cursor: Cursor[T], undoManager: UndoManager): Optional[T] =
    Impl.optional(cell, name = name, width = width, default0 = default)

  trait Optional[T <: Txn[T]] extends IntSpinnerView[T] {
    /** Sets a default value to be displayed when the model value is absent.
      * This must be called on the EDT.
      */
    var default: Option[Int]
  }
}
trait IntSpinnerView[T <: Txn[T]] extends View[T] {
  type C = Spinner
}