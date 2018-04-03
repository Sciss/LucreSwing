/*
 *  IntSpinnerViewImpl.scala
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

package de.sciss.lucre
package swing
package impl

import javax.swing.{JSpinner, SpinnerNumberModel}

import de.sciss.desktop.UndoManager
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.stm.Disposable
import de.sciss.swingplus.Spinner

object IntSpinnerViewImpl extends CellViewFactory[Int] {
  def apply[S <: Sys[S]](cell: CellView[S#Tx, Int], name: String, width: Int)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S],
                         undoManager: UndoManager): IntSpinnerView[S] = {
    val res = new Impl[S](maxWidth = width) {
      impl =>
      protected var (value, committer)          = CellViewFactory.mkCommitter(cell, name)(tx, cursor)
      protected val observer: Disposable[S#Tx]  = CellViewFactory.mkObserver (cell, impl)
    }

    deferTx(res.guiInit())
    res
  }

  def optional[S <: Sys[S]](_cell: CellView[S#Tx, Option[Int]], name: String, width: Int, default0: Option[Int])
                           (implicit tx: S#Tx, cursor: stm.Cursor[S],
                            undoManager: UndoManager): IntSpinnerView.Optional[S] = {
    val res = new OptionalImpl[S](maxWidth = width) {
      impl =>

      protected var (value, committer)          = CellViewFactory.mkCommitter(_cell, name)(tx, cursor)
      protected val observer: Disposable[S#Tx]  = CellViewFactory.mkObserver (_cell, impl)

      // private val defaultRef = Ref(Option.empty[S#Tx => Int])
      private var _default = default0

      // def default(implicit tx: S#Tx): Option[S#Tx => Int] = defaultRef.get(tx.peer)
      def default: Option[Int] = {
        requireEDT()
        _default
      }

      //      def default_=(option: Option[S#Tx => Int])(implicit tx: S#Tx): Unit =
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

  private abstract class Impl[S <: Sys[S]](maxWidth: Int)
                                          (implicit cursor: stm.Cursor[S], undoManager: UndoManager)
    extends DefinedNumberSpinnerViewImpl[S, Int](maxWidth) with IntSpinnerView[S] {

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
  private abstract class OptionalImpl[S <: Sys[S]](maxWidth: Int)
                                                  (implicit cursor: stm.Cursor[S], undoManager: UndoManager)
    extends OptionalNumberSpinnerViewImpl[S, Int](maxWidth, true) with IntSpinnerView.Optional[S] {

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