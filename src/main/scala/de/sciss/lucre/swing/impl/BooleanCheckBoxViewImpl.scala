/*
 *  BooleanCheckBoxViewImpl.scala
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

import de.sciss.desktop.UndoManager
import de.sciss.lucre.event.Sys
import de.sciss.lucre.stm.Disposable

import scala.swing.CheckBox
import scala.swing.event.ButtonClicked

object BooleanCheckBoxViewImpl extends CellViewFactory[Boolean] {
  def apply[S <: Sys[S]](cell: CellView[S#Tx, Boolean], name: String)
                           (implicit tx: S#Tx, cursor: stm.Cursor[S],
                            undoManager: UndoManager): BooleanCheckBoxView[S] = {
    val res = new Impl[S](editName = name) {
      impl =>
      protected var (value, committer)          = mkCommitter(cell, name)(tx, cursor)
      protected val observer: Disposable[S#Tx]  = mkObserver (cell, impl)
    }

    deferTx(res.guiInit())
    res
  }

  private abstract class Impl[S <: Sys[S]](editName: String)
                                          (implicit cursor: stm.Cursor[S], undoManager: UndoManager)
    extends BooleanCheckBoxView[S] with ExprEditor[S, Boolean, CheckBox] {

    protected def observer: Disposable[S#Tx]

    protected def committer: Option[CellViewFactory.Committer[S, Boolean]]

    protected def valueToComponent(): Unit = if (component.selected != value) component.selected = value

    protected def createComponent(): CheckBox = {
      val cb        = new CheckBox(editName)
      cb.selected   = value
      committer.foreach { com =>
        cb.listenTo(cb)
        cb.reactions += {
          case ButtonClicked(_) =>
            val newValue = cb.selected
            if (value != newValue) {
              val edit = cursor.step { implicit tx =>
                com.commit(newValue)
              }
              undoManager.add(edit)
              value = newValue
            }
            // clearDirty()
        }
        // observeDirty(cb)
      }
      cb
    }
  }
}