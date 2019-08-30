/*
 *  BooleanCheckBoxViewImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2019 Hanns Holger Rutz. All rights reserved.
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
import de.sciss.lucre.stm
import de.sciss.lucre.stm.{Disposable, Sys}
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.{BooleanCheckBoxView, View}

import scala.swing.CheckBox
import scala.swing.event.ButtonClicked

object BooleanCheckBoxViewImpl extends CellViewFactory[Boolean] {
  def apply[S <: Sys[S]](cell: CellView[S#Tx, Boolean], name: String)
                           (implicit tx: S#Tx, cursor: stm.Cursor[S],
                            undoManager: UndoManager): BooleanCheckBoxView[S] = {
    val res: Impl[S] = new Impl[S](editName = name) {
      impl =>
      protected var (value, committer)          = CellViewFactory.mkCommitter(cell, name)(tx, cursor)
      protected val observer: Disposable[S#Tx]  = CellViewFactory.mkObserver (cell, impl)
    }

    deferTx(res.guiInit())
    res
  }

  def optional[S <: Sys[S]](cell: CellView[S#Tx, Option[Boolean]], name: String, default: Boolean)
                           (implicit tx: S#Tx, cursor: stm.Cursor[S],
                            undoManager: UndoManager): BooleanCheckBoxView[S] = {
    val res: OptionalImpl[S] = new OptionalImpl[S](editName = name, default = default) {
      impl =>
      protected var (value, committer)          = CellViewFactory.mkCommitter(cell, name)(tx, cursor)
      protected val observer: Disposable[S#Tx]  = CellViewFactory.mkObserver (cell, impl)
    }

    deferTx(res.guiInit())
    res
  }

  private abstract class BasicImpl[S <: Sys[S], B]
    extends BooleanCheckBoxView[S] with CellViewEditor[S, B, CheckBox] with View.Editable[S] {

    override type C = scala.swing.CheckBox

    protected def editName: String

    protected def committer: Option[CellViewFactory.Committer[S, B]]

    protected var definedValue: Boolean

    protected def valueToComponent(): Unit = if (component.selected != definedValue) component.selected = definedValue

    protected def createComponent(): CheckBox = {
      val cb        = new CheckBox(editName)
      cb.selected   = definedValue
      committer.foreach { com =>
        cb.listenTo(cb)
        cb.reactions += {
          case ButtonClicked(_) =>
            val newValue = cb.selected
            if (definedValue != newValue) {
              definedValue = newValue
              val edit = cursor.step { implicit tx =>
                com.commit(value)
              }
              undoManager.add(edit)
            }
        }
      }
      cb
    }
  }

  private abstract class Impl[S <: Sys[S]](protected val editName: String)
                                          (implicit val cursor: stm.Cursor[S],
                                           val undoManager: UndoManager)
    extends BasicImpl[S, Boolean] {

    protected final def definedValue: Boolean = value
    protected final def definedValue_=(b: Boolean): Unit = value = b
  }

  private abstract class OptionalImpl[S <: Sys[S]](protected val editName: String, default: Boolean)
                                          (implicit val cursor: stm.Cursor[S], val undoManager: UndoManager)
    extends BasicImpl[S, Option[Boolean]] {

    protected final def definedValue: Boolean = value.getOrElse(default)
    protected final def definedValue_=(b: Boolean): Unit = value = Some(b)
  }
}