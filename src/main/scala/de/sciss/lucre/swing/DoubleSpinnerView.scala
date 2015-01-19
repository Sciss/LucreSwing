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
  def apply[S <: Sys[S]](expr: Expr[S, Double], name: String, width: Int = 160)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): DoubleSpinnerView[S] =
    Impl.fromExpr(expr, name = name, width = width)

  def fromMap[S <: Sys[S], A](map: expr.Map[S, A, Expr[S, Double], Change[Double]], key: A, default: Double,
                            name: String, width: Int = 160)
                           (implicit tx: S#Tx, keySerializer: Serializer[S#Tx, S#Acc, A],
                            cursor: stm.Cursor[S], undoManager: UndoManager): DoubleSpinnerView[S] =
    Impl.fromMap(map, key = key, default = default, name = name, width = width)
}
trait DoubleSpinnerView[S <: Sys[S]] extends View[S] {
  override def component: Spinner
}