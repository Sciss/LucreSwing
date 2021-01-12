/*
 *  BooleanCheckBoxViewImpl.scala
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

package de.sciss.lucre.swing.impl

import de.sciss.desktop.UndoManager
import de.sciss.lucre.expr.CellView
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.{BooleanCheckBoxView, View}
import de.sciss.lucre.{Cursor, Disposable, Txn}

import scala.swing.CheckBox
import scala.swing.event.ButtonClicked

object BooleanCheckBoxViewImpl extends CellViewFactory[Boolean] {
  def apply[T <: Txn[T]](cell: CellView[T, Boolean], name: String)
                           (implicit tx: T, cursor: Cursor[T],
                            undoManager: UndoManager): BooleanCheckBoxView[T] = {
    val res: Impl[T] = new Impl[T](editName = name) {
      impl =>
      protected var (value, committer)       = CellViewFactory.mkCommitter(cell, name)(tx, impl.cursor)
      protected val observer: Disposable[T]  = CellViewFactory.mkObserver (cell, impl)
    }

    deferTx(res.guiInit())
    res
  }

  def optional[T <: Txn[T]](cell: CellView[T, Option[Boolean]], name: String, default: Boolean)
                           (implicit tx: T, cursor: Cursor[T],
                            undoManager: UndoManager): BooleanCheckBoxView[T] = {
    val res: OptionalImpl[T] = new OptionalImpl[T](editName = name, default = default) {
      impl =>
      protected var (value, committer)       = CellViewFactory.mkCommitter(cell, name)(tx, impl.cursor)
      protected val observer: Disposable[T]  = CellViewFactory.mkObserver (cell, impl)
    }

    deferTx(res.guiInit())
    res
  }

  private abstract class BasicImpl[T <: Txn[T], B]
    extends BooleanCheckBoxView[T] with CellViewEditor[T, B, CheckBox] with View.Editable[T] {

    override type C = scala.swing.CheckBox

    protected def editName: String

    protected def committer: Option[CellViewFactory.Committer[T, B]]

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

  private abstract class Impl[T <: Txn[T]](protected val editName: String)
                                          (implicit val cursor: Cursor[T],
                                           val undoManager: UndoManager)
    extends BasicImpl[T, Boolean] {

    protected final def definedValue: Boolean = value
    protected final def definedValue_=(b: Boolean): Unit = value = b
  }

  private abstract class OptionalImpl[T <: Txn[T]](protected val editName: String, default: Boolean)
                                          (implicit val cursor: Cursor[T], val undoManager: UndoManager)
    extends BasicImpl[T, Option[Boolean]] {

    protected final def definedValue: Boolean = value.getOrElse(default)
    protected final def definedValue_=(b: Boolean): Unit = value = Some(b)
  }
}