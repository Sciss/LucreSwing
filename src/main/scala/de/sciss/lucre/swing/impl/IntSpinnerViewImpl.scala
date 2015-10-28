/*
 *  IntSpinnerViewImpl.scala
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
}