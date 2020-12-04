/*
 *  IntSpinnerViewImpl.scala
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
import de.sciss.lucre.swing.IntSpinnerView
import de.sciss.lucre.swing.LucreSwing.{deferTx, requireEDT}
import de.sciss.lucre.{Cursor, Disposable, Txn}
import de.sciss.swingplus.Spinner
import javax.swing.{JSpinner, SpinnerNumberModel}

object IntSpinnerViewImpl extends CellViewFactory[Int] {
  def apply[T <: Txn[T]](cell: CellView[T, Int], name: String, width: Int)
                        (implicit tx: T, cursor: Cursor[T],
                         undoManager: UndoManager): IntSpinnerView[T] = {
    val res: Impl[T] = new Impl[T](maxWidth = width) {
      impl =>
      protected var (value, committer)       = CellViewFactory.mkCommitter(cell, name)(tx, impl.cursor)
      protected val observer: Disposable[T]  = CellViewFactory.mkObserver (cell, impl)
    }

    deferTx(res.guiInit())
    res
  }

  def optional[T <: Txn[T]](_cell: CellView[T, Option[Int]], name: String, width: Int, default0: Option[Int])
                           (implicit tx: T, cursor: Cursor[T],
                            undoManager: UndoManager): IntSpinnerView.Optional[T] = {
    val res: OptionalImpl[T] = new OptionalImpl[T](maxWidth = width) {
      impl =>

      protected var (value, committer)       = CellViewFactory.mkCommitter(_cell, name)(tx, impl.cursor)
      protected val observer: Disposable[T]  = CellViewFactory.mkObserver (_cell, impl)

      // private val defaultRef = Ref(Option.empty[T => Int])
      private var _default = default0

      // def default(implicit tx: T): Option[T => Int] = defaultRef.get(tx.peer)
      def default: Option[Int] = {
        requireEDT()
        _default
      }

      //      def default_=(option: Option[T => Int])(implicit tx: T): Unit =
      //        defaultRef.set(option)(tx.peer)

      def default_=(value: Option[Int]): Unit = {
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

      // protected def defaultValue: Option[Int] = defaultRef.single.get.fol
    }

    deferTx(res.guiInit())
    res
  }

  private abstract class Impl[T <: Txn[T]](maxWidth: Int)
                                          (implicit cursor: Cursor[T], undoManager: UndoManager)
    extends DefinedNumberSpinnerViewImpl[T, Int](maxWidth) with IntSpinnerView[T] {

    override type C = Spinner

    // final protected val tpe = expr.Int

    protected def parseModelValue(v: Any): Option[Int] = v match {
      case i: Int => Some(i)
      case _      => None
    }

    override protected def mkSpinner: Spinner = {
      val sp = super.mkSpinner
      // do away with idiotic grouping
      sp.peer.getEditor match {
        case ed: JSpinner.NumberEditor =>
          val fmt = ed.getFormat
          fmt.setGroupingUsed(false)
          ed.getTextField.setText(fmt.format(sp.value)) // annoyingly, the text doesn't update without this
        case _ =>
      }
      sp
    }

    protected lazy val model: SpinnerNumberModel = new SpinnerNumberModel(value, Int.MinValue, Int.MaxValue, 1)
  }

  // XXX TODO --- the overloaded constructor is a hack
  // to preserve binary compatibility with previous release version
  private abstract class OptionalImpl[T <: Txn[T]](maxWidth: Int)
                                                  (implicit cursor: Cursor[T], undoManager: UndoManager)
    extends OptionalNumberSpinnerViewImpl[T, Int](maxWidth, true) with IntSpinnerView.Optional[T] {

    override type C = Spinner

    final protected def parseModelValue(v: Any): Option[Option[Int]] = v match {
      case Some(d: Int)  => Some(Some(d))
      case None          => Some(None)
      case _             => None
    }

    final protected def valueToComponent(): Unit =
      if (parseModelValue(component.value) != Some(value)) {
        component.value = value // .getOrElse(Int.NaN)
        // component.foreground  = if (value.isDefined) null else Color.gray
      }

    protected lazy val model = new NumericOptionSpinnerModel[Int](value0 = value,
      minimum0 = Some(Int.MinValue), maximum0 = Some(Int.MaxValue), stepSize0 = 1)
  }
}