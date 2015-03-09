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
import de.sciss.lucre.stm.Disposable

import scala.concurrent.stm.Ref

object DoubleSpinnerViewImpl {
  def apply[S <: Sys[S]](_cell: CellView[S#Tx, Double], name: String, width: Int)
                           (implicit tx: S#Tx, cursor: stm.Cursor[S],
                            undoManager: UndoManager): DoubleSpinnerView[S] = {
    val res = new Impl[S](maxWidth = width) {
      impl =>
      protected var (value, committer)          = CellViewFactory.mkCommitter(_cell, name)(tx, cursor)
      protected val observer: Disposable[S#Tx]  = CellViewFactory.mkObserver (_cell, impl)
    }

    deferTx(res.guiInit())
    res
  }

  def optional[S <: Sys[S]](_cell: CellView[S#Tx, Option[Double]], name: String, width: Int)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S],
                         undoManager: UndoManager): DoubleSpinnerView.Optional[S] = {
    val res = new OptionalImpl[S](maxWidth = width) {
      impl =>

      protected var (value, committer)          = CellViewFactory.mkCommitter(_cell, name)(tx, cursor)
      protected val observer: Disposable[S#Tx]  = CellViewFactory.mkObserver (_cell, impl)

      private val defaultRef = Ref(Option.empty[S#Tx => Double])

      def default(implicit tx: S#Tx): Option[S#Tx => Double] = defaultRef.get(tx.peer)

      def default_=(option: Option[S#Tx => Double])(implicit tx: S#Tx): Unit =
        defaultRef.set(option)(tx.peer)
    }

    deferTx(res.guiInit())
    res
  }

  private abstract class Impl[S <: Sys[S]](maxWidth: Int)
                                          (implicit cursor: stm.Cursor[S], undoManager: UndoManager)
    extends DefinedNumberSpinnerViewImpl[S, Double](maxWidth) with DoubleSpinnerView[S] {

    final protected val tpe = expr.Double

    final protected def parseModelValue(v: Any): Option[Double] = v match {
      case i: Double  => Some(i)
      case _          => None
    }

    protected lazy val model: SpinnerNumberModel = new SpinnerNumberModel(value, Double.MinValue, Double.MaxValue, 0.1)
  }

  private abstract class OptionalImpl[S <: Sys[S]](maxWidth: Int)
                                                  (implicit cursor: stm.Cursor[S], undoManager: UndoManager)
    extends OptionalNumberSpinnerViewImpl[S, Double](maxWidth) with DoubleSpinnerView.Optional[S] {

    final protected def parseModelValue(v: Any): Option[Option[Double]] = v match {
      case Some(d: Double)  => Some(Some(d))
      case None             => Some(None)
      case _                => None
    }

    final protected def valueToComponent(): Unit =
      if (parseModelValue(component.value) != Some(value)) {
        component.value = value // .getOrElse(Double.NaN)
        // component.foreground  = if (value.isDefined) null else Color.gray
      }

    protected lazy val model = new NumericOptionSpinnerModel[Double](value0 = value,
      minimum0 = None, maximum0 = None, stepSize0 = 0.1)
  }
}