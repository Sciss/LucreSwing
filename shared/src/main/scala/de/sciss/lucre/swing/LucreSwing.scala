/*
 *  LucreSwing.scala
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

import de.sciss.lucre.Log.{swing => log}
import de.sciss.lucre.TxnLike
import de.sciss.lucre.expr.ExElem
import de.sciss.lucre.expr.ExElem.ProductReader

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.stm.TxnLocal
import scala.util.control.NonFatal

object LucreSwing extends LucreSwingPlatform {
  /** Registers all known types. */
  def init(): Unit = {
    initPlatform()
    _init
  }

  private lazy val _init: Unit = {
    ExElem.addProductReaderSq({
      import graph._
      Seq[ProductReader[Product]](
        Bang,
        Border.Empty,
        BorderPanel, BorderPanel.HGap, BorderPanel.VGap,
        Button, Button.Clicked,
        CheckBox, CheckBox.Selected,
        ComboBox, ComboBox.Index, ComboBox.ValueOption,
        Component.Enabled, Component.Focusable, Component.Tooltip,
        DoubleField, DoubleField.Value, DoubleField.Min, DoubleField.Max, DoubleField.Step, DoubleField.Decimals, DoubleField.Unit, DoubleField.Prototype, DoubleField.Editable,
        Empty,
        FlowPanel, FlowPanel.HGap, FlowPanel.VGap, FlowPanel.Align,
        GridPanel, GridPanel.Rows, GridPanel.Columns, GridPanel.Compact, GridPanel.CompactRows, GridPanel.CompactColumns, GridPanel.HGap, GridPanel.VGap,
        IntField, IntField.Value, IntField.Min, IntField.Max, IntField.Step, IntField.Unit, IntField.Prototype, IntField.Editable,
        Label, Label.HAlign, Label.VAlign,
        Panel.Border,
        ProgressBar, ProgressBar.Value, ProgressBar.Min, ProgressBar.Max, ProgressBar.Label, ProgressBar.LabelPainted,
        Separator,
        Slider, Slider.Value, Slider.Min, Slider.Max,
        TextField, TextField.Text, TextField.Columns, TextField.Editable,
      )
    })
  }

  private[this] val guiCode = TxnLocal(init = Vec.empty[() => Unit], afterCommit = handleGUI)

  private[this] def handleGUI(seq: Vec[() => Unit]): Unit = {
    def exec(): Unit = {
      log.debug(s"handleGUI(seq.size = ${seq.size})")
      seq.foreach { fun =>
        try {
          fun()
        } catch {
          case NonFatal(e) => e.printStackTrace()
        }
      }
    }

    defer(exec())
  }

  def deferTx(thunk: => Unit)(implicit tx: TxnLike): Unit =
    guiCode.transform(_ :+ (() => thunk))(tx.peer)
}
