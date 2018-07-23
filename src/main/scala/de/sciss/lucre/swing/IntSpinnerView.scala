/*
 *  IntSpinnerView.scala
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
import de.sciss.lucre.expr.IntObj
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.impl.{IntSpinnerViewImpl => Impl}
import de.sciss.swingplus.Spinner

object IntSpinnerView {
  def apply[S <: Sys[S]](expr: IntObj[S], name: String, width: Int = 160)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): IntSpinnerView[S] = {
    implicit val tpe: IntObj.type = IntObj
    Impl(CellView.expr[S, Int, IntObj](expr), name = name, width = width)
  }

  def cell[S <: Sys[S]](cell: CellView[S#Tx, Int], name: String, width: Int = 160)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): IntSpinnerView[S] =
    Impl(cell, name = name, width = width)

  def optional[S <: Sys[S]](cell: CellView[S#Tx, Option[Int]], name: String, width: Int = 160,
                            default: Option[Int] = None)
                           (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): Optional[S] =
    Impl.optional(cell, name = name, width = width, default0 = default)

  trait Optional[S <: Sys[S]] extends IntSpinnerView[S] {
    /** Sets a default value to be displayed when the model value is absent.
      * This must be called on the EDT.
      */
    var default: Option[Int]
  }
}
trait IntSpinnerView[S <: Sys[S]] extends View[S] {
  type C = Spinner
}