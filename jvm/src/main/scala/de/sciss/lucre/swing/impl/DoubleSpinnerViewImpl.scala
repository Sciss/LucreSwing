/*
 *  DoubleSpinnerViewImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2020 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.impl

import de.sciss.desktop.UndoManager
import de.sciss.lucre.expr.CellView
import de.sciss.lucre.swing.DoubleSpinnerView
import de.sciss.lucre.swing.LucreSwing.{deferTx, requireEDT}
import de.sciss.lucre.{Cursor, Disposable, Txn}
import de.sciss.swingplus.Spinner
import javax.swing.{JSpinner, SpinnerNumberModel}

object DoubleSpinnerViewImpl {
  def apply[T <: Txn[T]](_cell: CellView[T, Double], name: String, width: Int)
                           (implicit tx: T, cursor: Cursor[T],
                            undoManager: UndoManager): DoubleSpinnerView[T] = {
    val res: Impl[T] = new Impl[T](maxWidth = width) {
      impl =>
      protected var (value, committer)       = CellViewFactory.mkCommitter(_cell, name)(tx, impl.cursor)
      protected val observer: Disposable[T]  = CellViewFactory.mkObserver (_cell, impl)
    }

    deferTx(res.guiInit())
    res
  }

  def optional[T <: Txn[T]](_cell: CellView[T, Option[Double]], name: String, width: Int, default0: Option[Double])
                        (implicit tx: T, cursor: Cursor[T],
                         undoManager: UndoManager): DoubleSpinnerView.Optional[T] = {
    val res: OptionalImpl[T] = new OptionalImpl[T](maxWidth = width) {
      impl =>

      protected var (value, committer)       = CellViewFactory.mkCommitter(_cell, name)(tx, impl.cursor)
      protected val observer: Disposable[T]  = CellViewFactory.mkObserver (_cell, impl)

      // private val defaultRef = Ref(Option.empty[T => Double])
      private var _default = default0

      // def default(implicit tx: T): Option[T => Double] = defaultRef.get(tx.peer)
      def default: Option[Double] = {
        requireEDT()
        _default
      }

      //      def default_=(option: Option[T => Double])(implicit tx: T): Unit =
      //        defaultRef.set(option)(tx.peer)

      def default_=(value: Option[Double]): Unit = {
        requireEDT()
        _default = value
        if (this.value.isEmpty) {
          component.peer.getEditor match {
            case editor: JSpinner.DefaultEditor =>
              editor.getTextField.setValue(editor.getSpinner.getValue)
            case _ =>
          }
        }
      }

      // protected def defaultValue: Option[Double] = defaultRef.single.get.fol
    }

    deferTx(res.guiInit())
    res
  }

  private abstract class Impl[T <: Txn[T]](maxWidth: Int)
                                          (implicit cursor: Cursor[T], undoManager: UndoManager)
    extends DefinedNumberSpinnerViewImpl[T, Double](maxWidth) with DoubleSpinnerView[T] {

    override type C = Spinner

    // final protected val tpe = expr.Double

    final protected def parseModelValue(v: Any): Option[Double] = v match {
      case i: Double  => Some(i)
      case _          => None
    }

    // do away with idiotic grouping and increase max fraction digits
    override protected def mkSpinner: Spinner = {
      val sp  = super.mkSpinner
      sp.peer.getEditor match {
        case ed: JSpinner.NumberEditor =>
          val fmt = ed.getFormat
          fmt.setMaximumFractionDigits(5)
          fmt.setGroupingUsed(false)
          ed.getTextField.setText(fmt.format(sp.value))
        case _ =>
      }
      sp
    }

    protected lazy val model: SpinnerNumberModel = new SpinnerNumberModel(value, Double.MinValue, Double.MaxValue, 0.1)
  }

  private abstract class OptionalImpl[T <: Txn[T]](maxWidth: Int)
                                                  (implicit cursor: Cursor[T], undoManager: UndoManager)
    extends OptionalNumberSpinnerViewImpl[T, Double](maxWidth, false) with DoubleSpinnerView.Optional[T] {

    override type C = Spinner

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
      minimum0 = Some(Double.MinValue), maximum0 = Some(Double.MaxValue), stepSize0 = 0.1)
  }
}