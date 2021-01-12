/*
 *  StringFieldViewImpl.scala
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

import java.awt.event.KeyEvent

import de.sciss.desktop.UndoManager
import de.sciss.lucre.expr.CellView
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.StringFieldView
import de.sciss.lucre.{Cursor, Disposable, Txn}
import javax.swing.KeyStroke

import scala.swing.event.EditDone
import scala.swing.{Action, TextField}

object StringFieldViewImpl extends CellViewFactory[String] {
  def apply[T <: Txn[T]](cell: CellView[T, String], name: String, columns: Int)
                        (implicit tx: T, cursor: Cursor[T],
                         undoManager: UndoManager): StringFieldView[T] = {
    val res: Impl[T] = new Impl[T](editName = name, columns0 = columns) {
      impl =>
      protected var (value, committer)          = CellViewFactory.mkCommitter(cell, name)(tx, cursor)
      protected val observer: Disposable[T]  = CellViewFactory.mkObserver (cell, impl)
    }

    deferTx(res.guiInit())
    res
  }
  
  private abstract class Impl[T <: Txn[T]](editName: String, columns0: Int)
                                       (implicit cursor: Cursor[T], undoManager: UndoManager)
    extends StringFieldView[T] with CellViewEditor[T, String, TextField] {

    override type C = scala.swing.TextField

    protected def observer: Disposable[T]

    protected def committer: Option[CellViewFactory.Committer[T, String]]

    protected def valueToComponent(): Unit = if (component.text != value) component.text = value

    protected def createComponent(): TextField = {
      val txt       = new TextField(value, columns0)
      val db        = DirtyBorder(txt)
      dirty         = Some(db)
      val aMap      = txt.peer.getActionMap
      val iMap      = txt.peer.getInputMap
      val keyAbort  = "de.sciss.Abort"

      aMap.put(keyAbort, Action("Cancel Editing") {
        if (db.visible) {
          txt.text = value
          clearDirty()
        }
      } .peer)
      iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), keyAbort)

      committer.foreach { com =>
        txt.listenTo(txt)
        txt.reactions += {
          case EditDone(_) =>
            val newValue = txt.text
            if (value != newValue) {
              val edit = cursor.step { implicit tx =>
                com.commit(newValue)
              }
              undoManager.add(edit)
              value = newValue
            }
            clearDirty()
        }
        observeDirty(txt)
      }
      txt
    }
  }
}