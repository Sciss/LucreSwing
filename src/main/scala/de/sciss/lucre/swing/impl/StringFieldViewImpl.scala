/*
 *  StringFieldViewImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2016 Hanns Holger Rutz. All rights reserved.
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

import java.awt.event.KeyEvent
import javax.swing.KeyStroke

import de.sciss.desktop.UndoManager
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.stm.Disposable

import scala.swing.event.EditDone
import scala.swing.{Action, TextField}

object StringFieldViewImpl extends CellViewFactory[String] {
  def apply[S <: Sys[S]](cell: CellView[S#Tx, String], name: String, columns: Int)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S],
                         undoManager: UndoManager): StringFieldView[S] = {
    val res = new Impl[S](editName = name, columns0 = columns) {
      impl =>
      protected var (value, committer)          = CellViewFactory.mkCommitter(cell, name)(tx, cursor)
      protected val observer: Disposable[S#Tx]  = CellViewFactory.mkObserver (cell, impl)
    }

    deferTx(res.guiInit())
    res
  }
  
  private abstract class Impl[S <: Sys[S]](editName: String, columns0: Int)
                                       (implicit cursor: stm.Cursor[S], undoManager: UndoManager)
    extends StringFieldView[S] with CellViewEditor[S, String, TextField] {

    protected def observer: Disposable[S#Tx]

    protected def committer: Option[CellViewFactory.Committer[S, String]]

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