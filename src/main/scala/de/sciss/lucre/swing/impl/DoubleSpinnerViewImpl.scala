/*
 *  DoubleSpinnerViewImpl.scala
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

package de.sciss.lucre
package swing
package impl

import javax.swing.SpinnerNumberModel

import de.sciss.desktop.UndoManager
import de.sciss.lucre.event.Sys
import de.sciss.lucre.expr.Expr
import de.sciss.lucre.stm.Disposable
import de.sciss.model.Change
import de.sciss.serial.Serializer

object DoubleSpinnerViewImpl extends CellViewFactory[Double] {
  def apply[S <: Sys[S]](_cell: CellView[S#Tx, Double], name: String, width: Int)
                           (implicit tx: S#Tx, cursor: stm.Cursor[S],
                            undoManager: UndoManager): DoubleSpinnerView[S] = {
    ???
//    val res = new Impl[S](maxWidth = width) {
//      impl =>
//      protected var (value, committer)          = mkCommitter(_cell, name)(tx, cursor, expr.Double)
//      protected val observer: Disposable[S#Tx]  = mkExprObserver (_cell, impl)
//    }
//
//    deferTx(res.guiInit())
//    res
  }

  def fromMap[S <: Sys[S], A](map: expr.Map[S, A, Expr[S, Double], Change[Double]], key: A, default: Double,
                             name: String, width: Int)
                            (implicit tx: S#Tx, keySerializer: Serializer[S#Tx, S#Acc, A],
                             cursor: stm.Cursor[S], undoManager: UndoManager): DoubleSpinnerView[S] = {
    val res = new Impl[S](maxWidth = width) {
      impl =>
      protected var (value, committer)          = mkMapCommitter(map, key, default, name)(
                                                    tx, cursor, keySerializer, expr.Double)
      protected val observer: Disposable[S#Tx]  = mkMapObserver (map, key, impl)
    }

    deferTx(res.guiInit())
    res
  }

  private abstract class Impl[S <: Sys[S]](maxWidth: Int)
                                          (implicit cursor: stm.Cursor[S], undoManager: UndoManager)
    extends NumberSpinnerViewImpl[S, Double](maxWidth) with DoubleSpinnerView[S] {

    final protected val tpe = expr.Double

    protected def parseModelValue(v: Any): Option[Double] = v match {
      case i: Double  => Some(i)
      case _          => None
    }

    protected lazy val model: SpinnerNumberModel = new SpinnerNumberModel(value, Double.MinValue, Double.MaxValue, 0.1)
  }
}